package com.example.shop.service;

import com.example.shop.dto.*;
import com.example.shop.entity.Category;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.exception.ForbiddenException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    /** Public catalogue search with server-side pagination. sort: newest | price_asc | price_desc | name. */
    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                           String sort, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(root.get("active")));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("description")), like, '\\')));
            }
            if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
            if (minPrice != null) ps.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) ps.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Product> result = productRepository.findAll(spec, pageable(sort, page, size));
        return PageResponse.of(result, DtoMapper::toDto);
    }

    private PageRequest pageable(String sort, int page, int size) {
        Sort s = switch (sort == null ? "newest" : sort) {
            case "price_asc" -> Sort.by("price").ascending().and(Sort.by("id"));
            case "price_desc" -> Sort.by("price").descending().and(Sort.by("id"));
            case "name" -> Sort.by("name").ascending().and(Sort.by("id"));
            default -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), s);
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return DtoMapper.toDto(productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found")));
    }

    // ---------- seller operations

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> listForSeller(Long sellerId, int page, int size) {
        return PageResponse.of(productRepository.findBySellerIdAndActiveTrue(sellerId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending())),
                DtoMapper::toDto);
    }

    @Transactional
    public ProductDto create(Long sellerId, ProductRequest req) {
        User seller = userRepository.findById(sellerId).orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        if (!seller.isSellerApproved()) {
            throw new ForbiddenException("Your seller account is awaiting admin approval");
        }
        Product p = new Product();
        p.setSeller(seller);
        apply(p, req);
        return DtoMapper.toDto(productRepository.save(p));
    }

    @Transactional
    public ProductDto update(Long sellerId, Long productId, ProductRequest req) {
        Product p = ownedProduct(sellerId, productId);
        apply(p, req);
        return DtoMapper.toDto(p);
    }

    /** Soft delete so existing orders keep their references. */
    @Transactional
    public void delete(Long sellerId, Long productId) {
        ownedProduct(sellerId, productId).setActive(false);
    }

    private Product ownedProduct(Long sellerId, Long productId) {
        Product p = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!p.isActive()) throw new ResourceNotFoundException("Product not found");
        return p;
    }

    private void apply(Product p, ProductRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setStock(req.stock());
        p.setImageUrl(req.imageUrl());
        p.setCategory(category);
    }
}
