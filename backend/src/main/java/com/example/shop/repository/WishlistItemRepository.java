package com.example.shop.repository;

import com.example.shop.entity.WishlistItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @Query("select w from WishlistItem w join fetch w.product p join fetch p.category join fetch p.seller where w.user.id = :userId order by w.id desc")
    List<WishlistItem> findByUserIdWithProduct(@Param("userId") Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("delete from WishlistItem w where w.user.id = :userId and w.product.id = :productId")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
