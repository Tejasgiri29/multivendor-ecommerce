-- Core schema for the multi-vendor e-commerce platform (PostgreSQL)

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    seller_approved BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    seller_id   BIGINT        NOT NULL REFERENCES users (id),
    category_id BIGINT        NOT NULL REFERENCES categories (id),
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    price       NUMERIC(12,2) NOT NULL,
    stock       INTEGER       NOT NULL,
    image_url   VARCHAR(500),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT ck_products_stock_nonneg CHECK (stock >= 0),
    CONSTRAINT ck_products_price_pos CHECK (price > 0)
);
CREATE INDEX idx_products_name     ON products (name);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_price    ON products (price);
CREATE INDEX idx_products_seller   ON products (seller_id);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT  NOT NULL REFERENCES products (id),
    quantity   INTEGER NOT NULL,
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT ck_cart_qty_pos CHECK (quantity > 0)
);
CREATE INDEX idx_cart_user ON cart_items (user_id);

CREATE TABLE wishlist_items (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id),
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
);
CREATE INDEX idx_wishlist_user ON wishlist_items (user_id);

CREATE TABLE coupons (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(50)   NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    discount_value   NUMERIC(12,2) NOT NULL,
    min_order_amount NUMERIC(12,2),
    expires_at       TIMESTAMP,
    usage_limit      INTEGER,
    used_count       INTEGER       NOT NULL DEFAULT 0,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_coupons_code UNIQUE (code)
);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT        NOT NULL REFERENCES users (id),
    status           VARCHAR(20)   NOT NULL,
    subtotal         NUMERIC(12,2) NOT NULL,
    discount         NUMERIC(12,2) NOT NULL,
    total            NUMERIC(12,2) NOT NULL,
    coupon_code      VARCHAR(50),
    shipping_address VARCHAR(500)  NOT NULL,
    created_at       TIMESTAMP     NOT NULL
);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status   ON orders (status);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products (id),
    seller_id    BIGINT        NOT NULL REFERENCES users (id),
    product_name VARCHAR(200)  NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    quantity     INTEGER       NOT NULL
);
CREATE INDEX idx_order_items_order  ON order_items (order_id);
CREATE INDEX idx_order_items_seller ON order_items (seller_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);
