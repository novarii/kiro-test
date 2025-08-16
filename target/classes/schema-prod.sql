-- Production Database Schema for PostgreSQL
-- This script creates the database schema optimized for PostgreSQL production environment
-- Execute this script manually in production or use a migration tool like Flyway

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'CREDIT', 'INVESTMENT')),
    initial_balance DECIMAL(19,2) DEFAULT 0.00,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    amount DECIMAL(19,2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    transaction_date DATE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transactions_updated_at BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

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
CREATE INDEX IF NOT EXISTS idx_accounts_user_deleted ON accounts(user_id, deleted) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_accounts_created_at ON accounts(created_at);

-- Transaction indexes
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_deleted ON transactions(deleted);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_transactions_account_deleted ON transactions(account_id, deleted) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transactions_date_deleted ON transactions(transaction_date, deleted) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transactions_account_date_deleted ON transactions(account_id, transaction_date, deleted) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transactions_category_date ON transactions(category_id, transaction_date) WHERE deleted = FALSE;

-- Partial indexes for better performance on filtered queries
CREATE INDEX IF NOT EXISTS idx_transactions_active ON transactions(id, account_id, transaction_date, amount) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_accounts_active ON accounts(id, user_id, name, type) WHERE deleted = FALSE;

-- Index for analytics queries
CREATE INDEX IF NOT EXISTS idx_transactions_analytics ON transactions(account_id, category_id, transaction_date, amount) WHERE deleted = FALSE;

-- Add constraints for data integrity
ALTER TABLE categories ADD CONSTRAINT chk_categories_default_unique 
    EXCLUDE (is_default WITH =) WHERE (is_default = TRUE);

-- Add check constraints
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_amount_not_zero 
    CHECK (amount != 0);

ALTER TABLE accounts ADD CONSTRAINT chk_accounts_name_not_empty 
    CHECK (LENGTH(TRIM(name)) > 0);

ALTER TABLE users ADD CONSTRAINT chk_users_email_format 
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Create materialized view for analytics (optional, for performance)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_monthly_transaction_summary AS
SELECT 
    DATE_TRUNC('month', t.transaction_date) as month,
    a.user_id,
    c.name as category_name,
    COUNT(*) as transaction_count,
    SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END) as total_income,
    SUM(CASE WHEN t.amount < 0 THEN ABS(t.amount) ELSE 0 END) as total_expenses,
    SUM(t.amount) as net_amount
FROM transactions t
JOIN accounts a ON t.account_id = a.id
JOIN categories c ON t.category_id = c.id
WHERE t.deleted = FALSE AND a.deleted = FALSE
GROUP BY DATE_TRUNC('month', t.transaction_date), a.user_id, c.name;

-- Create index on materialized view
CREATE INDEX IF NOT EXISTS idx_mv_monthly_summary_user_month 
    ON mv_monthly_transaction_summary(user_id, month);

-- Grant permissions (adjust as needed for your production setup)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO expense_tracker_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO expense_tracker_app;