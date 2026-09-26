CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- One inventory row per product (product master data itself lives in MongoDB).
-- product_id here is the Mongo ObjectId stored as a string.
CREATE TABLE inventory (
    id             BIGSERIAL PRIMARY KEY,
    product_id     VARCHAR(64) NOT NULL UNIQUE,
    quantity       INT NOT NULL CHECK (quantity >= 0),
    version        BIGINT NOT NULL DEFAULT 0   -- optimistic locking to prevent overselling
);

CREATE TABLE orders (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id),
    status         VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    total_amount   NUMERIC(12,2) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id     VARCHAR(64) NOT NULL,       -- references Mongo product _id
    product_name   VARCHAR(255) NOT NULL,      -- snapshot at time of order
    unit_price     NUMERIC(12,2) NOT NULL,     -- snapshot at time of order
    quantity       INT NOT NULL CHECK (quantity > 0)
);

CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL REFERENCES orders(id),
    amount         NUMERIC(12,2) NOT NULL,
    status         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    method         VARCHAR(30) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
