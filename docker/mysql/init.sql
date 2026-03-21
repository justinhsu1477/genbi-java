-- ============================================================
-- NLQ Demo 假資料 - 模擬電商場景
-- NLQ Demo sample data — simulates an e-commerce scenario
-- ============================================================

CREATE DATABASE IF NOT EXISTS nlq_demo;
USE nlq_demo;

-- 產品表 Products table
CREATE TABLE IF NOT EXISTS products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 訂單表 Orders table
CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 插入產品 Insert products
-- ============================================================
INSERT INTO products (product_name, category, price) VALUES
    ('MacBook Pro 14"', 'Electronics', 1999.00),
    ('iPhone 15 Pro', 'Electronics', 1199.00),
    ('AirPods Pro', 'Electronics', 249.00),
    ('iPad Air', 'Electronics', 599.00),
    ('Nike Air Max', 'Shoes', 150.00),
    ('Adidas Ultraboost', 'Shoes', 180.00),
    ('Levi''s 501 Jeans', 'Clothing', 69.00),
    ('Columbia Jacket', 'Clothing', 120.00),
    ('Sony WH-1000XM5', 'Electronics', 349.00),
    ('Samsung Galaxy S24', 'Electronics', 899.00);

-- ============================================================
-- 插入訂單 (30 筆，跨 3 個月)
-- Insert orders (30 rows, spanning 3 months)
-- ============================================================
INSERT INTO orders (product_id, amount, quantity, customer_name, created_at) VALUES
    -- 2025-01 January
    (1, 1999.00, 1, 'Alice Chen', '2025-01-05 10:30:00'),
    (2, 2398.00, 2, 'Bob Wang', '2025-01-08 14:20:00'),
    (3, 249.00, 1, 'Charlie Li', '2025-01-10 09:15:00'),
    (5, 300.00, 2, 'Diana Wu', '2025-01-12 16:45:00'),
    (7, 138.00, 2, 'Eve Zhang', '2025-01-15 11:00:00'),
    (2, 1199.00, 1, 'Frank Liu', '2025-01-18 13:30:00'),
    (4, 599.00, 1, 'Grace Huang', '2025-01-20 10:00:00'),
    (9, 349.00, 1, 'Henry Xu', '2025-01-22 15:20:00'),
    (10, 1798.00, 2, 'Ivy Sun', '2025-01-25 09:45:00'),
    (8, 120.00, 1, 'Jack Ma', '2025-01-28 14:10:00'),

    -- 2025-02 February
    (1, 3998.00, 2, 'Alice Chen', '2025-02-02 10:30:00'),
    (3, 498.00, 2, 'Kevin Lin', '2025-02-05 11:20:00'),
    (6, 360.00, 2, 'Laura Yang', '2025-02-08 14:00:00'),
    (2, 1199.00, 1, 'Mike Zhao', '2025-02-10 09:30:00'),
    (5, 150.00, 1, 'Nancy Deng', '2025-02-12 16:15:00'),
    (4, 1198.00, 2, 'Oscar Feng', '2025-02-15 13:45:00'),
    (10, 899.00, 1, 'Bob Wang', '2025-02-18 10:20:00'),
    (7, 69.00, 1, 'Patricia He', '2025-02-20 15:00:00'),
    (9, 698.00, 2, 'Quinn Luo', '2025-02-22 09:10:00'),
    (8, 240.00, 2, 'Rachel Cao', '2025-02-25 14:30:00'),

    -- 2025-03 March
    (1, 1999.00, 1, 'Sam Guo', '2025-03-01 10:00:00'),
    (2, 3597.00, 3, 'Alice Chen', '2025-03-03 11:30:00'),
    (3, 747.00, 3, 'Tom Zhu', '2025-03-06 14:20:00'),
    (6, 180.00, 1, 'Uma Xie', '2025-03-08 09:45:00'),
    (5, 450.00, 3, 'Victor Ren', '2025-03-10 16:00:00'),
    (4, 599.00, 1, 'Wendy Shi', '2025-03-12 13:15:00'),
    (10, 1798.00, 2, 'Xavier Pan', '2025-03-15 10:30:00'),
    (7, 207.00, 3, 'Yolanda Su', '2025-03-18 15:45:00'),
    (9, 349.00, 1, 'Zack Hu', '2025-03-20 09:00:00'),
    (8, 360.00, 3, 'Alice Chen', '2025-03-22 14:00:00');
