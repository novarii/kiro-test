-- Default Data Initialization Script
-- This script populates the database with default categories and essential data
-- It will be executed automatically by Spring Boot after schema creation

-- Insert default categories
-- The "Uncategorized" category is marked as default and will be used when no category is specified
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Uncategorized', 'Default category for transactions without a specific category', TRUE);

-- Insert common expense categories
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Food & Dining', 'Restaurants, groceries, and food-related expenses', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Transportation', 'Gas, public transport, car maintenance, and travel expenses', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Shopping', 'Clothing, electronics, and general retail purchases', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Entertainment', 'Movies, games, subscriptions, and recreational activities', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Bills & Utilities', 'Electricity, water, internet, phone, and other utility bills', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Healthcare', 'Medical expenses, pharmacy, insurance, and health-related costs', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Education', 'Tuition, books, courses, and educational expenses', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Home & Garden', 'Home improvement, furniture, gardening, and household items', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Personal Care', 'Haircuts, cosmetics, gym memberships, and personal services', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Insurance', 'Life, health, auto, and other insurance premiums', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Taxes', 'Income tax, property tax, and other tax payments', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Investments', 'Stock purchases, retirement contributions, and investment fees', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Gifts & Donations', 'Charitable donations, gifts, and contributions', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Travel', 'Vacation expenses, hotels, flights, and travel-related costs', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Business', 'Business meals, office supplies, and work-related expenses', FALSE);

-- Insert common income categories
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Salary', 'Regular employment income and wages', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Freelance', 'Freelance work and contract income', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Investment Income', 'Dividends, interest, and capital gains', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Rental Income', 'Income from rental properties', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Business Income', 'Income from business operations', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Bonus', 'Work bonuses and performance incentives', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Refunds', 'Tax refunds, purchase returns, and reimbursements', FALSE);
MERGE INTO categories (name, description, is_default) KEY(name) VALUES 
    ('Other Income', 'Miscellaneous income sources', FALSE);

-- Note: User accounts and transactions are not pre-populated as they are user-specific
-- and will be created through the API endpoints after user registration