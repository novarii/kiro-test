## Architecture Principles

RESTful API Design: Follow REST conventions for all endpoints
Layered Architecture: Controller → Service → Repository pattern
Dependency Injection: Use Spring's IoC container throughout
Security First: All endpoints except auth require authentication
Data Validation: Validate all inputs at controller level
Error Handling: Consistent error responses across all endpoints

## Database Design Principles

Use proper JPA relationships (@OneToMany, @ManyToOne)
Soft delete for financial records (never hard delete transactions)
Created/Updated timestamps on all entities
Proper indexing for query performance

## **Project Structure Steering File (`structure.md`)**

# Project Structure & Conventions

## Package Structure


src/main/java/com/fintech/expensetracker/
├── ExpenseTrackerApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── DatabaseConfig.java
├── controller/
│   ├── AuthController.java
│   ├── AccountController.java
│   ├── TransactionController.java
│   └── AnalyticsController.java
├── service/
│   ├── UserService.java
│   ├── AccountService.java
│   ├── TransactionService.java
│   └── AnalyticsService.java
├── repository/
│   ├── UserRepository.java
│   ├── AccountRepository.java
│   ├── TransactionRepository.java
│   └── CategoryRepository.java
├── entity/
│   ├── User.java
│   ├── Account.java
│   ├── Transaction.java
│   └── Category.java
├── dto/
│   ├── request/
│   └── response/
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── UserPrincipal.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BadRequestException.java
└── util/
├── DateUtil.java
└── ValidationUtil.java

## Naming Conventions
- **Classes:** PascalCase (UserService, AccountController)
- **Methods:** camelCase (calculateMonthlySpending, getUserById)
- **Variables:** camelCase (monthlyTotal, userAccount)
- **Constants:** UPPER_SNAKE_CASE (MAX_TRANSACTION_AMOUNT)
- **Database Tables:** snake_case (users, account_transactions)
- **API Endpoints:** kebab-case (/spending-summary, /account-balance)

## Code Style Guidelines
- Use @RestController for REST endpoints
- Use @Service for business logic
- Use @Repository for data access (though not required with Spring Data JPA)
- Always use @Transactional for service methods that modify data
- Use ResponseEntity<> for all controller responses
- Use @Valid for request validation
- Use @JsonProperty for DTO field mapping

## Error Handling Patterns
- Use @ControllerAdvice for global exception handling
- Return consistent error response format:
```json
{
    "timestamp": "2025-01-20T10:15:30",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "path": "/api/transactions"
}
```

## Testing Conventions

Test classes end with "Test" (UserServiceTest)
Integration tests end with "IntegrationTest"
Use @SpringBootTest for integration tests
Use @WebMvcTest for controller tests
Use @DataJpaTest for repository tests
Mock external dependencies with @MockBean

# API Design Standards

## Base URL Structure
- Base URL: `/api/v1`
- Authentication: `/api/v1/auth`
- Resources: `/api/v1/{resource}`

## Standard HTTP Methods
- GET: Retrieve data (list or single item)
- POST: Create new resources
- PUT: Full update of existing resource
- PATCH: Partial update of existing resource
- DELETE: Remove resource (soft delete for transactions)

## Response Format Standards
```json
// Success Response
{
    "data": {...},
    "message": "Success",
    "timestamp": "2025-01-20T10:15:30"
}
```
```json
// Error Response
{
    "error": {
        "code": "VALIDATION_ERROR",
        "message": "Invalid input data",
        "details": ["Amount must be positive"]
    },
    "timestamp": "2025-01-20T10:15:30"
}
```

## Authentication Requirements

All endpoints except /auth/* require JWT token
Include token in Authorization header: Bearer {token}
Token expires in 24 hours
Refresh token mechanism for extended sessions

