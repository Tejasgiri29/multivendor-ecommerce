package com.example.shop.repository;

import com.example.shop.entity.Coupon;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    /** Atomically consumes one use; returns 0 if the usage limit has been reached. */
    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)")
    int incrementUsage(@Param("id") Long id);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount - 1 where c.id = :id and c.usedCount > 0")
    int decrementUsage(@Param("id") Long id);
}
