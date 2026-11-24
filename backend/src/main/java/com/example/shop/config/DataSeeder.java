package com.example.shop.config;

import com.example.shop.entity.*;
import com.example.shop.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Inserts demo data on first start (only when the users table is empty). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CouponRepository coupons;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, CategoryRepository categories, ProductRepository products,
                      CouponRepository coupons, PasswordEncoder encoder) {
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.coupons = coupons;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        users.save(user("admin@shop.com", "Admin@123", "Site Admin", Role.ADMIN, true));
        User seller = users.save(user("seller@shop.com", "Seller@123", "Demo Seller", Role.SELLER, true));
        User seller2 = users.save(user("seller2@shop.com", "Seller@123", "Pending Seller", Role.SELLER, false));
        users.save(user("customer@shop.com", "Customer@123", "Demo Customer", Role.CUSTOMER, false));

        Map<String, Category> cats = new LinkedHashMap<>();
        for (String[] c : new String[][]{
                {"Electronics", "Gadgets, audio and accessories"}, {"Books", "Fiction and non-fiction"},
                {"Clothing", "Apparel for everyone"}, {"Home & Kitchen", "Everything for your home"},
                {"Sports", "Gear and equipment"}}) {
            cats.put(c[0], categories.save(Category.builder().name(c[0]).description(c[1]).build()));
        }

        Object[][] items = {
                {"Wireless Headphones", "Over-ear Bluetooth headphones with 30h battery.", "89.99", 40, "Electronics"},
                {"USB-C Fast Charger", "65W GaN charger with two ports.", "29.50", 120, "Electronics"},
                {"Mechanical Keyboard", "Hot-swappable keyboard with tactile switches.", "109.00", 25, "Electronics"},
                {"4K Action Camera", "Waterproof action camera with stabilisation.", "149.99", 15, "Electronics"},
                {"Clean Code", "A handbook of agile software craftsmanship.", "34.90", 60, "Books"},
                {"The Pragmatic Programmer", "Your journey to mastery.", "39.95", 50, "Books"},
                {"Science Fiction Anthology", "Twenty classic short stories.", "18.00", 80, "Books"},
                {"Cotton T-Shirt", "Soft 100% organic cotton tee.", "15.99", 200, "Clothing"},
                {"Denim Jacket", "Classic fit denim jacket.", "64.00", 35, "Clothing"},
                {"Running Shoes", "Lightweight breathable running shoes.", "74.50", 45, "Sports"},
                {"Yoga Mat", "Non-slip 6mm yoga mat.", "22.00", 90, "Sports"},
                {"Chef Knife", "8-inch stainless steel chef knife.", "49.00", 30, "Home & Kitchen"},
                {"Ceramic Mug Set", "Set of four 350ml mugs.", "24.99", 70, "Home & Kitchen"},
                {"Limited Edition Poster", "Only three left in stock!", "12.00", 3, "Home & Kitchen"},
        };
        int i = 0;
        for (Object[] it : items) {
            products.save(Product.builder()
                    .seller(i++ % 5 == 4 ? seller2 : seller)
                    .category(cats.get((String) it[4]))
                    .name((String) it[0]).description((String) it[1])
                    .price(new BigDecimal((String) it[2])).stock((Integer) it[3])
                    .imageUrl("https://picsum.photos/seed/" + ((String) it[0]).replace(' ', '-') + "/400/300")
                    .build());
        }

        coupons.save(Coupon.builder().code("WELCOME10").type(CouponType.PERCENT).value(new BigDecimal("10"))
                .minOrderAmount(new BigDecimal("20")).expiresAt(LocalDateTime.now().plusYears(1)).usageLimit(1000).build());
        coupons.save(Coupon.builder().code("SAVE5").type(CouponType.FIXED).value(new BigDecimal("5"))
                .minOrderAmount(new BigDecimal("30")).build());
        log.info("Seeded demo data. Admin login: admin@shop.com / Admin@123");
    }

    private User user(String email, String pw, String name, Role role, boolean approved) {
        return User.builder().email(email).passwordHash(encoder.encode(pw)).fullName(name).role(role)
                .enabled(true).sellerApproved(approved).build();
    }
}
