package com.example.shop.repository;

import com.example.shop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    @Query(value = "select o from CustomerOrder o where exists (select 1 from OrderItem i where i.orderRef = o and i.seller.id = :sellerId)",
           countQuery = "select count(o) from CustomerOrder o where exists (select 1 from OrderItem i where i.orderRef = o and i.seller.id = :sellerId)")
    Page<Order> findContainingSeller(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("select case when count(i) > 0 then true else false end from OrderItem i where i.orderRef.id = :orderId and i.seller.id = :sellerId")
    boolean orderContainsSeller(@Param("orderId") Long orderId, @Param("sellerId") Long sellerId);

    @Override
    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findAll(Pageable pageable);
}
