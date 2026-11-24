package com.example.shop.service;

import com.example.shop.dto.CouponDto;
import com.example.shop.dto.CouponRequest;
import com.example.shop.dto.CouponValidationDto;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.Coupon;
import com.example.shop.entity.CouponType;
import com.example.shop.exception.BadRequestException;
import com.example.shop.exception.ConflictException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.CartItemRepository;
import com.example.shop.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final CartItemRepository cartItemRepository;

    public CouponService(CouponRepository couponRepository, CartItemRepository cartItemRepository) {
        this.couponRepository = couponRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // ---------- admin CRUD

    @Transactional(readOnly = true)
    public List<CouponDto> list() {
        return couponRepository.findAll().stream().map(DtoMapper::toDto).toList();
    }

    @Transactional
    public CouponDto create(CouponRequest req) {
        String code = req.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) throw new ConflictException("Coupon code already exists");
        Coupon c = new Coupon();
        c.setCode(code);
        apply(c, req);
        return DtoMapper.toDto(couponRepository.save(c));
    }

    @Transactional
    public CouponDto update(Long id, CouponRequest req) {
        Coupon c = couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        String code = req.code().trim().toUpperCase();
        if (!c.getCode().equalsIgnoreCase(code) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Coupon code already exists");
        }
        c.setCode(code);
        apply(c, req);
        return DtoMapper.toDto(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!couponRepository.existsById(id)) throw new ResourceNotFoundException("Coupon not found");
        couponRepository.deleteById(id);
    }

    private void apply(Coupon c, CouponRequest req) {
        if (req.type() == CouponType.PERCENT && req.value().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percent coupons cannot exceed 100");
        }
        c.setType(req.type());
        c.setValue(req.value());
        c.setMinOrderAmount(req.minOrderAmount());
        c.setExpiresAt(req.expiresAt());
        c.setUsageLimit(req.usageLimit());
        c.setActive(req.active() == null || req.active());
    }

    // ---------- customer validation

    @Transactional(readOnly = true)
    public CouponValidationDto validate(Long userId, String code) {
        Coupon coupon = findByCode(code);
        List<CartItem> items = cartItemRepository.findByUserIdWithProduct(userId);
        BigDecimal subtotal = CartService.subtotal(items);
        BigDecimal discount = calculateDiscount(coupon, subtotal);
        return new CouponValidationDto(coupon.getCode(), subtotal, discount, subtotal.subtract(discount));
    }

    public Coupon findByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));
    }

    /**
     * Validates a coupon against an order subtotal and returns the discount (never more than the subtotal).
     * @throws BadRequestException when inactive, expired, exhausted or the minimum order is not met
     */
    public BigDecimal calculateDiscount(Coupon c, BigDecimal subtotal) {
        if (!c.isActive()) throw new BadRequestException("Coupon is not active");
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Coupon has expired");
        }
        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit()) {
            throw new BadRequestException("Coupon usage limit reached");
        }
        if (c.getMinOrderAmount() != null && subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Minimum order amount for this coupon is " + c.getMinOrderAmount());
        }
        BigDecimal discount = c.getType() == CouponType.PERCENT
                ? subtotal.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.getValue();
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
