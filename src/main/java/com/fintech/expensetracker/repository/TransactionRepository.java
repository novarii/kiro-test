package com.fintech.expensetracker.repository;

import com.fintech.expensetracker.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transaction entity operations
 * Provides filtering and balance calculation queries
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all non-deleted transactions for a specific user
     * 
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of non-deleted transactions belonging to the user
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdAndDeletedFalse(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find all non-deleted transactions for a specific account
     * 
     * @param accountId the ID of the account
     * @param userId the ID of the user (for security)
     * @param pageable pagination information
     * @return page of non-deleted transactions for the account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.account.user.id = :userId AND t.deleted = false ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByAccountIdAndUserIdAndDeletedFalse(@Param("accountId") Long accountId, @Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find transaction by ID and user ID (for ownership validation)
     * 
     * @param transactionId the transaction ID
     * @param userId the user ID
     * @return Optional containing the transaction if found and belongs to user, empty otherwise
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE t.id = :transactionId AND a.user.id = :userId AND t.deleted = false")
    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(@Param("transactionId") Long transactionId, @Param("userId") Long userId);
    
    /**
     * Find transactions within a date range for a user
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return page of transactions within the date range
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdAndDateRangeAndDeletedFalse(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
    
    /**
     * Find transactions by category for a user
     * 
     * @param userId the ID of the user
     * @param categoryId the ID of the category
     * @param pageable pagination information
     * @return page of transactions in the specified category
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.category.id = :categoryId AND t.deleted = false ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdAndCategoryIdAndDeletedFalse(@Param("userId") Long userId, @Param("categoryId") Long categoryId, Pageable pageable);
    
    /**
     * Find transactions by amount range for a user
     * 
     * @param userId the ID of the user
     * @param minAmount the minimum amount (inclusive)
     * @param maxAmount the maximum amount (inclusive)
     * @param pageable pagination information
     * @return page of transactions within the amount range
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.amount BETWEEN :minAmount AND :maxAmount ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdAndAmountRangeAndDeletedFalse(@Param("userId") Long userId, @Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount, Pageable pageable);
    
    /**
     * Find income transactions (positive amounts) for a user within date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of income transactions
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.amount > 0 AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findIncomeByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Find expense transactions (negative amounts) for a user within date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of expense transactions
     */
    @Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.amount < 0 AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findExpensesByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Calculate total income for a user within date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return total income amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.amount > 0 AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalIncomeByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Calculate total expenses for a user within date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return total expense amount (will be negative)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.amount < 0 AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalExpensesByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Calculate account balance from transactions
     * 
     * @param accountId the ID of the account
     * @param userId the ID of the user (for security)
     * @return sum of all non-deleted transaction amounts for the account
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.account.user.id = :userId AND t.deleted = false")
    BigDecimal calculateAccountBalanceFromTransactions(@Param("accountId") Long accountId, @Param("userId") Long userId);
    
    /**
     * Get spending by category for a user within date range
     * Returns category ID, category name, and total amount spent
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of objects containing category info and spending totals
     */
    @Query("SELECT t.category.id, t.category.name, SUM(ABS(t.amount)) " +
           "FROM Transaction t JOIN t.account a " +
           "WHERE a.user.id = :userId AND t.deleted = false AND t.amount < 0 " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.id, t.category.name " +
           "ORDER BY SUM(ABS(t.amount)) DESC")
    List<Object[]> getSpendingByCategoryForUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Get monthly transaction summary for a user
     * Returns year, month, total income, total expenses
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of objects containing monthly summaries
     */
    @Query("SELECT YEAR(t.transactionDate), MONTH(t.transactionDate), " +
           "SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), " +
           "SUM(CASE WHEN t.amount < 0 THEN ABS(t.amount) ELSE 0 END) " +
           "FROM Transaction t JOIN t.account a " +
           "WHERE a.user.id = :userId AND t.deleted = false " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
           "ORDER BY YEAR(t.transactionDate), MONTH(t.transactionDate)")
    List<Object[]> getMonthlySummaryForUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Count transactions for a user within date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return number of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countByUserIdAndDateRangeAndDeletedFalse(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Check if user has any transactions in an account (for account deletion validation)
     * 
     * @param accountId the ID of the account
     * @param userId the ID of the user
     * @return true if account has transactions, false otherwise
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.account.id = :accountId AND t.account.user.id = :userId AND t.deleted = false")
    boolean existsByAccountIdAndUserIdAndDeletedFalse(@Param("accountId") Long accountId, @Param("userId") Long userId);
}