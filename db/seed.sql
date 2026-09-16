-- DhatchinaMart seed data
-- Only the admin account is pre-created. Everyone else registers
-- themselves as a Buyer or Seller on the /register page.
-- Passwords are real bcrypt hashes:
--   admin@dhatchinamart.com / Admin@123
-- Demo mobile number for the seeded admin (used for OTP login):
--   9876500001
-- Products are owned by the platform admin (id 1) so the catalog is always populated.

INSERT INTO users (id, name, email, mobile_number, password_hash, role) VALUES
  (1, 'Platform Admin', 'admin@dhatchinamart.com', '9876500001', '$2a$10$i3Xw2lMwEHYovzkspFaqOu/aFUkFU90ANtgkNCdz5z9QKWYHgIlRO', 'ADMIN');

-- ==================================================
-- PRODUCT CATALOG (40 products, 5 categories x 8)
-- ==================================================

-- ELECTRONICS (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (1,  1, 'Wireless Bluetooth Headphones', 'Comfortable wireless headphones with Bluetooth connectivity and long battery life.', 1499.00, 25, 'Electronics', 'images/products/headphones.jpg'),
  (2,  1, 'Mechanical Keyboard', 'Compact mechanical keyboard suitable for gaming, programming and everyday use.', 2299.00, 20, 'Electronics', 'images/products/keyboard.jpg'),
  (3,  1, 'Wireless Mouse', 'Ergonomic wireless mouse with precise tracking and comfortable grip.', 799.00, 30, 'Electronics', 'images/products/mouse.jpg'),
  (4,  1, 'Smart Watch', 'Smart watch with activity tracking, notifications and fitness features.', 2999.00, 15, 'Electronics', 'images/products/smartwatch.jpg'),
  (5,  1, 'Portable Bluetooth Speaker', 'Compact portable speaker with Bluetooth connectivity and clear audio.', 1299.00, 20, 'Electronics', 'images/products/speaker.jpg'),
  (6,  1, 'USB-C Fast Charger', 'Fast USB-C charger suitable for compatible smartphones and electronic devices.', 899.00, 35, 'Electronics', 'images/products/charger.jpg'),
  (7,  1, 'Power Bank 20,000mAh', 'High-capacity portable power bank for charging devices while travelling.', 1599.00, 25, 'Electronics', 'images/products/powerbank.jpg'),
  (8,  1, 'Laptop Stand', 'Adjustable laptop stand designed for comfortable desk usage and improved posture.', 1099.00, 20, 'Electronics', 'images/products/laptopstand.jpg');

-- ACCESSORIES (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (9,  1, 'Leather Wallet', 'Compact leather wallet with multiple card and cash compartments.', 699.00, 30, 'Accessories', 'images/products/wallet.jpg'),
  (10, 1, 'Canvas Backpack', 'Durable everyday backpack suitable for college, work and travel.', 1299.00, 25, 'Accessories', 'images/products/backpack.jpg'),
  (11, 1, 'Sunglasses', 'Stylish sunglasses suitable for everyday outdoor use.', 899.00, 30, 'Accessories', 'images/products/sunglasses.jpg'),
  (12, 1, 'Analog Wrist Watch', 'Classic analog wrist watch with a clean and elegant design.', 1499.00, 15, 'Accessories', 'images/products/watch.jpg'),
  (13, 1, 'Travel Pouch', 'Compact travel pouch for organizing small personal items.', 499.00, 40, 'Accessories', 'images/products/pouch.jpg'),
  (14, 1, 'Keychain Set', 'Set of simple and durable keychains for everyday use.', 299.00, 50, 'Accessories', 'images/products/keychain.jpg'),
  (15, 1, 'Card Holder', 'Slim card holder designed for carrying essential cards.', 399.00, 40, 'Accessories', 'images/products/cardholder.jpg'),
  (16, 1, 'Belt', 'Adjustable casual belt suitable for everyday outfits.', 599.00, 30, 'Accessories', 'images/products/belt.jpg');

-- BOOKS (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (17, 1, 'Clean Code', 'A practical guide to writing readable, maintainable and professional software.', 599.00, 20, 'Books', 'images/products/clean-code.jpg'),
  (18, 1, 'The Pragmatic Programmer', 'A practical software development guide covering programming practices and principles.', 699.00, 20, 'Books', 'images/products/pragmatic-programmer.jpg'),
  (19, 1, 'Introduction to Algorithms', 'Comprehensive reference covering fundamental algorithms and data structures.', 899.00, 15, 'Books', 'images/products/intro-algorithms.jpg'),
  (20, 1, 'Atomic Habits', 'A practical guide to building good habits and improving daily routines.', 499.00, 25, 'Books', 'images/products/atomic-habits.jpg'),
  (21, 1, 'The Psychology of Money', 'A collection of insights about money, behaviour and personal finance.', 399.00, 30, 'Books', 'images/products/psychology-of-money.jpg'),
  (22, 1, 'Deep Work', 'A guide to focused work and improving productivity in a distracted world.', 449.00, 25, 'Books', 'images/products/deep-work.jpg'),
  (23, 1, 'Ikigai', 'A book exploring purpose, balance and meaningful living.', 299.00, 30, 'Books', 'images/products/ikigai.jpg'),
  (24, 1, 'Rich Dad Poor Dad', 'A personal finance book discussing financial habits and money management.', 399.00, 30, 'Books', 'images/products/rich-dad.jpg');

-- CLOTHING (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (25, 1, 'Classic Cotton T-Shirt', 'Comfortable cotton T-shirt suitable for everyday casual wear.', 499.00, 40, 'Clothing', 'images/products/tshirt.jpg'),
  (26, 1, 'Casual Polo Shirt', 'Comfortable polo shirt suitable for casual and semi-casual occasions.', 799.00, 30, 'Clothing', 'images/products/polo.jpg'),
  (27, 1, 'Denim Jeans', 'Classic denim jeans designed for everyday casual wear.', 1499.00, 25, 'Clothing', 'images/products/jeans.jpg'),
  (28, 1, 'Cotton Hoodie', 'Comfortable cotton hoodie suitable for casual and cool-weather use.', 1099.00, 25, 'Clothing', 'images/products/hoodie.jpg'),
  (29, 1, 'Formal Shirt', 'Classic formal shirt suitable for college presentations, office and formal occasions.', 899.00, 30, 'Clothing', 'images/products/formalshirt.jpg'),
  (30, 1, 'Casual Shorts', 'Lightweight casual shorts suitable for everyday wear.', 699.00, 35, 'Clothing', 'images/products/shorts.jpg'),
  (31, 1, 'Track Pants', 'Comfortable track pants suitable for sports, exercise and casual use.', 799.00, 30, 'Clothing', 'images/products/trackpants.jpg'),
  (32, 1, 'Denim Jacket', 'Classic denim jacket suitable for casual outfits.', 1799.00, 20, 'Clothing', 'images/products/denimjacket.jpg');

-- HOME (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (33, 1, 'Table Lamp', 'Compact table lamp suitable for study tables, desks and bedrooms.', 899.00, 25, 'Home', 'images/products/tablelamp.jpg'),
  (34, 1, 'Ceramic Coffee Mug', 'Durable ceramic coffee mug suitable for everyday beverages.', 299.00, 40, 'Home', 'images/products/mug.jpg'),
  (35, 1, 'Water Bottle', 'Reusable water bottle suitable for home, college and travel.', 599.00, 35, 'Home', 'images/products/bottle.jpg'),
  (36, 1, 'Desk Organizer', 'Compact organizer for keeping stationery and desk items arranged.', 449.00, 30, 'Home', 'images/products/organizer.jpg'),
  (37, 1, 'Cushion Set', 'Comfortable decorative cushion set suitable for sofas and chairs.', 699.00, 25, 'Home', 'images/products/cushion.jpg'),
  (38, 1, 'Wall Clock', 'Simple wall clock suitable for bedrooms, offices and living spaces.', 799.00, 20, 'Home', 'images/products/wallclock.jpg'),
  (39, 1, 'Storage Box Set', 'Set of reusable storage boxes for organizing household items.', 899.00, 25, 'Home', 'images/products/storagebox.jpg'),
  (40, 1, 'Electric Kettle', 'Compact electric kettle suitable for quickly boiling water.', 1299.00, 20, 'Home', 'images/products/kettle.jpg');

ALTER TABLE users       ALTER COLUMN id RESTART WITH 100;
ALTER TABLE products    ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cart_items  ALTER COLUMN id RESTART WITH 100;
ALTER TABLE orders      ALTER COLUMN id RESTART WITH 100;
ALTER TABLE order_items ALTER COLUMN id RESTART WITH 100;
