-- Create the database
CREATE DATABASE IF NOT EXISTS manginasal;
USE manginasal;

-- Drop existing tables if you want a fresh setup
DROP TABLE IF EXISTS Payments;
DROP TABLE IF EXISTS OrderDetails;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Menu;
DROP TABLE IF EXISTS PaymentMethods;

-- Menu Table
CREATE TABLE Menu (
    MenuID INT PRIMARY KEY AUTO_INCREMENT,
    ItemName VARCHAR(100) NOT NULL,
    Price DECIMAL(10,2) NOT NULL
);

-- Orders Table
CREATE TABLE Orders (
    OrderID INT PRIMARY KEY AUTO_INCREMENT,
    TotalAmount DECIMAL(10,2) DEFAULT 0.00
);

-- OrderDetails Table
CREATE TABLE OrderDetails (
    OrderDetailID INT PRIMARY KEY AUTO_INCREMENT,
    OrderID INT NOT NULL,
    MenuID INT NOT NULL,
    Quantity INT NOT NULL,
    Subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID) ON DELETE CASCADE,
    FOREIGN KEY (MenuID) REFERENCES Menu(MenuID) ON DELETE RESTRICT
);

-- PaymentMethods Table
CREATE TABLE PaymentMethods (
    PaymentMethodID INT PRIMARY KEY AUTO_INCREMENT,
    MethodName VARCHAR(50) NOT NULL
);

-- Payments Table
CREATE TABLE Payments (
    PaymentID INT PRIMARY KEY AUTO_INCREMENT,
    OrderID INT NOT NULL,
    PaymentMethodID INT NOT NULL,
    AmountPaid DECIMAL(10,2) NOT NULL,
    ChangeAmount DECIMAL(10,2) DEFAULT 0.00,
    PaymentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID) ON DELETE CASCADE,
    FOREIGN KEY (PaymentMethodID) REFERENCES PaymentMethods(PaymentMethodID) ON DELETE RESTRICT
);

-- Insert sample menu items
INSERT INTO Menu (ItemName, Price) VALUES
('Paa Large', 139.00),
('Pecho Large', 149.00),
('Chicken Inasal Meal', 199.00),
('Leche Flan', 49.00),
('Sago\'t Gulaman', 39.00);

-- Insert payment methods
INSERT INTO PaymentMethods (MethodName) VALUES
('Cash'),
('GCash'),
('Credit/Debit Card');

-- Check data
SELECT * FROM Menu;
SELECT * FROM PaymentMethods;
