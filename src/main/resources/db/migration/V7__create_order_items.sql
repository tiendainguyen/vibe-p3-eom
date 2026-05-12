CREATE TABLE order_items (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders(id),
    product_id   BIGINT         NOT NULL REFERENCES products(id),
    product_name VARCHAR(255)   NOT NULL,
    unit_price   NUMERIC(19, 2) NOT NULL CHECK (unit_price >= 0),
    quantity     INT            NOT NULL CHECK (quantity >= 1)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
