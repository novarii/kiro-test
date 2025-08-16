-- Database Schema Initialization Script
-- This script creates the database schema for the Expense Tracker API
-- It will be executed automatically by Spring Boot on application startup

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'CREDIT', 'INVESTMENT')),
    initial_balance DECIMAL(19,2) DEFAULT 0.00,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(19,2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    transaction_date DATE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- Create indexes for performance optimization
-- User indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- Category indexes
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_is_default ON categories(is_default);

-- Account indexes
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(type);
CREATE INDEX IF NOT EXISTS idx_accounts_deleted ON accounts(deleted);
CREATE INDEX IF NOT EXISTS idx_accounts_user_deleted ON accounts(user_id, deleted);

-- Transaction indexes
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_deleted ON transactions(deleted);
CREATE INDEX IF NOT EXISTS idx_transactions_account_deleted ON transactions(account_id, deleted);
CREATE INDEX IF NOT EXISTS idx_transactions_date_deleted ON transactions(transaction_date, deleted);
CREATE INDEX IF NOT EXISTS idx_transactions_account_date_deleted ON transactions(account_id, transaction_date, deleted);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions(account_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_category_date ON transactions(category_id, transaction_date);