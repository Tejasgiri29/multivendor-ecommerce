package com.example.shop.integration;

import com.example.shop.entity.Role;
import com.example.shop.entity.User;
import com.example.shop.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end flow through the real security filter chain, controllers, services and schema. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;

    private JsonNode json(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder b, String token, String body) {
        b.contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private String register(String email, String role) throws Exception {
        MvcResult r = mvc.perform(withJson(post("/api/auth/register"), null,
                "{\"email\":\"" + email + "\",\"password\":\"Passw0rd!\",\"fullName\":\"Tester\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return json(r).get("token").asText();
    }

    private String adminToken() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@test.com";
        userRepository.save(User.builder().email(email).passwordHash(encoder.encode("Admin@123")).fullName("Admin")
                .role(Role.ADMIN).sellerApproved(true).build());
        MvcResult r = mvc.perform(withJson(post("/api/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"Admin@123\"}")).andExpect(status().isOk()).andReturn();
        return json(r).get("token").asText();
    }

    @Test
    void fullMarketplaceFlow() throws Exception {
        String admin = adminToken();
        String sellerEmail = "seller-" + UUID.randomUUID() + "@test.com";
        String seller = register(sellerEmail, "SELLER");
        String customer = register("cust-" + UUID.randomUUID() + "@test.com", "CUSTOMER");

        // admin creates a category and a coupon
        String catName = "Gadgets-" + UUID.randomUUID();
        long catId = json(mvc.perform(withJson(post("/api/admin/categories"), admin, "{\"name\":\"" + catName + "\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        String code = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mvc.perform(withJson(post("/api/admin/coupons"), admin,
                "{\"code\":\"" + code + "\",\"type\":\"PERCENT\",\"value\":10,\"minOrderAmount\":10,\"expiresAt\":\"2099-01-01T00:00:00\",\"usageLimit\":5}"))
                .andExpect(status().isCreated());

        // unapproved seller cannot publish
        String productBody = "{\"name\":\"Widget\",\"description\":\"d\",\"price\":20.00,\"stock\":3,\"categoryId\":" + catId + "}";
        mvc.perform(withJson(post("/api/seller/products"), seller, productBody)).andExpect(status().isForbidden());

        // admin approves seller
        long sellerId = json(mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + seller))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();
        mvc.perform(post("/api/admin/users/" + sellerId + "/approve-seller").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sellerApproved", is(true)));

        // seller publishes 3 products
        long productId = json(mvc.perform(withJson(post("/api/seller/products"), seller, productBody))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        for (String n : new String[]{"Gizmo", "Doohickey"}) {
            mvc.perform(withJson(post("/api/seller/products"), seller,
                    "{\"name\":\"" + n + "\",\"price\":5.00,\"stock\":9,\"categoryId\":" + catId + "}")).andExpect(status().isCreated());
        }

        // public search with server-side pagination and sorting
        mvc.perform(get("/api/products").param("categoryId", String.valueOf(catId)).param("size", "2").param("page", "0")
                        .param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content[0].name", is("Widget")));
        mvc.perform(get("/api/products").param("categoryId", String.valueOf(catId)).param("keyword", "gizm")
                        .param("minPrice", "1").param("maxPrice", "6"))
                .andExpect(jsonPath("$.totalElements", is(1)));

        // authorization: anonymous -> 401, customer on admin/seller endpoints -> 403
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status", is(401)));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/seller/orders").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());

        // wishlist + cart
        mvc.perform(post("/api/wishlist/" + productId).header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(withJson(post("/api/cart/items"), customer, "{\"productId\":" + productId + ",\"quantity\":4}"))
                .andExpect(status().isBadRequest());   // only 3 in stock
        mvc.perform(withJson(post("/api/cart/items"), customer, "{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.subtotal", is(40.0)));

        // coupon preview + checkout
        mvc.perform(withJson(post("/api/coupons/validate"), customer, "{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discount", is(4.0))).andExpect(jsonPath("$.total", is(36.0)));
        MvcResult order = mvc.perform(withJson(post("/api/orders/checkout"), customer,
                "{\"shippingAddress\":\"1 Main St\",\"couponCode\":\"" + code + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.total", is(36.0))).andReturn();
        long orderId = json(order).get("id").asLong();
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stock", is(1)));
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + customer)).andExpect(jsonPath("$.items", hasSize(0)));

        // lifecycle: seller cannot ship before payment; pay -> ship -> deliver; cannot cancel afterwards
        mvc.perform(withJson(put("/api/orders/" + orderId + "/status"), seller, "{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders/" + orderId + "/pay").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("PAID")));
        mvc.perform(get("/api/seller/orders").header("Authorization", "Bearer " + seller))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id", is((int) orderId)));
        mvc.perform(withJson(put("/api/orders/" + orderId + "/status"), seller, "{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("SHIPPED")));
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + customer))
                .andExpect(status().isBadRequest());
        mvc.perform(withJson(put("/api/orders/" + orderId + "/status"), seller, "{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    void validationAndAuthErrorsUseConsistentJson() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), null, "{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.path", is("/api/auth/register")));

        mvc.perform(withJson(post("/api/auth/login"), null, "{\"email\":\"nobody@test.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error", is("Unauthorized")));

        mvc.perform(get("/api/products/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status", is(404)));
        mvc.perform(get("/api/cart").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUsersAreLockedOut() throws Exception {
        String email = "victim-" + UUID.randomUUID() + "@test.com";
        String token = register(email, "CUSTOMER");
        long id = json(mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andReturn()).get("id").asLong();
        mvc.perform(put("/api/admin/users/" + id + "/enabled").param("value", "false").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled", is(false)));
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(withJson(post("/api/auth/login"), null, "{\"email\":\"" + email + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isForbidden());
    }
}
