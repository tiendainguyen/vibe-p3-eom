CREATE TYPE order_status AS ENUM (
    'PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'
);

CREATE TABLE orders (
    id                        BIGSERIAL       PRIMARY KEY,
    user_id                   BIGINT          NOT NULL REFERENCES users(id),
    status                    order_status    NOT NULL DEFAULT 'PENDING',
    total_amount              NUMERIC(19, 2)  NOT NULL CHECK (total_amount >= 0),
    stripe_payment_intent_id  VARCHAR(255),
    created_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id  ON orders (user_id);
CREATE INDEX idx_orders_status   ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
