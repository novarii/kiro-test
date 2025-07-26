package com.fintech.expensetracker.repository;

import com.fintech.expensetracker.entity.Account;
import com.fintech.expensetracker.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity operations
 * Provides user-specific queries for account management
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * Find all non-deleted accounts for a specific user
     * 
     * @param userId the ID of the user
     * @return list of non-deleted accounts belonging to the user
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.deleted = false ORDER BY a.createdAt DESC")
    List<Account> findByUserIdAndDeletedFalse(@Param("userId") Long userId);
    
    /**
     * Find all accounts (including deleted) for a specific user
     * 
     * @param userId the ID of the user
     * @return list of all accounts belonging to the user
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<Account> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find account by ID and user ID (for ownership validation)
     * Only returns non-deleted accounts
     * 
     * @param accountId the account ID
     * @param userId the user ID
     * @return Optional containing the account if found and belongs to user, empty otherwise
     */
    @Query("SELECT a FROM Account a WHERE a.id = :accountId AND a.user.id = :userId AND a.deleted = false")
    Optional<Account> findByIdAndUserIdAndDeletedFalse(@Param("accountId") Long accountId, @Param("userId") Long userId);
    
    /**
     * Find account by ID and user ID including deleted accounts (for audit purposes)
     * 
     * @param accountId the account ID
     * @param userId the user ID
     * @return Optional containing the account if found and belongs to user, empty otherwise
     */
    @Query("SELECT a FROM Account a WHERE a.id = :accountId AND a.user.id = :userId")
    Optional<Account> findByIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);
    
    /**
     * Find accounts by type for a specific user
     * Only returns non-deleted accounts
     * 
     * @param userId the ID of the user
     * @param accountType the type of account to filter by
     * @return list of accounts of the specified type belonging to the user
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.type = :accountType AND a.deleted = false ORDER BY a.createdAt DESC")
    List<Account> findByUserIdAndTypeAndDeletedFalse(@Param("userId") Long userId, @Param("accountType") AccountType accountType);
    
    /**
     * Count non-deleted accounts for a user
     * 
     * @param userId the ID of the user
     * @return number of non-deleted accounts
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.user.id = :userId AND a.deleted = false")
    long countByUserIdAndDeletedFalse(@Param("userId") Long userId);
    
    /**
     * Check if account exists and belongs to user (for validation)
     * Only checks non-deleted accounts
     * 
     * @param accountId the account ID
     * @param userId the user ID
     * @return true if account exists and belongs to user, false otherwise
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.id = :accountId AND a.user.id = :userId AND a.deleted = false")
    boolean existsByIdAndUserIdAndDeletedFalse(@Param("accountId") Long accountId, @Param("userId") Long userId);
    
    /**
     * Find account with transactions for balance calculation
     * 
     * @param accountId the account ID
     * @param userId the user ID
     * @return Optional containing the account with transactions if found and belongs to user
     */
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.transactions t WHERE a.id = :accountId AND a.user.id = :userId AND a.deleted = false")
    Optional<Account> findByIdAndUserIdWithTransactions(@Param("accountId") Long accountId, @Param("userId") Long userId);
    
    /**
     * Calculate total balance across all user accounts
     * This sums initial balances and all non-deleted transaction amounts
     * 
     * @param userId the ID of the user
     * @return total balance across all user accounts
     */
    @Query("SELECT COALESCE(SUM(a.initialBalance), 0) + COALESCE(SUM(t.amount), 0) " +
           "FROM Account a LEFT JOIN a.transactions t " +
           "WHERE a.user.id = :userId AND a.deleted = false AND (t.deleted = false OR t.deleted IS NULL)")
    BigDecimal calculateTotalBalanceForUser(@Param("userId") Long userId);
}