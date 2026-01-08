-- =====================================================
-- 交易系统测试数据初始化脚本
-- 包含：用户、商家、商品、库存、购物车、订单、结算等
-- =====================================================

-- =====================================================
-- 1. 用户数据 (Users)
-- =====================================================
INSERT INTO users (id, username, password, balance, created_at, version) VALUES
(1, 'user_alice', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 10000.00, '2024-01-01 10:00:00', 0),
(2, 'user_bob', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 5000.00, '2024-01-02 11:00:00', 0),
(3, 'user_charlie', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 2500.00, '2024-01-03 12:00:00', 0),
(4, 'user_diana', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 8000.00, '2024-01-04 13:00:00', 0),
(5, 'user_eve', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 0.00, '2024-01-05 14:00:00', 0);

-- =====================================================
-- 2. 商家数据 (Merchants)
-- =====================================================
-- 超级管理员
INSERT INTO merchants (id, business_name, username, password, role, balance, created_at, version) VALUES
(0, '系统管理员', 'root', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 'ADMIN', 0.00, '2024-01-01 00:00:00', 0);

-- 普通商家
INSERT INTO merchants (id, business_name, username, password, role, balance, created_at, version) VALUES
(1, '数码旗舰店', 'merchant_digital', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 'MERCHANT', 50000.00, '2024-01-01 09:00:00', 0),
(2, '时尚服饰店', 'merchant_fashion', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 'MERCHANT', 30000.00, '2024-01-02 09:00:00', 0),
(3, '美食天地', 'merchant_food', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 'MERCHANT', 15000.00, '2024-01-03 09:00:00', 0),
(4, '家居生活馆', 'merchant_home', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 'MERCHANT', 20000.00, '2024-01-04 09:00:00', 0),
(5, '运动健身店', 'merchant_sports', '$2a$10$TuwdBuf.UD6jz9pnCn/vtOW/SbV4OwJsropxNp.3d5DI4IB9EqfEe', 'MERCHANT', 0.00, '2024-01-05 09:00:00', 0);

-- =====================================================
-- 3. 商品数据 (Products)
-- =====================================================
-- 数码旗舰店商品
INSERT INTO products (id, name, description, category, merchant_id, created_at) VALUES
(1, 'iPhone 15 Pro', '苹果最新旗舰手机，A17 Pro芯片，钛金属边框', '手机', 1, '2024-01-10 10:00:00'),
(2, 'MacBook Pro 14', 'M3 Pro芯片，14英寸Liquid Retina XDR显示屏', '电脑', 1, '2024-01-10 10:00:00'),
(3, 'AirPods Pro 2', '主动降噪，自适应音频，MagSafe充电盒', '耳机', 1, '2024-01-10 10:00:00'),
(4, 'iPad Air', 'M1芯片，10.9英寸显示屏，支持Apple Pencil', '平板', 1, '2024-01-10 10:00:00');

-- 时尚服饰店商品
INSERT INTO products (id, name, description, category, merchant_id, created_at) VALUES
(5, '男士休闲夹克', '春秋款，纯棉面料，修身版型', '男装', 2, '2024-01-11 10:00:00'),
(6, '女士连衣裙', '夏季新款，雪纺面料，碎花图案', '女装', 2, '2024-01-11 10:00:00'),
(7, '运动鞋', '透气网面，减震鞋底，多色可选', '鞋类', 2, '2024-01-11 10:00:00'),
(8, '真皮手提包', '头层牛皮，大容量，商务休闲两用', '箱包', 2, '2024-01-11 10:00:00');

-- 美食天地商品
INSERT INTO products (id, name, description, category, merchant_id, created_at) VALUES
(9, '有机大米', '东北五常大米，5kg装，新米上市', '粮油', 3, '2024-01-12 10:00:00'),
(10, '进口牛排', '澳洲和牛M5级，200g/块，原切雪花', '肉类', 3, '2024-01-12 10:00:00'),
(11, '精选红酒', '法国波尔多产区，2019年份，干红', '酒水', 3, '2024-01-12 10:00:00'),
(12, '坚果礼盒', '混合坚果，500g，无添加', '零食', 3, '2024-01-12 10:00:00');

-- 家居生活馆商品
INSERT INTO products (id, name, description, category, merchant_id, created_at) VALUES
(13, '智能台灯', 'LED护眼，无极调光，支持语音控制', '灯具', 4, '2024-01-13 10:00:00'),
(14, '记忆棉枕头', '慢回弹，人体工学设计，透气面料', '床品', 4, '2024-01-13 10:00:00'),
(15, '空气净化器', 'HEPA滤网，PM2.5实时显示，静音模式', '电器', 4, '2024-01-13 10:00:00'),
(16, '咖啡机', '全自动研磨，一键萃取，奶泡功能', '厨电', 4, '2024-01-13 10:00:00');

-- 运动健身店商品
INSERT INTO products (id, name, description, category, merchant_id, created_at) VALUES
(17, '瑜伽垫', 'TPE材质，6mm厚度，防滑纹理', '健身器材', 5, '2024-01-14 10:00:00'),
(18, '哑铃套装', '可调节重量，2-20kg，包胶防滑', '健身器材', 5, '2024-01-14 10:00:00'),
(19, '跑步机', '家用静音，折叠收纳，心率监测', '健身器材', 5, '2024-01-14 10:00:00'),
(20, '蛋白粉', '乳清蛋白，2.27kg，巧克力味', '营养补剂', 5, '2024-01-14 10:00:00');

-- =====================================================
-- 4. 库存数据 (Inventory)
-- =====================================================
-- 数码旗舰店库存
INSERT INTO inventory (id, sku, product_id, merchant_id, quantity, price, version) VALUES
(1, 'IPHONE15PRO-256-BLK', 1, 1, 100, 8999.00, 0),
(2, 'IPHONE15PRO-512-SLV', 1, 1, 50, 10999.00, 0),
(3, 'MACBOOK14-M3PRO-512', 2, 1, 30, 16999.00, 0),
(4, 'AIRPODS-PRO2', 3, 1, 200, 1899.00, 0),
(5, 'IPAD-AIR-256-GRAY', 4, 1, 80, 5499.00, 0);

-- 时尚服饰店库存
INSERT INTO inventory (id, sku, product_id, merchant_id, quantity, price, version) VALUES
(6, 'JACKET-M-BLK', 5, 2, 50, 399.00, 0),
(7, 'JACKET-L-BLU', 5, 2, 40, 399.00, 0),
(8, 'DRESS-S-FLR', 6, 2, 60, 299.00, 0),
(9, 'SHOES-42-WHT', 7, 2, 100, 599.00, 0),
(10, 'BAG-LEATHER-BRN', 8, 2, 25, 1299.00, 0);

-- 美食天地库存
INSERT INTO inventory (id, sku, product_id, merchant_id, quantity, price, version) VALUES
(11, 'RICE-5KG-ORGANIC', 9, 3, 500, 89.00, 0),
(12, 'STEAK-M5-200G', 10, 3, 100, 168.00, 0),
(13, 'WINE-BORDEAUX-2019', 11, 3, 50, 388.00, 0),
(14, 'NUTS-MIX-500G', 12, 3, 200, 128.00, 0);

-- 家居生活馆库存
INSERT INTO inventory (id, sku, product_id, merchant_id, quantity, price, version) VALUES
(15, 'LAMP-SMART-WHT', 13, 4, 150, 199.00, 0),
(16, 'PILLOW-MEMORY-STD', 14, 4, 200, 299.00, 0),
(17, 'PURIFIER-HEPA-L', 15, 4, 40, 1599.00, 0),
(18, 'COFFEE-AUTO-BLK', 16, 4, 30, 2999.00, 0);

-- 运动健身店库存
INSERT INTO inventory (id, sku, product_id, merchant_id, quantity, price, version) VALUES
(19, 'YOGA-MAT-6MM-PUR', 17, 5, 300, 99.00, 0),
(20, 'DUMBBELL-SET-ADJ', 18, 5, 50, 699.00, 0),
(21, 'TREADMILL-HOME', 19, 5, 10, 3999.00, 0),
(22, 'PROTEIN-WHEY-CHOC', 20, 5, 100, 399.00, 0);


-- =====================================================
-- 5. 购物车数据 (Cart Items)
-- =====================================================
-- Alice的购物车
INSERT INTO cart_items (id, user_id, sku, quantity, created_at) VALUES
(1, 1, 'IPHONE15PRO-256-BLK', 1, '2024-01-20 10:00:00'),
(2, 1, 'AIRPODS-PRO2', 1, '2024-01-20 10:05:00'),
(3, 1, 'LAMP-SMART-WHT', 2, '2024-01-20 10:10:00');

-- Bob的购物车
INSERT INTO cart_items (id, user_id, sku, quantity, created_at) VALUES
(4, 2, 'JACKET-M-BLK', 1, '2024-01-21 11:00:00'),
(5, 2, 'SHOES-42-WHT', 2, '2024-01-21 11:05:00');

-- Charlie的购物车
INSERT INTO cart_items (id, user_id, sku, quantity, created_at) VALUES
(6, 3, 'RICE-5KG-ORGANIC', 3, '2024-01-22 12:00:00'),
(7, 3, 'STEAK-M5-200G', 5, '2024-01-22 12:05:00'),
(8, 3, 'WINE-BORDEAUX-2019', 2, '2024-01-22 12:10:00');

-- Diana的购物车
INSERT INTO cart_items (id, user_id, sku, quantity, created_at) VALUES
(9, 4, 'YOGA-MAT-6MM-PUR', 1, '2024-01-23 13:00:00'),
(10, 4, 'PROTEIN-WHEY-CHOC', 2, '2024-01-23 13:05:00');

-- =====================================================
-- 6. 订单数据 (Orders)
-- =====================================================
-- 已完成订单
INSERT INTO orders (id, order_number, user_id, merchant_id, total_amount, status, created_at, updated_at) VALUES
(1, 'ORD-20240115-001', 1, 1, 10898.00, 'COMPLETED', '2024-01-15 10:00:00', '2024-01-18 15:00:00'),
(2, 'ORD-20240116-001', 2, 2, 998.00, 'COMPLETED', '2024-01-16 11:00:00', '2024-01-19 16:00:00'),
(3, 'ORD-20240117-001', 3, 3, 773.00, 'COMPLETED', '2024-01-17 12:00:00', '2024-01-20 17:00:00');

-- 已支付订单（待发货）
INSERT INTO orders (id, order_number, user_id, merchant_id, total_amount, status, created_at, updated_at) VALUES
(4, 'ORD-20240120-001', 1, 4, 2097.00, 'PAID', '2024-01-20 10:00:00', '2024-01-20 10:05:00'),
(5, 'ORD-20240121-001', 4, 5, 1197.00, 'PAID', '2024-01-21 11:00:00', '2024-01-21 11:05:00');

-- 已发货订单
INSERT INTO orders (id, order_number, user_id, merchant_id, total_amount, status, created_at, updated_at) VALUES
(6, 'ORD-20240118-001', 2, 1, 1899.00, 'SHIPPED', '2024-01-18 14:00:00', '2024-01-22 09:00:00'),
(7, 'ORD-20240119-001', 3, 4, 598.00, 'SHIPPED', '2024-01-19 15:00:00', '2024-01-23 10:00:00');

-- 待支付订单
INSERT INTO orders (id, order_number, user_id, merchant_id, total_amount, status, created_at, updated_at) VALUES
(8, 'ORD-20240125-001', 4, 1, 16999.00, 'PENDING', '2024-01-25 16:00:00', '2024-01-25 16:00:00'),
(9, 'ORD-20240125-002', 1, 3, 516.00, 'PENDING', '2024-01-25 17:00:00', '2024-01-25 17:00:00');

-- 已取消订单
INSERT INTO orders (id, order_number, user_id, merchant_id, total_amount, status, created_at, updated_at) VALUES
(10, 'ORD-20240110-001', 5, 2, 1299.00, 'CANCELLED', '2024-01-10 09:00:00', '2024-01-10 12:00:00');

-- 已退款订单
INSERT INTO orders (id, order_number, user_id, merchant_id, total_amount, status, created_at, updated_at) VALUES
(11, 'ORD-20240112-001', 2, 3, 388.00, 'REFUNDED', '2024-01-12 10:00:00', '2024-01-14 11:00:00');

-- =====================================================
-- 7. 订单明细数据 (Order Items)
-- =====================================================
-- 订单1明细 (Alice购买数码产品)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(1, 1, 'IPHONE15PRO-256-BLK', 'iPhone 15 Pro', 1, 8999.00, 8999.00),
(2, 1, 'AIRPODS-PRO2', 'AirPods Pro 2', 1, 1899.00, 1899.00);

-- 订单2明细 (Bob购买服饰)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(3, 2, 'JACKET-M-BLK', '男士休闲夹克', 1, 399.00, 399.00),
(4, 2, 'SHOES-42-WHT', '运动鞋', 1, 599.00, 599.00);

-- 订单3明细 (Charlie购买食品)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(5, 3, 'RICE-5KG-ORGANIC', '有机大米', 2, 89.00, 178.00),
(6, 3, 'STEAK-M5-200G', '进口牛排', 2, 168.00, 336.00),
(7, 3, 'NUTS-MIX-500G', '坚果礼盒', 2, 128.00, 256.00);

-- 订单4明细 (Alice购买家居)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(8, 4, 'LAMP-SMART-WHT', '智能台灯', 3, 199.00, 597.00),
(9, 4, 'PILLOW-MEMORY-STD', '记忆棉枕头', 5, 299.00, 1495.00);

-- 订单5明细 (Diana购买运动用品)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(10, 5, 'YOGA-MAT-6MM-PUR', '瑜伽垫', 1, 99.00, 99.00),
(11, 5, 'DUMBBELL-SET-ADJ', '哑铃套装', 1, 699.00, 699.00),
(12, 5, 'PROTEIN-WHEY-CHOC', '蛋白粉', 1, 399.00, 399.00);

-- 订单6明细 (Bob购买耳机)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(13, 6, 'AIRPODS-PRO2', 'AirPods Pro 2', 1, 1899.00, 1899.00);

-- 订单7明细 (Charlie购买家居)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(14, 7, 'PILLOW-MEMORY-STD', '记忆棉枕头', 2, 299.00, 598.00);

-- 订单8明细 (Diana购买电脑)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(15, 8, 'MACBOOK14-M3PRO-512', 'MacBook Pro 14', 1, 16999.00, 16999.00);

-- 订单9明细 (Alice购买食品)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(16, 9, 'STEAK-M5-200G', '进口牛排', 2, 168.00, 336.00),
(17, 9, 'NUTS-MIX-500G', '坚果礼盒', 1, 128.00, 128.00);

-- 订单10明细 (Eve取消的订单)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(18, 10, 'BAG-LEATHER-BRN', '真皮手提包', 1, 1299.00, 1299.00);

-- 订单11明细 (Bob退款的订单)
INSERT INTO order_items (id, order_id, sku, product_name, quantity, unit_price, subtotal) VALUES
(19, 11, 'WINE-BORDEAUX-2019', '精选红酒', 1, 388.00, 388.00);


-- =====================================================
-- 8. 结算数据 (Settlements)
-- =====================================================
INSERT INTO settlements (id, merchant_id, settlement_date, total_sales, total_refunds, net_amount, balance_change, discrepancy, status, created_at) VALUES
(1, 1, '2024-01-15', 10898.00, 0.00, 10898.00, 10898.00, 0.00, 'MATCHED', '2024-01-16 00:00:00'),
(2, 2, '2024-01-16', 998.00, 0.00, 998.00, 998.00, 0.00, 'MATCHED', '2024-01-17 00:00:00'),
(3, 3, '2024-01-17', 773.00, 388.00, 385.00, 385.00, 0.00, 'MATCHED', '2024-01-18 00:00:00'),
(4, 1, '2024-01-18', 1899.00, 0.00, 1899.00, 1899.00, 0.00, 'MATCHED', '2024-01-19 00:00:00'),
(5, 4, '2024-01-19', 598.00, 0.00, 598.00, 598.00, 0.00, 'MATCHED', '2024-01-20 00:00:00'),
(6, 4, '2024-01-21', 2097.00, 0.00, 2097.00, 2097.00, 0.00, 'MATCHED', '2024-01-22 00:00:00'),
(7, 5, '2024-01-22', 1197.00, 0.00, 1197.00, 1197.00, 0.00, 'MATCHED', '2024-01-23 00:00:00');

-- =====================================================
-- 9. 交易记录数据 (Transaction Records)
-- =====================================================
-- 用户充值记录
INSERT INTO transaction_records (id, transaction_id, account_type, account_id, type, amount, balance_before, balance_after, related_order_id, created_at) VALUES
(1, 'TXN-20240101-001', 'USER', 1, 'DEPOSIT', 10000.00, 0.00, 10000.00, NULL, '2024-01-01 10:00:00'),
(2, 'TXN-20240102-001', 'USER', 2, 'DEPOSIT', 5000.00, 0.00, 5000.00, NULL, '2024-01-02 11:00:00'),
(3, 'TXN-20240103-001', 'USER', 3, 'DEPOSIT', 3000.00, 0.00, 3000.00, NULL, '2024-01-03 12:00:00'),
(4, 'TXN-20240104-001', 'USER', 4, 'DEPOSIT', 10000.00, 0.00, 10000.00, NULL, '2024-01-04 13:00:00');

-- 用户支付记录
INSERT INTO transaction_records (id, transaction_id, account_type, account_id, type, amount, balance_before, balance_after, related_order_id, created_at) VALUES
(5, 'TXN-20240115-001', 'USER', 1, 'PURCHASE', 10898.00, 20898.00, 10000.00, 1, '2024-01-15 10:00:00'),
(6, 'TXN-20240116-001', 'USER', 2, 'PURCHASE', 998.00, 5998.00, 5000.00, 2, '2024-01-16 11:00:00'),
(7, 'TXN-20240117-001', 'USER', 3, 'PURCHASE', 773.00, 3273.00, 2500.00, 3, '2024-01-17 12:00:00'),
(8, 'TXN-20240120-001', 'USER', 1, 'PURCHASE', 2097.00, 12097.00, 10000.00, 4, '2024-01-20 10:00:00'),
(9, 'TXN-20240121-001', 'USER', 4, 'PURCHASE', 1197.00, 9197.00, 8000.00, 5, '2024-01-21 11:00:00');

-- 商家收款记录
INSERT INTO transaction_records (id, transaction_id, account_type, account_id, type, amount, balance_before, balance_after, related_order_id, created_at) VALUES
(10, 'TXN-20240115-002', 'MERCHANT', 1, 'SALE', 10898.00, 39102.00, 50000.00, 1, '2024-01-15 10:00:00'),
(11, 'TXN-20240116-002', 'MERCHANT', 2, 'SALE', 998.00, 29002.00, 30000.00, 2, '2024-01-16 11:00:00'),
(12, 'TXN-20240117-002', 'MERCHANT', 3, 'SALE', 773.00, 14227.00, 15000.00, 3, '2024-01-17 12:00:00'),
(13, 'TXN-20240120-002', 'MERCHANT', 4, 'SALE', 2097.00, 17903.00, 20000.00, 4, '2024-01-20 10:00:00'),
(14, 'TXN-20240121-002', 'MERCHANT', 5, 'SALE', 1197.00, 0.00, 1197.00, 5, '2024-01-21 11:00:00');

-- 退款记录
INSERT INTO transaction_records (id, transaction_id, account_type, account_id, type, amount, balance_before, balance_after, related_order_id, created_at) VALUES
(15, 'TXN-20240114-001', 'USER', 2, 'REFUND_IN', 388.00, 4612.00, 5000.00, 11, '2024-01-14 11:00:00'),
(16, 'TXN-20240114-002', 'MERCHANT', 3, 'REFUND_OUT', 388.00, 15388.00, 15000.00, 11, '2024-01-14 11:00:00');

-- =====================================================
-- 重置自增ID序列 (H2数据库)
-- =====================================================
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE merchants ALTER COLUMN id RESTART WITH 100;
ALTER TABLE products ALTER COLUMN id RESTART WITH 100;
ALTER TABLE inventory ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cart_items ALTER COLUMN id RESTART WITH 100;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE order_items ALTER COLUMN id RESTART WITH 100;
ALTER TABLE settlements ALTER COLUMN id RESTART WITH 100;
ALTER TABLE transaction_records ALTER COLUMN id RESTART WITH 100;
