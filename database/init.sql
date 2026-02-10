-- 1. Create the Products Table
CREATE TABLE IF NOT EXISTS products ( 
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INTEGER NOT NULL
);

-- 2. Make Initial Data 
INSERT INTO products (name, price, stock) VALUES 
('The Count of Monte Christo', 15.99, 50),
('Da Bible', 12.50, 100),
('Book Three', 45.00, 20),
('The DevOps Handbook', 35.99, 30),
('Introduction to Java', 55.50, 15);

-- 3. Create Admin User 
CREATE USER sadmin WITH PASSWORD 'sadmin';
GRANT ALL PRIVILEGES ON DATABASE bookstore TO sadmin;
