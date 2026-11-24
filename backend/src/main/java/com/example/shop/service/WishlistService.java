package com.example.shop.service;

import com.example.shop.dto.ProductDto;
import com.example.shop.entity.Product;
import com.example.shop.entity.WishlistItem;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {
    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(WishlistItemRepository wishlistRepository, ProductRepository productRepository,
                           UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> list(Long userId) {
        return wishlistRepository.findByUserIdWithProduct(userId).stream()
                .map(w -> DtoMapper.toDto(w.getProduct())).toList();
    }

    @Transactional
    public List<ProductDto> add(Long userId, Long productId) {
        Product p = productRepository.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepository.save(WishlistItem.builder().user(userRepository.getReferenceById(userId)).product(p).build());
        }
        wishlistRepository.flush();
        return list(userId);
    }

    @Transactional
    public List<ProductDto> remove(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        return list(userId);
    }
}
