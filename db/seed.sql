-- DhatchinaMart seed data
-- One admin + a curated set of demo accounts so the whole marketplace
-- (browse, cart, checkout, orders, seller fulfilment, ratings) can be
-- explored without registering. Passwords are real bcrypt hashes.
--
-- Demo accounts (see docs/DEMO_CREDENTIALS.md for full notes):
--   admin@dhatchinamart.com   / Admin@123    (9876500001) - Platform Admin
--   buyer@dhatchinamart.com   / Buyer@123    (9844090001) - Buyer, cart ready to checkout
--   student@dhatchinamart.com / Student@123  (9844090002) - Buyer with a delivered order
--   seller@dhatchinamart.com  / Seller@123   (9844090003) - Seller (Electronics)
--   campus@dhatchinamart.com  / Campus@123   (9844090004) - Seller (Books)
-- All four demo mobiles accept a mock/dev OTP when signing in with a phone.
--
-- The remaining 33 of the 40 catalog products stay owned by the platform
-- admin (id 1) so the admin catalog is always populated.

INSERT INTO users (id, name, email, mobile_number, password_hash, role) VALUES
  (1, 'Platform Admin', 'admin@dhatchinamart.com', '9876500001', '$2a$10$i3Xw2lMwEHYovzkspFaqOu/aFUkFU90ANtgkNCdz5z9QKWYHgIlRO', 'ADMIN');

INSERT INTO users (id, name, email, mobile_number, password_hash, role, created_at) VALUES
  (2, 'Demo Buyer',       'buyer@dhatchinamart.com',     '9844090001', '$2a$10$D46XcZlULkF49TTrhsgTIuM/5LRXZhreOltf1NYuky9pmm/pCQDem', 'BUYER',  '2026-08-01 09:00:00'),
  (3, 'Demo Student',     'student@dhatchinamart.com',   '9844090002', '$2a$10$oAUkm7s5tczbsM1fNcAfRuMgn/bCyYP3XkGmphGrKEQhlf7ZgM5YS', 'BUYER',  '2026-08-01 09:05:00'),
  (4, 'Demo Seller',      'seller@dhatchinamart.com',    '9844090003', '$2a$10$3LpK6oFgMhZnZaxgloXo0u8zdG.i.gmMug7Pn.TXIL/9t5W7h8fam', 'SELLER', '2026-08-02 10:00:00'),
  (5, 'Demo Campus Shop', 'campus@dhatchinamart.com',    '9844090004', '$2a$10$3dPaAtqIiCBtKh.Wm0.HjegiZyi83OZNFn9hZHopscNNChZSOn0mu', 'SELLER', '2026-08-02 10:10:00');

-- ==================================================
-- PRODUCT CATALOG (40 products, 5 categories x 8)
-- ==================================================

-- ACCESSORIES (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (1,  1, 'Kalamkari Canvas Tote Bag', 'A reusable canvas tote featuring a traditional Kalamkari-inspired print. Spacious enough for college, market runs and everyday carry.', 599.00, 24, 'Accessories', 'images/products/kalamkari-tote.jpg'),
  (2,  1, 'Coconut Shell Keychain Set', 'A set of lightweight handcrafted keychains made from polished coconut shell pieces, finished by hand.', 199.00, 35, 'Accessories', 'images/products/coconut-keychain.jpg'),
  (3,  1, 'Handcrafted Wooden Bookmark', 'A smooth, hand-engraved wooden bookmark that keeps your reading spot in style.', 149.00, 40, 'Accessories', 'images/products/wooden-bookmark.jpg'),
  (4,  1, 'Recycled Fabric Sling Bag', 'A durable sling bag made from upcycled fabric, great for carrying small everyday essentials hands-free.', 549.00, 18, 'Accessories', 'images/products/recycled-sling.jpg'),
  (5,  1, 'Palm Leaf Mini Wallet', 'A compact wallet handwoven from palm leaf strips with a secure fold-over closure.', 299.00, 28, 'Accessories', 'images/products/palm-leaf-wallet.jpg'),
  (6,  1, 'Bamboo ID Card Holder', 'A light bamboo card holder that keeps your ID lanyard tidy and easy to scan.', 249.00, 30, 'Accessories', 'images/products/bamboo-id-holder.jpg'),
  (7,  1, 'Handwoven Cotton Wrist Pouch', 'A small wrist pouch woven from leftover cotton yarns, perfect for cash, cards and lip balm.', 279.00, 22, 'Accessories', 'images/products/cotton-wrist-pouch.jpg'),
  (8,  1, 'Terracotta Bead Bracelet', 'A simple bracelet of natural terracotta beads with a hand-tied cord.', 229.00, 26, 'Accessories', 'images/products/terracotta-bracelet.jpg');

-- BOOKS (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (9,  1, 'The Minimalist Student Handbook', 'A refreshingly short guide to studying less and learning more, one idea per page.', 299.00, 20, 'Books', 'images/products/minimalist-student.jpg'),
  (10, 1, 'Algorithms Without Fear', 'A gentle introduction to algorithms that skips the maths-scary parts and focuses on intuition.', 399.00, 18, 'Books', 'images/products/algorithms-book.jpg'),
  (11, 1, 'Practical Linux for Beginners', 'A hands-on guide to daily Linux commands, file systems and shell basics.', 449.00, 16, 'Books', 'images/products/linux-beginners.jpg'),
  (12, 1, 'The Creative Coder''s Notebook', 'A filled-with-ideas notebook for programmers who sketch, doodle and design before they code.', 249.00, 32, 'Books', 'images/products/coders-notebook.jpg'),
  (13, 1, 'Build Your First Cloud Project', 'A beginner-friendly walkthrough of deploying your first app to the cloud end to end.', 499.00, 15, 'Books', 'images/products/cloud-project-book.jpg'),
  (14, 1, 'Everyday Data Structures', 'A simple, practical introduction to arrays, linked lists, stacks, queues, trees and graph algorithms.', 379.00, 21, 'Books', 'images/products/data-structures-book.jpg'),
  (15, 1, 'The Curious Inventor''s Journal', 'A guided journal for recording experiments and the small steps of every invention.', 279.00, 27, 'Books', 'images/products/inventor-journal.jpg'),
  (16, 1, 'Pocket Guide to Git & GitHub', 'A compact reference for the git commands every student uses daily.', 329.00, 24, 'Books', 'images/products/git-guide.jpg');

-- CLOTHING (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (17, 1, 'Handblock Printed Cotton Shirt', 'A breathable cotton shirt with traditional hand-block prints, made for everyday wear.', 899.00, 14, 'Clothing', 'images/products/handblock-shirt.jpg'),
  (18, 1, 'Kalamkari Casual Stole', 'A soft cotton stole with a fine Kalamkari print that goes with everything.', 649.00, 19, 'Clothing', 'images/products/kalamkari-stole.jpg'),
  (19, 1, 'Handwoven Cotton Scarf', 'A light handwoven scarf that adds a layer of colour to any outfit.', 499.00, 23, 'Clothing', 'images/products/cotton-scarf.jpg'),
  (20, 1, 'Indigo Print Overshirt', 'A relaxed cotton overshirt in deep indigo, sized for layering over tees.', 949.00, 12, 'Clothing', 'images/products/indigo-overshirt.jpg'),
  (21, 1, 'Handloom Cotton Kurta', 'A comfortable handloom kurta cut for everyday wear.', 799.00, 17, 'Clothing', 'images/products/handloom-kurta.jpg'),
  (22, 1, 'Organic Cotton Lounge Pants', 'Soft, breathable lounge pants in certified organic cotton.', 699.00, 20, 'Clothing', 'images/products/organic-lounge-pants.jpg'),
  (23, 1, 'Block Print Casual Top', 'A casual top with subtle block-printed details.', 749.00, 16, 'Clothing', 'images/products/block-print-top.jpg'),
  (24, 1, 'Cotton Everyday Overshirt', 'A classic cotton overshirt that works for the library, the lab or a weekend trip.', 849.00, 13, 'Clothing', 'images/products/cotton-overshirt.jpg');

-- ELECTRONICS (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (25, 1, 'Wooden Laptop Stand', 'A sturdy wooden stand that lifts your laptop to eye level for a comfortable desk setup.', 799.00, 18, 'Electronics', 'images/products/wooden-laptop-stand.jpg'),
  (26, 1, 'USB-C Multi-Port Hub', 'A compact USB-C hub with the ports you actually need for a student desk.', 899.00, 15, 'Electronics', 'images/products/usb-c-hub.jpg'),
  (27, 1, 'Rechargeable Study Light', 'A clip-on rechargeable light for late-night study without disturbing anyone.', 499.00, 25, 'Electronics', 'images/products/rechargeable-study-light.jpg'),
  (28, 1, 'Compact Desk LED Lamp', 'A slim LED lamp with three brightness levels for a tidy desk.', 649.00, 20, 'Electronics', 'images/products/desk-led-lamp.jpg'),
  (29, 1, 'Cable Management Travel Kit', 'A small organizer to keep chargers, cables and earbuds tangle-free.', 349.00, 30, 'Electronics', 'images/products/cable-travel-kit.jpg'),
  (30, 1, 'Foldable Phone Stand', 'A lightweight foldable stand that props your phone up beside your laptop.', 299.00, 32, 'Electronics', 'images/products/foldable-phone-stand.jpg'),
  (31, 1, 'USB Rechargeable Mini Fan', 'A little desk fan that runs off USB and keeps you cool during long study sessions.', 549.00, 22, 'Electronics', 'images/products/mini-usb-fan.jpg'),
  (32, 1, 'Smart Desk Cable Organizer', 'A simple organizer that keeps your desk cables neat and reachable.', 249.00, 35, 'Electronics', 'images/products/desk-cable-organizer.jpg');

-- HOME (8)
INSERT INTO products (id, seller_id, name, description, price, stock_qty, category, image_url) VALUES
  (33, 1, 'Terracotta Self-Watering Plant Pot', 'A terracotta pot that waters your plant gently as it needs.', 499.00, 20, 'Home', 'images/products/terracotta-self-watering-pot.jpg'),
  (34, 1, 'Coconut Shell Desk Organizer', 'A natural organizer made using coconut shell-inspired elements for small desk items.', 349.00, 25, 'Home', 'images/products/coconut-desk-organizer.jpg'),
  (35, 1, 'Handwoven Palm Leaf Storage Basket', 'A sturdy basket handwoven from dried palm leaves for neatly storing daily items.', 699.00, 14, 'Home', 'images/products/palm-leaf-basket.jpg'),
  (36, 1, 'Terracotta Aroma Lamp', 'A small terracotta lamp that makes your room feel warm and calm.', 299.00, 27, 'Home', 'images/products/terracotta-aroma-lamp.jpg'),
  (37, 1, 'Bamboo Cable Organizer', 'A bamboo organizer that wraps desk cables neatly out of the way.', 249.00, 31, 'Home', 'images/products/bamboo-cable-organizer.jpg'),
  (38, 1, 'Handmade Ceramic Moon Mug', 'A hand-glazed ceramic mug with a moon-inspired finish.', 399.00, 19, 'Home', 'images/products/moon-ceramic-mug.jpg'),
  (39, 1, 'Recycled Paper Desk Journal', 'A sturdy journal made from recycled paper for notes, lists or a diary.', 279.00, 29, 'Home', 'images/products/recycled-paper-journal.jpg'),
  (40, 1, 'Mini Indoor Plant Starter Kit', 'A small kit with a pot, seeds and soil to start your first indoor plant.', 449.00, 17, 'Home', 'images/products/indoor-plant-kit.jpg');

-- ==================================================
-- PRODUCT OWNERSHIP (demo sellers)
-- Six catalog products are handed to the two demo sellers so their
-- dashboards are not empty. Names, prices, stock, categories, images and
-- the 40-product catalog size are unchanged.
-- ==================================================

-- Demo Seller (Electronics) -> laptop stand, USB-C hub, phone stand, moon mug
UPDATE products SET seller_id = 4 WHERE id IN (25, 26, 30, 38);
-- Demo Campus Shop (Books) -> cloud book, git guide
UPDATE products SET seller_id = 5 WHERE id IN (13, 16);

-- ==================================================
-- DEMO BUYER CART
-- buyer@dhatchinamart.com (id 2) left a cart ready to checkout.
-- ==================================================

INSERT INTO cart_items (id, user_id, product_id, quantity) VALUES
  (9001, 2, 1, 1),
  (9002, 2, 25, 1),
  (9003, 2, 38, 2);

-- ==================================================
-- DEMO ORDER HISTORY
-- buyer@ has orders in every status (DELIVERED, SHIPPED, CONFIRMED, PENDING)
-- so both the buyer history page and the seller dashboards have content.
-- student@ has one delivered order used to seed a review.
-- ==================================================

INSERT INTO orders (id, buyer_id, status, total_amount, created_at) VALUES
  (1001, 2, 'DELIVERED', 1727.00, '2026-08-28 11:05:00'),
  (1002, 2, 'SHIPPED',    899.00, '2026-09-06 16:40:00'),
  (1003, 2, 'CONFIRMED', 1297.00, '2026-09-14 09:15:00'),
  (1004, 2, 'PENDING',    299.00, '2026-09-19 18:30:00'),
  (1005, 3, 'DELIVERED',  499.00, '2026-09-01 13:20:00');

INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES
  (2001, 1001, 1,  1, 599.00),
  (2002, 1001, 25, 1, 799.00),
  (2003, 1001, 16, 1, 329.00),
  (2004, 1002, 26, 1, 899.00),
  (2005, 1003, 38, 2, 399.00),
  (2006, 1003, 27, 1, 499.00),
  (2007, 1004, 30, 1, 299.00),
  (2008, 1005, 13, 1, 499.00);

-- ==================================================
-- DEMO REVIEWS (orders must be DELIVERED, buyer owns the order)
-- ==================================================

INSERT INTO reviews (id, order_id, product_id, user_id, rating, review_text, created_at) VALUES
  (1, 1001, 25, 2, 5, 'Solid wooden stand - my laptop sits at eye level and it looks great on the desk. Highly recommended!', '2026-09-02 09:30:00'),
  (2, 1001, 16, 2, 4, 'Concise and handy. Exactly the commands I reach for every day between classes.',                          '2026-09-02 09:32:00'),
  (3, 1005, 13, 3, 4, 'Finished the whole deployment walkthrough in one evening. Perfect first cloud project guide.',            '2026-09-05 18:10:00');

ALTER TABLE users       ALTER COLUMN id RESTART WITH 100;
ALTER TABLE products    ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cart_items  ALTER COLUMN id RESTART WITH 100;
ALTER TABLE orders      ALTER COLUMN id RESTART WITH 100;
ALTER TABLE order_items ALTER COLUMN id RESTART WITH 100;
ALTER TABLE reviews     ALTER COLUMN id RESTART WITH 100;
