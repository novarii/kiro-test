package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.TransactionType;
import com.fintech.expensetracker.dto.request.CreateTransactionRequest;
import com.fintech.expensetracker.dto.request.TransactionFilter;
import com.fintech.expensetracker.dto.request.UpdateTransactionRequest;
import com.fintech.expensetracker.dto.response.AccountResponse;
import com.fintech.expensetracker.dto.response.CategoryResponse;
import com.fintech.expensetracker.dto.response.TransactionResponse;
import com.fintech.expensetracker.entity.Account;
import com.fintech.expensetracker.entity.Category;
import com.fintech.expensetracker.entity.Transaction;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.repository.AccountRepository;
import com.fintech.expensetracker.repository.CategoryRepository;
import com.fintech.expensetracker.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for transaction management operations
 * Handles transaction creation, updates, filtering, and categorization
 */
@Service
@Transactional
public class TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final String DEFAULT_CATEGORY_NAME = "Uncategorized";
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                            AccountRepository accountRepository,
                            CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Create a new transaction with amount validation and categorization
     * 
     * @param request the transaction creation request
     * @param userId the ID of the user creating the transaction
     * @return the created transaction response
     * @throws ResourceNotFoundException if account or category not found
     * @throws BadRequestException if validation fails
     */
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long userId) {
        logger.debug("Creating transaction for user {} with request: {}", userId, request);
        
        // DEBUG: Log the exact values being received
        logger.info("DEBUG - Amount received: [{}], Type: [{}], Class: [{}]", 
                   request.getAmount(), 
                   request.getType(),
                   request.getAmount() != null ? request.getAmount().getClass().getName() : "null");
        
        if (request.getAmount() != null) {
            logger.info("DEBUG - Amount details: value=[{}], scale=[{}], precision=[{}], signum=[{}]",
                       request.getAmount().toString(),
                       request.getAmount().scale(),
                       request.getAmount().precision(),
                       request.getAmount().signum());
        }
        
        // Validate and get account
        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + request.getAccountId()));
        
        // Get or assign default category
        Category category = getOrAssignDefaultCategory(request.getCategoryId());
        
        // Validate transaction date
        LocalDate transactionDate = request.getTransactionDate() != null ? 
                request.getTransactionDate() : LocalDate.now();
        
        // Convert amount based on transaction type
        BigDecimal amount = convertAmountByType(request.getAmount(), request.getType());
        
        // Create and save transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(request.getDescription().trim());
        transaction.setTransactionDate(transactionDate);
        transaction.setAccount(account);
        transaction.setCategory(category);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        logger.info("Created transaction with ID {} for user {}", savedTransaction.getId(), userId);
        return convertToTransactionResponse(savedTransaction);
    }
    
    /**
     * Update an existing transaction with audit trail
     * 
     * @param transactionId the ID of the transaction to update
     * @param request the update request
     * @param userId the ID of the user updating the transaction
     * @return the updated transaction response
     * @throws ResourceNotFoundException if transaction not found
     * @throws BadRequestException if validation fails
     */
    public TransactionResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, Long userId) {
        logger.debug("Updating transaction {} for user {} with request: {}", transactionId, userId, request);
        
        // Find existing transaction
        Transaction existingTransaction = transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with ID: " + transactionId));
        
        // Log original values for audit trail
        logTransactionUpdate(existingTransaction, request, userId);
        
        // Update fields if provided
        if (request.getAmount() != null && request.getType() != null) {
            BigDecimal newAmount = convertAmountByType(request.getAmount(), request.getType());
            existingTransaction.setAmount(newAmount);
        }
        
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            existingTransaction.setDescription(request.getDescription().trim());
        }
        
        if (request.getTransactionDate() != null) {
            existingTransaction.setTransactionDate(request.getTransactionDate());
        }
        
        if (request.getAccountId() != null) {
            Account newAccount = accountRepository.findByIdAndUserIdAndDeletedFalse(request.getAccountId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account not found with ID: " + request.getAccountId()));
            existingTransaction.setAccount(newAccount);
        }
        
        if (request.getCategoryId() != null) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with ID: " + request.getCategoryId()));
            existingTransaction.setCategory(newCategory);
        }
        
        Transaction updatedTransaction = transactionRepository.save(existingTransaction);
        
        logger.info("Updated transaction with ID {} for user {}", transactionId, userId);
        return convertToTransactionResponse(updatedTransaction);
    }
    
    /**
     * Get transactions for a user with filtering capabilities
     * 
     * @param userId the ID of the user
     * @param filter the filter criteria
     * @param page the page number (0-based)
     * @param size the page size
     * @return page of filtered transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactions(Long userId, TransactionFilter filter, int page, int size) {
        logger.debug("Getting transactions for user {} with filter: {}", userId, filter);
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt"));
        
        Page<Transaction> transactions;
        
        if (filter == null || !filter.hasFilters()) {
            // No filters - get all user transactions
            transactions = transactionRepository.findByUserIdAndDeletedFalse(userId, pageable);
        } else {
            // Apply filters
            transactions = getFilteredTransactions(userId, filter, pageable);
        }
        
        return transactions.map(this::convertToTransactionResponse);
    }
    
    /**
     * Get a specific transaction by ID
     * 
     * @param transactionId the transaction ID
     * @param userId the user ID for ownership validation
     * @return the transaction response
     * @throws ResourceNotFoundException if transaction not found
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long transactionId, Long userId) {
        logger.debug("Getting transaction {} for user {}", transactionId, userId);
        
        Transaction transaction = transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with ID: " + transactionId));
        
        return convertToTransactionResponse(transaction);
    }
    
    /**
     * Soft delete a transaction
     * 
     * @param transactionId the ID of the transaction to delete
     * @param userId the ID of the user deleting the transaction
     * @throws ResourceNotFoundException if transaction not found
     */
    public void softDeleteTransaction(Long transactionId, Long userId) {
        logger.debug("Soft deleting transaction {} for user {}", transactionId, userId);
        
        Transaction transaction = transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with ID: " + transactionId));
        
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
        
        logger.info("Soft deleted transaction with ID {} for user {}", transactionId, userId);
    }
    
    /**
     * Get transactions for a specific account
     * 
     * @param accountId the account ID
     * @param userId the user ID for ownership validation
     * @param page the page number
     * @param size the page size
     * @return page of transactions for the account
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(Long accountId, Long userId, int page, int size) {
        logger.debug("Getting transactions for account {} and user {}", accountId, userId);
        
        // Validate account ownership
        accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + accountId));
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt"));
        
        Page<Transaction> transactions = transactionRepository
                .findByAccountIdAndUserIdAndDeletedFalse(accountId, userId, pageable);
        
        return transactions.map(this::convertToTransactionResponse);
    }
    
    // Private helper methods
    
    /**
     * Get or assign default category if none specified
     */
    private Category getOrAssignDefaultCategory(Long categoryId) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with ID: " + categoryId));
        }
        
        // Get or create default category
        return categoryRepository.findByIsDefaultTrue()
                .orElseGet(this::createDefaultCategory);
    }
    
    /**
     * Create default "Uncategorized" category if it doesn't exist
     */
    private Category createDefaultCategory() {
        logger.info("Creating default category: {}", DEFAULT_CATEGORY_NAME);
        
        Category defaultCategory = new Category();
        defaultCategory.setName(DEFAULT_CATEGORY_NAME);
        defaultCategory.setDescription("Default category for uncategorized transactions");
        defaultCategory.setDefault(true);
        
        return categoryRepository.save(defaultCategory);
    }
    
    /**
     * Convert amount based on transaction type
     * Income = positive, Expense = negative
     */
    private BigDecimal convertAmountByType(BigDecimal amount, TransactionType type) {
        logger.info("DEBUG - convertAmountByType called with amount: [{}], type: [{}]", amount, type);
        
        if (amount == null || type == null) {
            logger.error("DEBUG - Amount or type is null! Amount: [{}], Type: [{}]", amount, type);
            throw new BadRequestException("Amount and transaction type are required");
        }
        
        logger.info("DEBUG - Amount comparison with zero: amount.compareTo(BigDecimal.ZERO) = [{}]", 
                   amount.compareTo(BigDecimal.ZERO));
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("DEBUG - Amount validation failed! Amount: [{}], signum: [{}], scale: [{}]", 
                        amount, amount.signum(), amount.scale());
            throw new BadRequestException("Amount must be greater than zero");
        }
        
        BigDecimal result = type == TransactionType.INCOME ? amount : amount.negate();
        logger.info("DEBUG - Converted amount: [{}] -> [{}] for type: [{}]", amount, result, type);
        return result;
    }
    
    /**
     * Apply filters to get filtered transactions
     */
    private Page<Transaction> getFilteredTransactions(Long userId, TransactionFilter filter, Pageable pageable) {
        // For complex filtering, we'll use the most specific filter available
        // This is a simplified approach - in a real application, you might want to use Criteria API
        
        if (filter.getAccountId() != null) {
            return transactionRepository.findByAccountIdAndUserIdAndDeletedFalse(
                    filter.getAccountId(), userId, pageable);
        }
        
        if (filter.getCategoryId() != null) {
            return transactionRepository.findByUserIdAndCategoryIdAndDeletedFalse(
                    userId, filter.getCategoryId(), pageable);
        }
        
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            return transactionRepository.findByUserIdAndDateRangeAndDeletedFalse(
                    userId, filter.getStartDate(), filter.getEndDate(), pageable);
        }
        
        if (filter.getMinAmount() != null && filter.getMaxAmount() != null) {
            return transactionRepository.findByUserIdAndAmountRangeAndDeletedFalse(
                    userId, filter.getMinAmount(), filter.getMaxAmount(), pageable);
        }
        
        // Default to all user transactions if no specific filter matches
        return transactionRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }
    
    /**
     * Log transaction update for audit trail
     */
    private void logTransactionUpdate(Transaction original, UpdateTransactionRequest request, Long userId) {
        logger.info("Transaction update audit - User: {}, Transaction ID: {}, " +
                   "Original: [amount={}, description='{}', date={}, account={}, category={}], " +
                   "Update request: {}",
                   userId, original.getId(),
                   original.getAmount(), original.getDescription(), original.getTransactionDate(),
                   original.getAccount().getId(), original.getCategory().getId(),
                   request);
    }
    
    /**
     * Convert Transaction entity to TransactionResponse DTO
     */
    private TransactionResponse convertToTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        
        // Determine transaction type based on amount
        response.setType(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                TransactionType.INCOME : TransactionType.EXPENSE);
        
        // Convert account to AccountResponse
        Account account = transaction.getAccount();
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setName(account.getName());
        accountResponse.setType(account.getType());
        response.setAccount(accountResponse);
        
        // Convert category to CategoryResponse
        Category category = transaction.getCategory();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setId(category.getId());
        categoryResponse.setName(category.getName());
        categoryResponse.setDescription(category.getDescription());
        categoryResponse.setDefault(category.isDefault());
        response.setCategory(categoryResponse);
        
        return response;
    }
}