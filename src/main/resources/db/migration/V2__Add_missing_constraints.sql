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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cart_item_size') THEN
ALTER TABLE "CART_ITEMS"
    ADD CONSTRAINT fk_cart_item_size
        FOREIGN KEY (size_id) REFERENCES "SIZES" (id) ON DELETE SET NULL;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cart_item_color') THEN
ALTER TABLE "CART_ITEMS"
    ADD CONSTRAINT fk_cart_item_color
        FOREIGN KEY (color_id) REFERENCES "COLORS" (id) ON DELETE SET NULL;
    END IF;
END $$; 