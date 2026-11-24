package com.example.shop.integration;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.entity.*;
import com.example.shop.exception.InsufficientStockException;
import com.example.shop.repository.*;
import com.example.shop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against H2 (PostgreSQL mode) with the real Flyway schema. Demonstrates
 * that stock can never be oversold: the conditional UPDATE, transactional
 * rollback, concurrent checkouts and stock restoration on cancel.
 */
@SpringBootTest
@ActiveProfiles("test")
class StockIntegrationTest {

    @Autowired
    ProductRepository productRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderService orderService;
    @Autowired
    TransactionTemplate tx;

    User seller;
    Category category;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(newUser(Role.SELLER));
        category = categoryRepository.save(Category.builder().name("Cat-" + UUID.randomUUID()).build());
    }

    private User newUser(Role role) {
        return User.builder().email(UUID.randomUUID() + "@test.com").passwordHash("x").fullName("T").role(role)
                .sellerApproved(true).build();
    }

    private Product newProduct(int stock) {
        return productRepository.save(Product.builder().seller(seller).category(category).name("P-" + UUID.randomUUID())
                .price(new BigDecimal("10.00")).stock(stock).build());
    }

    private User customerWithCart(Product p, int qty) {
        User c = userRepository.save(newUser(Role.CUSTOMER));
        cartItemRepository.save(CartItem.builder().user(c).product(p).quantity(qty).build());
        return c;
    }

    private int stockOf(Product p) {
        return productRepository.findStockById(p.getId());
    }

    @Test
    void conditionalUpdateNeverGoesNegative() {
        Product p = newProduct(5);
        assertEquals(1, (int) tx.execute(s -> productRepository.decrementStock(p.getId(), 5)));
        assertEquals(0, stockOf(p));
        assertEquals(0, (int) tx.execute(s -> productRepository.decrementStock(p.getId(), 1)), "no rows updated when stock is 0");
        assertEquals(0, stockOf(p));

        Product q = newProduct(2);
        assertEquals(0, (int) tx.execute(s -> productRepository.decrementStock(q.getId(), 3)), "request exceeds stock");
        assertEquals(2, stockOf(q), "row untouched when the condition fails");
    }

    @Test
    void failedCheckoutRollsBackEarlierDecrementsAndKeepsCart() {
        Product a = newProduct(10);   // lower id: decremented first
        Product b = newProduct(1);
        User customer = userRepository.save(newUser(Role.CUSTOMER));
        cartItemRepository.save(CartItem.builder().user(customer).product(a).quantity(2).build());
        cartItemRepository.save(CartItem.builder().user(customer).product(b).quantity(2).build());   // more than available

        assertThrows(InsufficientStockException.class,
                () -> orderService.checkout(customer.getId(), new CheckoutRequest("addr", null)));

        assertEquals(10, stockOf(a), "decrement of product A must be rolled back");
        assertEquals(1, stockOf(b));
        assertEquals(0, orderRepository.findByCustomerId(customer.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        assertEquals(2, (int) tx.execute(s -> cartItemRepository.findByUserIdWithProduct(customer.getId()).size()));
    }

    @Test
    void concurrentCheckoutsCannotOversell() throws Exception {
        final int stock = 5;
        final int buyers = 12;
        Product p = newProduct(stock);
        List<User> customers = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            customers.add(customerWithCart(p, 1));
        }

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for (User c : customers) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    orderService.checkout(c.getId(), new CheckoutRequest("addr", null));
                    ok.incrementAndGet();
                } catch (InsufficientStockException e) {
                    outOfStock.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertTrue(unexpected.isEmpty(), "unexpected errors: " + unexpected);
        assertEquals(stock, ok.get(), "exactly 'stock' checkouts succeed");
        assertEquals(buyers - stock, outOfStock.get());
        assertEquals(0, stockOf(p), "stock is exactly exhausted, never negative");
    }

    @Test
    void cancelRestoresStock() {
        Product p = newProduct(10);
        User customer = customerWithCart(p, 4);

        var order = orderService.checkout(customer.getId(), new CheckoutRequest("addr", null));
        assertEquals(6, stockOf(p));

        var cancelled = orderService.changeStatus(order.id(), customer.getId(), Role.CUSTOMER, OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(10, stockOf(p));
    }
}
