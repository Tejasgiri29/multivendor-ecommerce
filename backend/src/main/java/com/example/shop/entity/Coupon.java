package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    /** Percentage (0-100) for PERCENT, currency amount for FIXED. */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    private LocalDateTime expiresAt;

    /** null = unlimited. */
    private Integer usageLimit;

    @Column(nullable = false)
    private int usedCount;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
