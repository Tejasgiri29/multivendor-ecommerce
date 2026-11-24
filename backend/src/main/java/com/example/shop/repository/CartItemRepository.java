package com.example.shop.repository;

import com.example.shop.entity.CartItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select ci from CartItem ci join fetch ci.product p join fetch p.seller where ci.user.id = :userId order by p.id")
    List<CartItem> findByUserIdWithProduct(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("delete from CartItem ci where ci.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
