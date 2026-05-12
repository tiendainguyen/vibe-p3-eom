CREATE TABLE cart_items (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    product_id  BIGINT      NOT NULL REFERENCES products(id),
    quantity    INT         NOT NULL CHECK (quantity >= 1),
    added_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_cart_items_user_id ON cart_items (user_id);
