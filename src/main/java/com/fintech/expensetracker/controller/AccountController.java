package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.request.CreateAccountRequest;
import com.fintech.expensetracker.dto.request.UpdateAccountRequest;
import com.fintech.expensetracker.dto.response.AccountResponse;
import com.fintech.expensetracker.security.UserPrincipal;
import com.fintech.expensetracker.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for account management operations
 * Provides CRUD operations for user accounts with proper authentication and authorization
 */
@RestController
@RequestMapping("/api/v1/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AccountController {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    
    private final AccountService accountService;
    
    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    /**
     * Get all accounts for the authenticated user
     * 
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing list of user's accounts
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getUserAccounts(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Retrieving accounts for user ID: {}", userPrincipal.getId());
        
        List<AccountResponse> accounts = accountService.getUserAccounts(userPrincipal.getId());
        
        logger.info("Retrieved {} accounts for user ID: {}", accounts.size(), userPrincipal.getId());
        return ResponseEntity.ok(accounts);
    }
    
    /**
     * Get a specific account by ID for the authenticated user
     * 
     * @param accountId the ID of the account to retrieve
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the account details
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Retrieving account ID: {} for user ID: {}", accountId, userPrincipal.getId());
        
        AccountResponse account = accountService.getAccountById(accountId, userPrincipal.getId());
        
        logger.info("Retrieved account ID: {} for user ID: {}", accountId, userPrincipal.getId());
        return ResponseEntity.ok(account);
    }
    
    /**
     * Create a new account for the authenticated user
     * 
     * @param createAccountRequest the account creation request
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the created account details
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Creating account for user ID: {} with name: {}", 
                   userPrincipal.getId(), createAccountRequest.getName());
        
        AccountResponse createdAccount = accountService.createAccount(createAccountRequest, userPrincipal.getId());
        
        logger.info("Successfully created account ID: {} for user ID: {}", 
                   createdAccount.getId(), userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }
    
    /**
     * Update an existing account for the authenticated user
     * 
     * @param accountId the ID of the account to update
     * @param updateAccountRequest the account update request
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the updated account details
     */
    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountRequest updateAccountRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Updating account ID: {} for user ID: {}", accountId, userPrincipal.getId());
        
        AccountResponse updatedAccount = accountService.updateAccount(
                accountId, updateAccountRequest, userPrincipal.getId());
        
        logger.info("Successfully updated account ID: {} for user ID: {}", 
                   accountId, userPrincipal.getId());
        return ResponseEntity.ok(updatedAccount);
    }
    
    /**
     * Soft delete an account for the authenticated user
     * Only allows deletion if account has no active transactions
     * 
     * @param accountId the ID of the account to delete
     * @param userPrincipal the authenticated user
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Deleting account ID: {} for user ID: {}", accountId, userPrincipal.getId());
        
        accountService.softDeleteAccount(accountId, userPrincipal.getId());
        
        logger.info("Successfully deleted account ID: {} for user ID: {}", 
                   accountId, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get the calculated balance for a specific account
     * Balance is calculated from initial balance plus sum of all transactions
     * 
     * @param accountId the ID of the account
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the calculated balance
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getAccountBalance(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Calculating balance for account ID: {} for user ID: {}", 
                   accountId, userPrincipal.getId());
        
        BigDecimal balance = accountService.calculateAccountBalance(accountId, userPrincipal.getId());
        
        BalanceResponse balanceResponse = new BalanceResponse(accountId, balance);
        
        logger.info("Calculated balance for account ID: {} is: {}", accountId, balance);
        return ResponseEntity.ok(balanceResponse);
    }
    
    /**
     * Get total balance across all user accounts
     * 
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the total balance
     */
    @GetMapping("/total-balance")
    public ResponseEntity<TotalBalanceResponse> getTotalBalance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Calculating total balance for user ID: {}", userPrincipal.getId());
        
        BigDecimal totalBalance = accountService.getTotalBalanceForUser(userPrincipal.getId());
        
        TotalBalanceResponse totalBalanceResponse = new TotalBalanceResponse(
                userPrincipal.getId(), totalBalance);
        
        logger.info("Total balance for user ID: {} is: {}", userPrincipal.getId(), totalBalance);
        return ResponseEntity.ok(totalBalanceResponse);
    }
    
    /**
     * Inner class for balance response
     */
    public static class BalanceResponse {
        private Long accountId;
        private BigDecimal balance;
        
        public BalanceResponse(Long accountId, BigDecimal balance) {
            this.accountId = accountId;
            this.balance = balance;
        }
        
        public Long getAccountId() {
            return accountId;
        }
        
        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }
        
        public BigDecimal getBalance() {
            return balance;
        }
        
        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }
    
    /**
     * Inner class for total balance response
     */
    public static class TotalBalanceResponse {
        private Long userId;
        private BigDecimal totalBalance;
        
        public TotalBalanceResponse(Long userId, BigDecimal totalBalance) {
            this.userId = userId;
            this.totalBalance = totalBalance;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public BigDecimal getTotalBalance() {
            return totalBalance;
        }
        
        public void setTotalBalance(BigDecimal totalBalance) {
            this.totalBalance = totalBalance;
        }
    }
}