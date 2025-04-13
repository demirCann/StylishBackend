-- Create ENUM types for role and status
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN');
END IF;
    
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION');
END IF;
    
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'gender') THEN
CREATE TYPE gender AS ENUM ('Male', 'Female', 'Unisex');
END IF;
END $$;

-- Alter USERS table to add authentication and session management fields
ALTER TABLE "USERS"
    ADD COLUMN IF NOT EXISTS role user_role NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN IF NOT EXISTS status account_status NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS verification_token VARCHAR (128),
    ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR (128),
    ADD COLUMN IF NOT EXISTS password_reset_token_expiry TIMESTAMP,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_failed_login_attempt TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_login_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Alter PRODUCT_DETAILS table to use gender enum
ALTER TABLE "PRODUCT_DETAILS"
ALTER
COLUMN gender TYPE gender USING gender::gender;

-- Alter ORDERS table to add shipping_provider if not exists
DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'ORDERS' AND column_name = 'shipping_provider'
    ) THEN
ALTER TABLE "ORDERS"
    ADD COLUMN shipping_provider VARCHAR(50);
END IF;
END $$;

-- Create refresh token table
CREATE TABLE IF NOT EXISTS "REFRESH_TOKENS"
(
    id
    SERIAL
    PRIMARY
    KEY,
    user_id
    INTEGER
    NOT
    NULL
    REFERENCES
    "USERS"
(
    id
) ON DELETE CASCADE,
    token VARCHAR
(
    255
) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON "REFRESH_TOKENS"(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON "REFRESH_TOKENS"(user_id);
CREATE INDEX IF NOT EXISTS idx_users_status ON "USERS"(status);
CREATE INDEX IF NOT EXISTS idx_users_role ON "USERS"(role);
CREATE INDEX IF NOT EXISTS idx_users_active ON "USERS"(is_active);
CREATE INDEX IF NOT EXISTS idx_order_shipping ON "ORDERS"(shipping_provider);
CREATE INDEX IF NOT EXISTS idx_product_updated ON "PRODUCTS"(date_added);
CREATE INDEX IF NOT EXISTS idx_cart_updated ON "CARTS"(updated_at);
CREATE INDEX IF NOT EXISTS idx_order_date ON "ORDERS"(order_date);
CREATE INDEX IF NOT EXISTS idx_review_date ON "REVIEWS"(review_date);

-- Add any missing foreign key constraints
DO
$$
BEGIN
    -- CART_ITEMS tablosunda size_id sütunu için FK kontrolü
    IF
NOT EXISTS (SELECT 1
                   FROM pg_constraint pc
                            JOIN pg_attribute pa1 ON pa1.attrelid = pc.conrelid AND pa1.attnum = ANY (pc.conkey)
                            JOIN pg_attribute pa2 ON pa2.attrelid = pc.confrelid AND pa2.attnum = ANY (pc.confkey)
                   WHERE pc.conrelid = '"CART_ITEMS"'::regclass
                     AND pc.confrelid = '"SIZES"'::regclass
                     AND pa1.attname = 'size_id'
                     AND pa2.attname = 'id'
                     AND pc.contype = 'f') THEN
ALTER TABLE "CART_ITEMS"
    ADD CONSTRAINT fk_cart_item_size
        FOREIGN KEY (size_id) REFERENCES "SIZES" (id) ON DELETE SET NULL;
END IF;

    -- CART_ITEMS tablosunda color_id sütunu için FK kontrolü
    IF
NOT EXISTS (SELECT 1
                   FROM pg_constraint pc
                            JOIN pg_attribute pa1 ON pa1.attrelid = pc.conrelid AND pa1.attnum = ANY (pc.conkey)
                            JOIN pg_attribute pa2 ON pa2.attrelid = pc.confrelid AND pa2.attnum = ANY (pc.confkey)
                   WHERE pc.conrelid = '"CART_ITEMS"'::regclass
                     AND pc.confrelid = '"COLORS"'::regclass
                     AND pa1.attname = 'color_id'
                     AND pa2.attname = 'id'
                     AND pc.contype = 'f') THEN
ALTER TABLE "CART_ITEMS"
    ADD CONSTRAINT fk_cart_item_color
        FOREIGN KEY (color_id) REFERENCES "COLORS" (id) ON DELETE SET NULL;
END IF;
    
    -- ORDER_ITEMS tablosunda size_id sütunu için FK kontrolü
    IF
NOT EXISTS (SELECT 1
                   FROM pg_constraint pc
                            JOIN pg_attribute pa1 ON pa1.attrelid = pc.conrelid AND pa1.attnum = ANY (pc.conkey)
                            JOIN pg_attribute pa2 ON pa2.attrelid = pc.confrelid AND pa2.attnum = ANY (pc.confkey)
                   WHERE pc.conrelid = '"ORDER_ITEMS"'::regclass
                     AND pc.confrelid = '"SIZES"'::regclass
                     AND pa1.attname = 'size_id'
                     AND pa2.attname = 'id'
                     AND pc.contype = 'f') THEN
ALTER TABLE "ORDER_ITEMS"
    ADD CONSTRAINT fk_order_item_size
        FOREIGN KEY (size_id) REFERENCES "SIZES" (id) ON DELETE SET NULL;
END IF;

    -- ORDER_ITEMS tablosunda color_id sütunu için FK kontrolü
    IF
NOT EXISTS (SELECT 1
                   FROM pg_constraint pc
                            JOIN pg_attribute pa1 ON pa1.attrelid = pc.conrelid AND pa1.attnum = ANY (pc.conkey)
                            JOIN pg_attribute pa2 ON pa2.attrelid = pc.confrelid AND pa2.attnum = ANY (pc.confkey)
                   WHERE pc.conrelid = '"ORDER_ITEMS"'::regclass
                     AND pc.confrelid = '"COLORS"'::regclass
                     AND pa1.attname = 'color_id'
                     AND pa2.attname = 'id'
                     AND pc.contype = 'f') THEN
ALTER TABLE "ORDER_ITEMS"
    ADD CONSTRAINT fk_order_item_color
        FOREIGN KEY (color_id) REFERENCES "COLORS" (id) ON DELETE SET NULL;
END IF;
END $$;

-- Add test admin user if not exists
-- Password: admin123 (with SHA-256 hash)
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM "USERS" WHERE email = 'admin@stylish.com') THEN
        INSERT INTO "USERS" (
            email, 
            password_hash, 
            first_name, 
            last_name, 
            role, 
            status, 
            registration_date,
            is_active
        )
        VALUES (
            'admin@stylish.com', 
            'DF0Q1/jnQgT7xMiAOD1kveZQTGIhPJ6JQoCQdcoO9cE=', 
            'Admin', 
            'User', 
            'ADMIN', 
            'ACTIVE', 
            CURRENT_TIMESTAMP,
            TRUE
        );
END IF;
END $$;

-- Add test customer user if not exists
-- Password: password123 (with SHA-256 hash)
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM "USERS" WHERE email = 'customer@example.com') THEN
        INSERT INTO "USERS" (
            email,
            password_hash,
            first_name,
            last_name,
            phone_number,
            role,
            status,
            registration_date,
            is_active
        )
        VALUES (
            'customer@example.com',
            'dfAG15qVGhkXZyQfbzY1/GwNbUSDV8+LbWxAhCTZefs=',
            'Test',
            'Customer',
            '+905551234567',
            'CUSTOMER',
            'ACTIVE',
            CURRENT_TIMESTAMP,
            TRUE
        );
END IF;
END $$; 