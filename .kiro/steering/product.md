# Personal Expense Tracker API

## Product Purpose
A RESTful API backend for personal financial management that tracks expenses, income, and provides basic financial analytics. This serves as the foundation for a larger Personal Financial Wellness Platform.

## Target Users
- Individual users who want to track their personal finances
- Users with multiple bank accounts (checking, savings, credit cards)
- Users who need spending insights and basic financial analytics

## Core Business Value
- Centralized financial data management
- Automated spending categorization and analysis
- Foundation for financial health scoring and recommendations
- Secure personal financial data handling

## Key Business Rules
- All financial amounts stored as decimal values (no floating point)
- Transactions must be categorized for proper analytics
- User data must be completely isolated (no cross-user data access)
- All monetary calculations must be precise and auditable
- Account balances calculated from transaction history