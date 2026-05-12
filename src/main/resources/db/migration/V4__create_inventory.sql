CREATE TABLE inventory (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT          NOT NULL UNIQUE REFERENCES products(id),
    quantity_on_hand    INT             NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved   INT             NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reserved_lte_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);
