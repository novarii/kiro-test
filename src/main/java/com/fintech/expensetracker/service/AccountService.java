package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.request.CreateAccountRequest;
import com.fintech.expensetracker.dto.request.UpdateAccountRequest;
import com.fintech.expensetracker.dto.response.AccountResponse;
import com.fintech.expensetracker.entity.Account;
import com.fintech.expensetracker.entity.User;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.repository.AccountRepository;
import com.fintech.expensetracker.repository.TransactionRepository;
import com.fintech.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for account management operations
 * Handles account creation, listing, updates, soft deletion, and balance calculations
 */
@Service
@Transactional
public class AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public AccountService(AccountRepository accountRepository, 
                         UserRepository userRepository,
                         TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Create a new account for a user with ownership validation
     * 
     * @param request the account creation request
     * @param userId the ID of the user creating the account
     * @return AccountResponse containing the created account details
     * @throws ResourceNotFoundException if user is not found
     */
    public AccountResponse createAccount(CreateAccountRequest request, Long userId) {
        logger.info("Creating account for user ID: {}", userId);
        
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Create new account
        Account account = new Account(
                request.getName(),
                request.getType(),
                request.getInitialBalance(),
                user
        );
        
        // Save account
        Account savedAccount = accountRepository.save(account);
        
        logger.info("Successfully created account with ID: {} for user ID: {}", savedAccount.getId(), userId);
        
        return mapToAccountResponse(savedAccount);
    }
    
    /**
     * Get all accounts for a user with user isolation
     * 
     * @param userId the ID of the user
     * @return list of AccountResponse objects belonging to the user
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(Long userId) {
        logger.info("Retrieving accounts for user ID: {}", userId);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        
        // Get user's accounts
        List<Account> accounts = accountRepository.findByUserIdAndDeletedFalse(userId);
        
        logger.info("Found {} accounts for user ID: {}", accounts.size(), userId);
        
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Update account information with ownership validation
     * 
     * @param accountId the ID of the account to update
     * @param request the update request containing new values
     * @param userId the ID of the user making the request
     * @return AccountResponse containing updated account details
     * @throws ResourceNotFoundException if account is not found or doesn't belong to user
     */
    public AccountResponse updateAccount(Long accountId, UpdateAccountRequest request, Long userId) {
        logger.info("Updating account ID: {} for user ID: {}", accountId, userId);
        
        // Find account with ownership validation
        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + accountId + " for user ID: " + userId));
        
        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            account.setName(request.getName().trim());
        }
        
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        
        if (request.getInitialBalance() != null) {
            account.setInitialBalance(request.getInitialBalance());
        }
        
        // Save updated account
        Account updatedAccount = accountRepository.save(account);
        
        logger.info("Successfully updated account ID: {} for user ID: {}", accountId, userId);
        
        return mapToAccountResponse(updatedAccount);
    }
    
    /**
     * Soft delete an account with transaction validation
     * Only allows deletion if account has no active transactions
     * 
     * @param accountId the ID of the account to delete
     * @param userId the ID of the user making the request
     * @throws ResourceNotFoundException if account is not found or doesn't belong to user
     * @throws BadRequestException if account has existing transactions
     */
    public void softDeleteAccount(Long accountId, Long userId) {
        logger.info("Attempting to delete account ID: {} for user ID: {}", accountId, userId);
        
        // Find account with ownership validation
        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + accountId + " for user ID: " + userId));
        
        // Check if account has any active transactions
        boolean hasTransactions = transactionRepository.existsByAccountIdAndUserIdAndDeletedFalse(accountId, userId);
        
        if (hasTransactions) {
            throw new BadRequestException(
                    "Cannot delete account with existing transactions. Account ID: " + accountId);
        }
        
        // Perform soft delete
        account.setDeleted(true);
        accountRepository.save(account);
        
        logger.info("Successfully soft deleted account ID: {} for user ID: {}", accountId, userId);
    }
    
    /**
     * Calculate account balance from transaction history
     * 
     * @param accountId the ID of the account
     * @param userId the ID of the user (for ownership validation)
     * @return the calculated account balance
     * @throws ResourceNotFoundException if account is not found or doesn't belong to user
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAccountBalance(Long accountId, Long userId) {
        logger.info("Calculating balance for account ID: {} for user ID: {}", accountId, userId);
        
        // Validate account exists and belongs to user
        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + accountId + " for user ID: " + userId));
        
        // Calculate balance: initial balance + sum of all non-deleted transactions
        BigDecimal transactionSum = transactionRepository.calculateAccountBalanceFromTransactions(accountId, userId);
        BigDecimal totalBalance = account.getInitialBalance().add(transactionSum);
        
        logger.info("Calculated balance for account ID: {} is: {}", accountId, totalBalance);
        
        return totalBalance;
    }
    
    /**
     * Get account by ID with ownership validation
     * 
     * @param accountId the ID of the account
     * @param userId the ID of the user
     * @return AccountResponse containing account details
     * @throws ResourceNotFoundException if account is not found or doesn't belong to user
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, Long userId) {
        logger.info("Retrieving account ID: {} for user ID: {}", accountId, userId);
        
        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + accountId + " for user ID: " + userId));
        
        return mapToAccountResponse(account);
    }
    
    /**
     * Check if account exists and belongs to user
     * 
     * @param accountId the ID of the account
     * @param userId the ID of the user
     * @return true if account exists and belongs to user, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean accountExistsForUser(Long accountId, Long userId) {
        return accountRepository.existsByIdAndUserIdAndDeletedFalse(accountId, userId);
    }
    
    /**
     * Get total balance across all user accounts
     * 
     * @param userId the ID of the user
     * @return total balance across all user accounts
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceForUser(Long userId) {
        logger.info("Calculating total balance for user ID: {}", userId);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        
        BigDecimal totalBalance = accountRepository.calculateTotalBalanceForUser(userId);
        
        logger.info("Total balance for user ID: {} is: {}", userId, totalBalance);
        
        return totalBalance != null ? totalBalance : BigDecimal.ZERO;
    }
    
    /**
     * Map Account entity to AccountResponse DTO
     * 
     * @param account the Account entity
     * @return AccountResponse DTO
     */
    private AccountResponse mapToAccountResponse(Account account) {
        // Calculate current balance
        BigDecimal currentBalance = account.getInitialBalance();
        if (account.getTransactions() != null && !account.getTransactions().isEmpty()) {
            BigDecimal transactionSum = account.getTransactions().stream()
                    .filter(transaction -> !transaction.isDeleted())
                    .map(transaction -> transaction.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            currentBalance = currentBalance.add(transactionSum);
        } else {
            // If transactions are not loaded, calculate from repository
            BigDecimal transactionSum = transactionRepository.calculateAccountBalanceFromTransactions(
                    account.getId(), account.getUser().getId());
            currentBalance = currentBalance.add(transactionSum);
        }
        
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                currentBalance,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}