-- ============================================================
-- Migration 002: Reviews + Ratings (Phase 6)
-- Safe for existing databases (idempotent, run on every boot).
-- Fresh installs already get the table from db/schema.sql; this
-- migration only fills the gap for databases created before
-- the reviews feature existed.
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT      NOT NULL,
    product_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    rating      INT         NOT NULL,
    review_text VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reviews_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_reviews_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews (product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews (user_id);