package com.example.shop.repository;

import com.example.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Optional<Product> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = {"category", "seller"})
    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Atomic, conditional stock decrement. Returns the number of rows updated: 1 on success,
     * 0 when there is not enough stock (the row is untouched). Never allows stock to go negative,
     * even under concurrent checkouts, because the check and the write happen in one SQL statement.
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /** Restores stock (used when an order is cancelled). */
    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Query("select p.stock from Product p where p.id = :id")
    Integer findStockById(@Param("id") Long id);
}
