# Database Initialization Documentation

## Overview

The Expense Tracker API includes comprehensive database initialization that sets up the schema, creates indexes for performance optimization, and populates default data including categories for transaction classification.

## Components

### 1. Schema Creation (`schema.sql`)
- Creates all necessary tables with proper constraints and relationships
- Adds performance-optimized indexes for common query patterns
- Includes foreign key constraints to maintain data integrity
- Compatible with H2 (development) and PostgreSQL (production)

### 2. Default Data Population (`data.sql`)
- Populates default categories including the required "Uncategorized" category
- Includes common expense and income categories with descriptions
- Uses MERGE statements to prevent duplicate entries

### 3. Database Configuration (`DatabaseConfig.java`)
- Provides fallback initialization if SQL scripts fail
- Ensures the default "Uncategorized" category always exists
- Creates essential categories with proper descriptions
- Validates database integrity on startup

### 4. Database Migration Utility (`DatabaseMigrationUtil.java`)
- Provides utilities for schema validation and maintenance
- Includes methods for checking table/column/index existence
- Offers database optimization and integrity validation
- Supports future migration scenarios

## Database Schema

### Tables Created
- `users` - User account information with encrypted passwords
- `categories` - Transaction categories with default category support
- `accounts` - User financial accounts (checking, savings, etc.)
- `transactions` - Financial transactions with categorization

### Key Indexes
- User email lookup: `idx_users_email`
- Account queries: `idx_accounts_user_id`, `idx_accounts_user_deleted`
- Transaction queries: `idx_transactions_account_date_deleted`, `idx_transactions_category_date`
- Analytics queries: `idx_transactions_analytics`

## Default Categories

### Expense Categories
- Food & Dining
- Transportation
- Shopping
- Entertainment
- Bills & Utilities
- Healthcare
- Education
- Home & Garden
- Personal Care
- Insurance
- Taxes
- Investments
- Gifts & Donations
- Travel
- Business

### Income Categories
- Salary
- Freelance
- Investment Income
- Rental Income
- Business Income
- Bonus
- Refunds
- Other Income

### Special Categories
- **Uncategorized** (default category, marked with `is_default = true`)

## Configuration

### Development (H2)
```properties
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=create-drop
```

### Production (PostgreSQL)
```properties
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema-prod.sql
spring.sql.init.data-locations=classpath:data.sql
spring.jpa.hibernate.ddl-auto=validate
```

## Performance Optimizations

### Indexes
- Composite indexes for common query patterns
- Partial indexes for active records (where deleted = false)
- Specialized indexes for analytics queries

### PostgreSQL Specific
- Materialized view for monthly transaction summaries
- Automatic timestamp updates with triggers
- Advanced constraints and exclusions

## Validation

The `DatabaseConfigTest` class validates:
- Default category existence and uniqueness
- Essential categories are created with descriptions
- Category names are unique
- Database initialization completes successfully

## Maintenance

Use `DatabaseMigrationUtil` for:
- Checking database integrity
- Optimizing database performance
- Validating schema consistency
- Future migration support

## Troubleshooting

### Common Issues
1. **SQL Script Failures**: DatabaseConfig provides fallback initialization
2. **Missing Categories**: Essential categories are created automatically
3. **Performance Issues**: Indexes are created automatically for optimization
4. **Data Integrity**: Foreign key constraints prevent orphaned records

### Validation Commands
```java
// Check database integrity
boolean isValid = databaseMigrationUtil.validateDatabaseIntegrity();

// Get database statistics
Map<String, Object> stats = databaseMigrationUtil.getDatabaseStatistics();

// Optimize database
databaseMigrationUtil.optimizeDatabase();
```