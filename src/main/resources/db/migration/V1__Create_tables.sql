-- Drop constraints if they exist to avoid the duplication error
DO $$
BEGIN
    -- Drop existing constraints to avoid duplication errors
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_email_unique') THEN
ALTER TABLE "USERS" DROP CONSTRAINT user_email_unique;
    END IF;
    
    -- Check if order_status type exists and create it if it doesn't
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        CREATE TYPE order_status AS ENUM ('Pending', 'Confirmed', 'Processing', 'Shipped', 'Delivered', 'Cancelled', 'Returned');
    END IF;
END $$;

-- Create USERS table
CREATE TABLE IF NOT EXISTS "USERS"
(
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Add unique constraint to email
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_unique ON "USERS" (email);

-- Create CATEGORIES table
CREATE TABLE IF NOT EXISTS "CATEGORIES"
(
    id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_category_id INT,
    CONSTRAINT fk_category_parent FOREIGN KEY
(
    parent_category_id
) REFERENCES "CATEGORIES"
(
    id
) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_category_parent ON "CATEGORIES" (parent_category_id);

-- Create SIZES table
CREATE TABLE IF NOT EXISTS "SIZES"
(
    id SERIAL PRIMARY KEY,
    size_name VARCHAR(20) NOT NULL,
    size_type VARCHAR(50) NOT NULL
);

-- Create COLORS table
CREATE TABLE IF NOT EXISTS "COLORS"
(
    id SERIAL PRIMARY KEY,
    color_name VARCHAR(50) NOT NULL,
    hex_code VARCHAR(7)
);

-- Create PRODUCTS table
CREATE TABLE IF NOT EXISTS "PRODUCTS"
(
    id SERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER DEFAULT 0,
    category_id INT,
    brand VARCHAR(100),
    date_added TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_product_category FOREIGN KEY
(
    category_id
) REFERENCES "CATEGORIES"
(
    id
) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_product_category ON "PRODUCTS" (category_id);
CREATE INDEX IF NOT EXISTS idx_product_name ON "PRODUCTS" (product_name);
CREATE INDEX IF NOT EXISTS idx_product_active ON "PRODUCTS" (is_active);

-- Create PRODUCT_DETAILS table
CREATE TABLE IF NOT EXISTS "PRODUCT_DETAILS"
(
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL,
    color VARCHAR(50),
    size VARCHAR(20),
    material VARCHAR(100),
    made_in VARCHAR(100),
    care_instructions TEXT,
    gender VARCHAR(10) NOT NULL,
    season VARCHAR(50),
    CONSTRAINT fk_product_detail_product FOREIGN KEY
(
    product_id
) REFERENCES "PRODUCTS"
(
    id
) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_detail_product ON "PRODUCT_DETAILS" (product_id);

-- Create PRODUCT_IMAGES table
CREATE TABLE IF NOT EXISTS "PRODUCT_IMAGES"
(
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    CONSTRAINT fk_product_image_product FOREIGN KEY
(
    product_id
) REFERENCES "PRODUCTS"
(
    id
) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_image_product ON "PRODUCT_IMAGES" (product_id);

-- Create ADDRESSES table
CREATE TABLE IF NOT EXISTS "ADDRESSES"
(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    address_title VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20),
    country VARCHAR(50) DEFAULT 'Turkey',
    CONSTRAINT fk_address_user FOREIGN KEY
(
    user_id
) REFERENCES "USERS"
(
    id
) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_address_user ON "ADDRESSES" (user_id);

-- Create CARTS table
CREATE TABLE IF NOT EXISTS "CARTS"
(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    total_amount DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY
(
    user_id
) REFERENCES "USERS"
(
    id
) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cart_user ON "CARTS" (user_id);

-- Create CART_ITEMS table
CREATE TABLE IF NOT EXISTS "CART_ITEMS"
(
    id SERIAL PRIMARY KEY,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT DEFAULT 1,
    size_id INT,
    color_id INT,
    unit_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_cart_item_cart FOREIGN KEY
(
    cart_id
) REFERENCES "CARTS"
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY
(
    product_id
) REFERENCES "PRODUCTS"
(
    id
)
  ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_size FOREIGN KEY
(
    size_id
) REFERENCES "SIZES"
(
    id
)
  ON DELETE SET NULL,
    CONSTRAINT fk_cart_item_color FOREIGN KEY
(
    color_id
) REFERENCES "COLORS"
(
    id
)
  ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_cart_item_cart_product ON "CART_ITEMS" (cart_id, product_id);

-- Create DISCOUNTS table
CREATE TABLE IF NOT EXISTS "DISCOUNTS"
(
    id SERIAL PRIMARY KEY,
    discount_code VARCHAR(50) NOT NULL,
    discount_rate DECIMAL(5, 2) NOT NULL,
    min_cart_amount DECIMAL(10, 2) DEFAULT 0,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INT,
    usage_count INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_discount_code ON "DISCOUNTS" (discount_code);
CREATE INDEX IF NOT EXISTS idx_discount_date_range ON "DISCOUNTS" (start_date, end_date);

-- Create ORDERS table with enum
CREATE TABLE IF NOT EXISTS "ORDERS"
(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    address_id INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50) NOT NULL,
    order_status order_status DEFAULT 'Pending',
    tracking_number VARCHAR(50),
    shipping_fee DECIMAL(10, 2) DEFAULT 0,
    CONSTRAINT fk_order_user FOREIGN KEY
(
    user_id
) REFERENCES "USERS"
(
    id
),
    CONSTRAINT fk_order_address FOREIGN KEY
(
    address_id
) REFERENCES "ADDRESSES"
(
    id
)
);

CREATE INDEX IF NOT EXISTS idx_order_user ON "ORDERS" (user_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON "ORDERS" (order_status);

-- Create ORDER_ITEMS table
CREATE TABLE IF NOT EXISTS "ORDER_ITEMS"
(
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    size_id INT,
    color_id INT,
    CONSTRAINT fk_order_item_order FOREIGN KEY
(
    order_id
) REFERENCES "ORDERS"
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY
(
    product_id
) REFERENCES "PRODUCTS"
(
    id
),
    CONSTRAINT fk_order_item_size FOREIGN KEY
(
    size_id
) REFERENCES "SIZES"
(
    id
)
  ON DELETE SET NULL,
    CONSTRAINT fk_order_item_color FOREIGN KEY
(
    color_id
) REFERENCES "COLORS"
(
    id
)
  ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_order_item_order ON "ORDER_ITEMS" (order_id);

-- Create REVIEWS table
CREATE TABLE IF NOT EXISTS "REVIEWS"
(
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT,
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_approved BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_review_product FOREIGN KEY
(
    product_id
) REFERENCES "PRODUCTS"
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY
(
    user_id
) REFERENCES "USERS"
(
    id
)
  ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_review_product ON "REVIEWS" (product_id);
CREATE INDEX IF NOT EXISTS idx_review_user ON "REVIEWS" (user_id);
CREATE INDEX IF NOT EXISTS idx_review_approved ON "REVIEWS" (is_approved); 