package com.fintech.expensetracker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.expensetracker.dto.request.*;
import com.fintech.expensetracker.dto.response.*;
import com.fintech.expensetracker.entity.AccountType;
import com.fintech.expensetracker.dto.TransactionType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpenseTrackerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("expense_tracker_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String secondUserToken;
    private Long userId;
    private Long secondUserId;
    private Long accountId;
    private Long secondAccountId;
    private Long transactionId;

    @Test
    @Order(1)
    void shouldRegisterNewUser() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = response.getToken();
        userId = response.getUser().getId();

        assertThat(userToken).isNotNull();
        assertThat(userId).isNotNull();
    }

    @Test
    @Order(2)
    void shouldRegisterSecondUserForIsolationTesting() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("second@example.com");
        request.setPassword("password123");
        request.setName("Second User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        secondUserToken = response.getToken();
        secondUserId = response.getUser().getId();
    }

    @Test
    @Order(3)
    void shouldNotRegisterUserWithDuplicateEmail() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Duplicate User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @Order(4)
    void shouldAuthenticateUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(response.getToken()).isNotNull();
    }

    @Test
    @Order(5)
    void shouldNotAuthenticateWithInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @Order(6)
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/analytics/monthly-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void shouldCreateAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Test Checking Account");
        request.setType(AccountType.CHECKING);
        request.setInitialBalance(new BigDecimal("1000.00"));

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Checking Account"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.currentBalance").value(1000.00))
                .andReturn();

        AccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccountResponse.class);
        accountId = response.getId();
    }

    @Test
    @Order(8)
    void shouldCreateSecondUserAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Second User Account");
        request.setType(AccountType.SAVINGS);
        request.setInitialBalance(new BigDecimal("2000.00"));

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccountResponse.class);
        secondAccountId = response.getId();
    }

    @Test
    @Order(9)
    void shouldListUserAccountsWithIsolation() throws Exception {
        // First user should only see their account
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Checking Account"));

        // Second user should only see their account
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Second User Account"));
    }

    @Test
    @Order(10)
    void shouldUpdateAccount() throws Exception {
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Updated Checking Account");

        mockMvc.perform(put("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Checking Account"));
    }

    @Test
    @Order(11)
    void shouldNotAllowCrossUserAccountAccess() throws Exception {
        // Second user should not be able to update first user's account
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Hacked Account");

        mockMvc.perform(put("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(12)
    void shouldCreateIncomeTransaction() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(new BigDecimal("2500.00"));
        request.setDescription("Salary");
        request.setTransactionDate(LocalDate.now());
        request.setAccountId(accountId);
        request.setType(TransactionType.INCOME);

        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(2500.00))
                .andExpect(jsonPath("$.description").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andReturn();

        TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TransactionResponse.class);
        transactionId = response.getId();
    }

    @Test
    @Order(13)
    void shouldCreateExpenseTransactions() throws Exception {
        // Create multiple expense transactions for analytics testing
        String[] descriptions = {"Groceries", "Gas", "Restaurant", "Utilities"};
        BigDecimal[] amounts = {
                new BigDecimal("150.00"),
                new BigDecimal("60.00"),
                new BigDecimal("45.00"),
                new BigDecimal("120.00")
        };

        for (int i = 0; i < descriptions.length; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest();
            request.setAmount(amounts[i]);
            request.setDescription(descriptions[i]);
            request.setTransactionDate(LocalDate.now().minusDays(i));
            request.setAccountId(accountId);
            request.setType(TransactionType.EXPENSE);

            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("EXPENSE"));
        }
    }

    @Test
    @Order(14)
    void shouldListTransactionsWithFiltering() throws Exception {
        // List all transactions
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5)); // 1 income + 4 expenses

        // Filter by account
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));

        // Filter by type
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @Order(15)
    void shouldUpdateTransaction() throws Exception {
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setDescription("Updated Salary");
        request.setAmount(new BigDecimal("2600.00"));

        mockMvc.perform(put("/api/v1/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Salary"))
                .andExpect(jsonPath("$.amount").value(2600.00));
    }

    @Test
    @Order(16)
    void shouldCalculateAccountBalance() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3225.00)); // 1000 + 2600 - 375 (expenses)
    }

    @Test
    @Order(17)
    void shouldGetMonthlySpendingSummary() throws Exception {
        YearMonth currentMonth = YearMonth.now();
        
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("year", String.valueOf(currentMonth.getYear()))
                        .param("month", String.valueOf(currentMonth.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(2600.00))
                .andExpect(jsonPath("$.totalExpenses").value(375.00))
                .andExpect(jsonPath("$.netSavings").value(2225.00))
                .andExpect(jsonPath("$.year").value(currentMonth.getYear()))
                .andExpect(jsonPath("$.month").value(currentMonth.getMonthValue()));
    }

    @Test
    @Order(18)
    void shouldGetSpendingByCategory() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/v1/analytics/spending-by-category")
                        .header("Authorization", "Bearer " + userToken)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1)) // All expenses are uncategorized
                .andExpect(jsonPath("$[0].categoryName").value("Uncategorized"))
                .andExpect(jsonPath("$[0].totalAmount").value(375.00));
    }

    @Test
    @Order(19)
    void shouldCalculateSavingsRate() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/v1/analytics/savings-rate")
                        .header("Authorization", "Bearer " + userToken)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savingsRate").value(85.58)) // (2600-375)/2600 * 100
                .andExpect(jsonPath("$.totalIncome").value(2600.00))
                .andExpect(jsonPath("$.totalExpenses").value(375.00))
                .andExpect(jsonPath("$.netSavings").value(2225.00));
    }

    @Test
    @Order(20)
    void shouldEnsureDataIsolationInAnalytics() throws Exception {
        // Create transaction for second user
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setDescription("Second User Income");
        request.setTransactionDate(LocalDate.now());
        request.setAccountId(secondAccountId);
        request.setType(TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // First user's analytics should not include second user's data
        YearMonth currentMonth = YearMonth.now();
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("year", String.valueOf(currentMonth.getYear()))
                        .param("month", String.valueOf(currentMonth.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(2600.00)) // Should not include second user's 500
                .andExpect(jsonPath("$.totalExpenses").value(375.00));

        // Second user should only see their data
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .param("year", String.valueOf(currentMonth.getYear()))
                        .param("month", String.valueOf(currentMonth.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(500.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00));
    }

    @Test
    @Order(21)
    void shouldSoftDeleteTransaction() throws Exception {
        mockMvc.perform(delete("/api/v1/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Transaction should not appear in listings
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4)); // Should be 4 now (expenses only)
    }

    @Test
    @Order(22)
    void shouldHandleValidationErrors() throws Exception {
        // Test invalid account creation
        CreateAccountRequest invalidAccount = new CreateAccountRequest();
        invalidAccount.setName(""); // Invalid empty name
        invalidAccount.setType(AccountType.CHECKING);

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Test invalid transaction creation
        CreateTransactionRequest invalidTransaction = new CreateTransactionRequest();
        invalidTransaction.setAmount(new BigDecimal("-100.00")); // Invalid negative amount for expense
        invalidTransaction.setDescription("");
        invalidTransaction.setAccountId(accountId);
        invalidTransaction.setType(TransactionType.EXPENSE);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(23)
    void shouldHandleResourceNotFound() throws Exception {
        // Try to access non-existent account
        mockMvc.perform(get("/api/v1/accounts/99999/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());

        // Try to update non-existent transaction
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setDescription("Non-existent");

        mockMvc.perform(put("/api/v1/transactions/99999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(24)
    void shouldHandleAnalyticsWithNoData() throws Exception {
        // Test analytics for a future month with no data
        YearMonth futureMonth = YearMonth.now().plusMonths(6);
        
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("year", String.valueOf(futureMonth.getYear()))
                        .param("month", String.valueOf(futureMonth.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.netSavings").value(0.00));
    }

    @Test
    @Order(25)
    void shouldMaintainFinancialPrecision() throws Exception {
        // Create transaction with precise decimal values
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(new BigDecimal("123.456")); // Will be rounded to 2 decimal places
        request.setDescription("Precision Test");
        request.setTransactionDate(LocalDate.now());
        request.setAccountId(accountId);
        request.setType(TransactionType.EXPENSE);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(123.46)); // Should be properly rounded
    }
}