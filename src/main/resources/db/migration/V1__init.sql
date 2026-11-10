CREATE TABLE product (
    id         BIGSERIAL PRIMARY KEY,
    sku        VARCHAR(64)    NOT NULL UNIQUE,
    name       VARCHAR(200)   NOT NULL,
    category   VARCHAR(100)   NOT NULL,
    price      NUMERIC(12, 2) NOT NULL,
    stock      INTEGER        NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_category ON product (category);

INSERT INTO product (sku, name, category, price, stock) VALUES
    ('AUD-001', 'Studio Headphones',      'audio',       199.00, 25),
    ('AUD-002', 'Bluetooth Speaker',      'audio',        89.50,  0),
    ('AUD-003', 'USB Microphone',         'audio',       129.99, 12),
    ('ACC-001', 'Braided USB-C Cable',    'accessories',  14.90, 300),
    ('ACC-002', 'Laptop Stand',           'accessories',  49.00, 40),
    ('WEA-001', 'Fitness Tracker',        'wearables',    79.00, 18);
