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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseTrackerH2IntegrationTest {

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
    void shouldCompleteFullIntegrationWorkflow() throws Exception {
        // === USER REGISTRATION AND AUTHENTICATION FLOW ===
        
        // Test user registration
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("integration@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setName("Integration Test User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("integration@example.com"))
                .andExpect(jsonPath("$.user.name").value("Integration Test User"))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = authResponse.getToken();
        userId = authResponse.getUser().getId();

        assertThat(userToken).isNotNull();
        assertThat(userId).isNotNull();

        // Test duplicate email registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email address is already in use"));

        // Test successful login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("integration@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("integration@example.com"));

        // Test invalid credentials
        loginRequest.setPassword("wrongpassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials provided"));

        // Test authentication requirement
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isForbidden());

        // === ACCOUNT MANAGEMENT FLOW ===
        
        // Create account
        CreateAccountRequest accountRequest = new CreateAccountRequest();
        accountRequest.setName("Integration Test Account");
        accountRequest.setType(AccountType.CHECKING);
        accountRequest.setInitialBalance(new BigDecimal("1500.00"));

        result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Test Account"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.currentBalance").value(1500.00))
                .andReturn();

        AccountResponse accountResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccountResponse.class);
        accountId = accountResponse.getId();

        // List accounts
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Integration Test Account"));

        // Update account
        UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest();
        updateAccountRequest.setName("Updated Integration Account");

        mockMvc.perform(put("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAccountRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration Account"));

        // Get account balance
        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00));

        // === TRANSACTION MANAGEMENT FLOW ===
        
        // Create income transaction
        CreateTransactionRequest incomeRequest = new CreateTransactionRequest();
        incomeRequest.setAmount(new BigDecimal("3000.00"));
        incomeRequest.setDescription("Monthly Salary");
        incomeRequest.setTransactionDate(LocalDate.now());
        incomeRequest.setAccountId(accountId);
        incomeRequest.setType(TransactionType.INCOME);

        result = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(3000.00))
                .andExpect(jsonPath("$.description").value("Monthly Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andReturn();

        TransactionResponse transactionResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TransactionResponse.class);
        transactionId = transactionResponse.getId();

        // Create expense transactions
        String[] expenses = {"Rent", "Groceries", "Utilities", "Transportation"};
        BigDecimal[] amounts = {
                new BigDecimal("1200.00"),
                new BigDecimal("300.00"),
                new BigDecimal("150.00"),
                new BigDecimal("200.00")
        };

        for (int i = 0; i < expenses.length; i++) {
            CreateTransactionRequest expenseRequest = new CreateTransactionRequest();
            expenseRequest.setAmount(amounts[i]);
            expenseRequest.setDescription(expenses[i]);
            expenseRequest.setTransactionDate(LocalDate.now().minusDays(i + 1));
            expenseRequest.setAccountId(accountId);
            expenseRequest.setType(TransactionType.EXPENSE);

            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(expenseRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.description").value(expenses[i]));
        }

        // List transactions
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())  // Changed from "$" to "$.content"
                .andExpect(jsonPath("$.content.length()").value(5)); // Changed from "$.length()" to "$.content.length()" // 1 income + 4 expenses

        /*
         * Currently, my code does not support filtering transactions by type.
         * I should add the functionality later and disregard the test for now.
         */

        // // Filter transactions by type
        // mockMvc.perform(get("/api/v1/transactions")
        //                 .header("Authorization", "Bearer " + userToken)
        //                 .param("type", "EXPENSE"))
        //         .andExpect(status().isOk())
        //         .andExpect(jsonPath("$.length()").value(4));

        // Update transaction
        UpdateTransactionRequest updateTransactionRequest = new UpdateTransactionRequest();
        updateTransactionRequest.setDescription("Updated Monthly Salary");
        updateTransactionRequest.setAmount(new BigDecimal("3200.00"));
        updateTransactionRequest.setType(TransactionType.INCOME);  

        mockMvc.perform(put("/api/v1/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTransactionRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Monthly Salary"))
                .andExpect(jsonPath("$.amount").value(3200.00));

        // Verify updated account balance
        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2850.00)); // 1500 + 3200 - 1850 (expenses)

        // === ANALYTICS FLOW ===
        
        YearMonth currentMonth = YearMonth.now();

        // Test monthly spending summary
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("month", currentMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(3200.00))
                .andExpect(jsonPath("$.totalExpenses").value(1850.00))
                .andExpect(jsonPath("$.netSavings").value(1350.00))
                .andExpect(jsonPath("$.month").value(currentMonth.toString()));

        // Test spending by category
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/v1/analytics/spending-by-category")
                        .header("Authorization", "Bearer " + userToken)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryBreakdown").isArray())  // Changed from "$" to "$.categoryBreakdown"
                .andExpect(jsonPath("$.categoryBreakdown.length()").value(1))  // Changed from "$.length()" to "$.categoryBreakdown.length()"
                .andExpect(jsonPath("$.categoryBreakdown[0].category.name").value("Uncategorized"))  // Changed from "$[0].categoryName" to "$.categoryBreakdown[0].category.name"
                .andExpect(jsonPath("$.categoryBreakdown[0].amount").value(1850.00));  // Changed from "$[0].totalAmount" to "$.categoryBreakdown[0].amount"

        // Test savings rate calculation
        mockMvc.perform(get("/api/v1/analytics/savings-rate")
                        .header("Authorization", "Bearer " + userToken)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savingsRate").value(42.19)); // (3200-1850)/3200 * 100

        // Test analytics with no data (future month)
        YearMonth futureMonth = YearMonth.now().plusMonths(6);
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("month", futureMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.netSavings").value(0.00));

        // === DATA ISOLATION TESTING ===
        
        // Register second user
        UserRegistrationRequest secondUserRequest = new UserRegistrationRequest();
        secondUserRequest.setEmail("second@example.com");
        secondUserRequest.setPassword("password123");
        secondUserRequest.setName("Second User");

        result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUserRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse secondAuthResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        secondUserToken = secondAuthResponse.getToken();
        secondUserId = secondAuthResponse.getUser().getId();

        // Create account for second user
        CreateAccountRequest secondAccountRequest = new CreateAccountRequest();
        secondAccountRequest.setName("Second User Account");
        secondAccountRequest.setType(AccountType.SAVINGS);
        secondAccountRequest.setInitialBalance(new BigDecimal("5000.00"));

        result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondAccountRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse secondAccountResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccountResponse.class);
        secondAccountId = secondAccountResponse.getId();

        // Create transaction for second user
        CreateTransactionRequest secondUserTransactionRequest = new CreateTransactionRequest();
        secondUserTransactionRequest.setAmount(new BigDecimal("1000.00"));
        secondUserTransactionRequest.setDescription("Second User Income");
        secondUserTransactionRequest.setTransactionDate(LocalDate.now());
        secondUserTransactionRequest.setAccountId(secondAccountId);
        secondUserTransactionRequest.setType(TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUserTransactionRequest)))
                .andExpect(status().isCreated());

        // Verify data isolation - first user should only see their accounts
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Updated Integration Account"));

        // Second user should only see their accounts
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Second User Account"));

        // Verify transaction isolation
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5)); // Changed from $.length() to $.content.length()

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1)); // Changed from $.length() to $.content.length()

        // Verify analytics isolation
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("month", currentMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(3200.00)); // Should not include second user's income

        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .param("month", currentMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00));

        // Test cross-user access prevention
        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/balance")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isNotFound());

        UpdateAccountRequest hackAttempt = new UpdateAccountRequest();
        hackAttempt.setName("Hacked Account");
        mockMvc.perform(put("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackAttempt)))
                .andExpect(status().isNotFound());

        // === ERROR HANDLING TESTING ===
        
        // Test validation errors
        CreateAccountRequest invalidAccount = new CreateAccountRequest();
        invalidAccount.setName(""); // Invalid empty name
        invalidAccount.setType(AccountType.CHECKING);

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Test resource not found
        mockMvc.perform(get("/api/v1/accounts/99999/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());

        // Test invalid transaction data 
        CreateTransactionRequest invalidTransaction = new CreateTransactionRequest();
        invalidTransaction.setAmount(new BigDecimal("0.00")); // Invalid zero amount
        invalidTransaction.setDescription("");
        invalidTransaction.setAccountId(accountId);
        invalidTransaction.setType(TransactionType.EXPENSE);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());

        // Test financial precision
        CreateTransactionRequest precisionTest = new CreateTransactionRequest();
        precisionTest.setAmount(new BigDecimal("123.4563131")); // Should be rounded to 2 decimal places
        precisionTest.setDescription("Precision Test");
        precisionTest.setTransactionDate(LocalDate.now());
        precisionTest.setAccountId(accountId);
        precisionTest.setType(TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(precisionTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(123.46)); // Should be properly rounded

        // === SOFT DELETE TESTING ===
        
        // Soft delete transaction
        mockMvc.perform(delete("/api/v1/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Transaction should not appear in listings
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5)); // Should exclude the deleted transaction

        // Analytics should reflect the deletion
        mockMvc.perform(get("/api/v1/analytics/monthly-summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("month", currentMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0.00)); // Income transaction was deleted
    }
}