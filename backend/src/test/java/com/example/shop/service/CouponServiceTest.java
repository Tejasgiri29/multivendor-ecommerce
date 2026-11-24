package com.example.shop.service;

import com.example.shop.entity.Coupon;
import com.example.shop.entity.CouponType;
import com.example.shop.exception.BadRequestException;
import com.example.shop.repository.CartItemRepository;
import com.example.shop.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {
    @Mock CouponRepository couponRepository;
    @Mock CartItemRepository cartItemRepository;

    private CouponService service() {
        return new CouponService(couponRepository, cartItemRepository);
    }

    private Coupon coupon(CouponType type, String value) {
        return Coupon.builder().id(1L).code("C").type(type).value(new BigDecimal(value)).active(true).build();
    }

    @Test
    void percentDiscountIsRounded() {
        assertEquals(new BigDecimal("2.55"), service().calculateDiscount(coupon(CouponType.PERCENT, "10"), new BigDecimal("25.50")));
    }

    @Test
    void fixedDiscountIsCappedAtSubtotal() {
        assertEquals(new BigDecimal("8.00"), service().calculateDiscount(coupon(CouponType.FIXED, "20"), new BigDecimal("8.00")));
        assertEquals(new BigDecimal("5.00"), service().calculateDiscount(coupon(CouponType.FIXED, "5"), new BigDecimal("50.00")));
    }

    @Test
    void rejectsExpiredCoupon() {
        Coupon c = coupon(CouponType.PERCENT, "10");
        c.setExpiresAt(LocalDateTime.now().minusDays(1));
        assertThrows(BadRequestException.class, () -> service().calculateDiscount(c, new BigDecimal("100")));
    }

    @Test
    void rejectsBelowMinimumOrder() {
        Coupon c = coupon(CouponType.PERCENT, "10");
        c.setMinOrderAmount(new BigDecimal("50"));
        assertThrows(BadRequestException.class, () -> service().calculateDiscount(c, new BigDecimal("49.99")));
        assertDoesNotThrow(() -> service().calculateDiscount(c, new BigDecimal("50.00")));
    }

    @Test
    void rejectsExhaustedAndInactiveCoupons() {
        Coupon c = coupon(CouponType.FIXED, "5");
        c.setUsageLimit(2);
        c.setUsedCount(2);
        assertThrows(BadRequestException.class, () -> service().calculateDiscount(c, new BigDecimal("100")));
        Coupon off = coupon(CouponType.FIXED, "5");
        off.setActive(false);
        assertThrows(BadRequestException.class, () -> service().calculateDiscount(off, new BigDecimal("100")));
    }
}
