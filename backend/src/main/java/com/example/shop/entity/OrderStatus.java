package com.example.shop.entity;

import java.util.EnumSet;
import java.util.Set;

/** Order lifecycle: PLACED -> PAID -> SHIPPED -> DELIVERED, with CANCELLED allowed before shipping. */
public enum OrderStatus {
    PLACED, PAID, SHIPPED, DELIVERED, CANCELLED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PLACED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }
}
