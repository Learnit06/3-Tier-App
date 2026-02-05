-- MySQL Database Setup for User Registration App
-- Run this script as MySQL root user

-- Create database
CREATE DATABASE IF NOT EXISTS userdb;

-- Use the database
USE userdb;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create a user for the application
-- Note: Change 'AppPass123!' to a secure password in production
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'AppPass123!';

-- Grant privileges
GRANT ALL PRIVILEGES ON userdb.* TO 'appuser'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify table creation
DESCRIBE users;

-- Show all tables
SHOW TABLES;

-- Sample: Insert test data (optional)
-- INSERT INTO users (name, email) VALUES ('Test User', 'test@example.com');

-- Query to view all users
SELECT * FROM users;
