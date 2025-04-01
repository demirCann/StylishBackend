-- Ensure schema exists and set search path
CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;

-- This migration adds any missing constraints or indexes that might not be created in V1
-- It's safe to run on an existing database

-- Check and create gender enum type if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'gender') THEN
        CREATE TYPE gender AS ENUM ('Male', 'Female', 'Unisex');
    END IF;
END $$;

-- Ensure all tables have proper indexes
CREATE INDEX IF NOT EXISTS idx_product_updated ON "PRODUCTS" (date_added);
CREATE INDEX IF NOT EXISTS idx_cart_updated ON "CARTS" (updated_at);
CREATE INDEX IF NOT EXISTS idx_order_date ON "ORDERS" (order_date);
CREATE INDEX IF NOT EXISTS idx_review_date ON "REVIEWS" (review_date);

-- Add any missing foreign key constraints
DO $$
BEGIN
    -- Daha güvenli bir yaklaşım: Constraint isimleri yerine sadece ilişkileri kontrol ediyoruz
    -- CART_ITEMS tablosunda size_id sütunu için FK kontrolü
    IF NOT EXISTS (SELECT 1
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
    IF NOT EXISTS (SELECT 1
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
END $$; 