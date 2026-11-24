package com.example.shop.service;

import com.example.shop.entity.*;

import java.math.BigDecimal;

/** Small factory helpers for unit tests. */
final class TestData {
    private TestData() {}

    static User user(Long id, Role role) {
        return User.builder().id(id).email("u" + id + "@test.com").passwordHash("hash").fullName("User " + id)
                .role(role).enabled(true).sellerApproved(role == Role.SELLER).build();
    }

    static Product product(Long id, String price, int stock, User seller) {
        return Product.builder().id(id).name("Product " + id).price(new BigDecimal(price)).stock(stock)
                .seller(seller).category(Category.builder().id(1L).name("Cat").build()).active(true).build();
    }

    static CartItem cartItem(User user, Product product, int qty) {
        return CartItem.builder().user(user).product(product).quantity(qty).build();
    }
}
