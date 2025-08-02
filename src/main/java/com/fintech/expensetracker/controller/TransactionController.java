package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.request.CreateTransactionRequest;
import com.fintech.expensetracker.dto.request.TransactionFilter;
import com.fintech.expensetracker.dto.request.UpdateTransactionRequest;
import com.fintech.expensetracker.dto.response.TransactionResponse;
import com.fintech.expensetracker.security.UserPrincipal;
import com.fintech.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for transaction management operations
 * Provides CRUD operations for user transactions with proper authentication, authorization, and filtering
 */
@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    
    private final TransactionService transactionService;
    
    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    /**
     * Get all transactions for the authenticated user with optional filtering and pagination
     * 
     * @param userPrincipal the authenticated user
     * @param accountId optional filter by account ID
     * @param categoryId optional filter by category ID
     * @param startDate optional filter by start date (YYYY-MM-DD)
     * @param endDate optional filter by end date (YYYY-MM-DD)
     * @param minAmount optional filter by minimum amount
     * @param maxAmount optional filter by maximum amount
     * @param description optional filter by description (partial match)
     * @param page page number (0-based, default: 0)
     * @param size page size (default: 20)
     * @return ResponseEntity containing paginated list of user's transactions
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Retrieving transactions for user ID: {} with filters - page: {}, size: {}", 
                   userPrincipal.getId(), page, size);
        
        // Build filter object from request parameters
        TransactionFilter filter = buildTransactionFilter(accountId, categoryId, startDate, endDate, 
                                                         minAmount, maxAmount, description);
        
        Page<TransactionResponse> transactions = transactionService.getUserTransactions(
                userPrincipal.getId(), filter, page, size);
        
        logger.info("Retrieved {} transactions for user ID: {} (page {}/{})", 
                   transactions.getNumberOfElements(), userPrincipal.getId(), 
                   page + 1, transactions.getTotalPages());
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get a specific transaction by ID for the authenticated user
     * 
     * @param transactionId the ID of the transaction to retrieve
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the transaction details
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Retrieving transaction ID: {} for user ID: {}", transactionId, userPrincipal.getId());
        
        TransactionResponse transaction = transactionService.getTransactionById(transactionId, userPrincipal.getId());
        
        logger.info("Retrieved transaction ID: {} for user ID: {}", transactionId, userPrincipal.getId());
        return ResponseEntity.ok(transaction);
    }
    
    /**
     * Create a new transaction for the authenticated user
     * 
     * @param createTransactionRequest the transaction creation request
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the created transaction details
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest createTransactionRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Creating transaction for user ID: {} with amount: {} and description: '{}'", 
                   userPrincipal.getId(), createTransactionRequest.getAmount(), 
                   createTransactionRequest.getDescription());
        
        TransactionResponse createdTransaction = transactionService.createTransaction(
                createTransactionRequest, userPrincipal.getId());
        
        logger.info("Successfully created transaction ID: {} for user ID: {}", 
                   createdTransaction.getId(), userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }
    
    /**
     * Update an existing transaction for the authenticated user
     * 
     * @param transactionId the ID of the transaction to update
     * @param updateTransactionRequest the transaction update request
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing the updated transaction details
     */
    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateTransactionRequest updateTransactionRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Updating transaction ID: {} for user ID: {}", transactionId, userPrincipal.getId());
        
        TransactionResponse updatedTransaction = transactionService.updateTransaction(
                transactionId, updateTransactionRequest, userPrincipal.getId());
        
        logger.info("Successfully updated transaction ID: {} for user ID: {}", 
                   transactionId, userPrincipal.getId());
        return ResponseEntity.ok(updatedTransaction);
    }
    
    /**
     * Soft delete a transaction for the authenticated user
     * Maintains transaction history for financial integrity
     * 
     * @param transactionId the ID of the transaction to delete
     * @param userPrincipal the authenticated user
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Deleting transaction ID: {} for user ID: {}", transactionId, userPrincipal.getId());
        
        transactionService.softDeleteTransaction(transactionId, userPrincipal.getId());
        
        logger.info("Successfully deleted transaction ID: {} for user ID: {}", 
                   transactionId, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get transactions for a specific account
     * 
     * @param accountId the ID of the account
     * @param userPrincipal the authenticated user
     * @param page page number (0-based, default: 0)
     * @param size page size (default: 20)
     * @return ResponseEntity containing paginated list of account transactions
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Retrieving transactions for account ID: {} and user ID: {} - page: {}, size: {}", 
                   accountId, userPrincipal.getId(), page, size);
        
        Page<TransactionResponse> transactions = transactionService.getAccountTransactions(
                accountId, userPrincipal.getId(), page, size);
        
        logger.info("Retrieved {} transactions for account ID: {} and user ID: {} (page {}/{})", 
                   transactions.getNumberOfElements(), accountId, userPrincipal.getId(), 
                   page + 1, transactions.getTotalPages());
        
        return ResponseEntity.ok(transactions);
    }
    
    // Private helper methods
    
    /**
     * Build TransactionFilter object from request parameters
     */
    private TransactionFilter buildTransactionFilter(Long accountId, Long categoryId, 
                                                   String startDate, String endDate,
                                                   String minAmount, String maxAmount, 
                                                   String description) {
        TransactionFilter filter = new TransactionFilter();
        
        if (accountId != null) {
            filter.setAccountId(accountId);
        }
        
        if (categoryId != null) {
            filter.setCategoryId(categoryId);
        }
        
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                filter.setStartDate(java.time.LocalDate.parse(startDate));
            } catch (Exception e) {
                logger.warn("Invalid start date format: {}", startDate);
                // Invalid date format will be ignored, could also throw BadRequestException
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                filter.setEndDate(java.time.LocalDate.parse(endDate));
            } catch (Exception e) {
                logger.warn("Invalid end date format: {}", endDate);
                // Invalid date format will be ignored, could also throw BadRequestException
            }
        }
        
        if (minAmount != null && !minAmount.trim().isEmpty()) {
            try {
                filter.setMinAmount(new java.math.BigDecimal(minAmount));
            } catch (Exception e) {
                logger.warn("Invalid min amount format: {}", minAmount);
                // Invalid amount format will be ignored, could also throw BadRequestException
            }
        }
        
        if (maxAmount != null && !maxAmount.trim().isEmpty()) {
            try {
                filter.setMaxAmount(new java.math.BigDecimal(maxAmount));
            } catch (Exception e) {
                logger.warn("Invalid max amount format: {}", maxAmount);
                // Invalid amount format will be ignored, could also throw BadRequestException
            }
        }
        
        if (description != null && !description.trim().isEmpty()) {
            filter.setDescription(description.trim());
        }
        
        return filter;
    }
}