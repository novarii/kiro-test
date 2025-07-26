package com.fintech.expensetracker.repository;

import com.fintech.expensetracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Category entity operations
 * Provides default category management and category lookup methods
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find category by name
     * 
     * @param name the category name to search for
     * @return Optional containing the category if found, empty otherwise
     */
    Optional<Category> findByName(String name);
    
    /**
     * Find the default category (typically "Uncategorized")
     * Used when transactions are created without a specific category
     * 
     * @return Optional containing the default category if found, empty otherwise
     */
    Optional<Category> findByIsDefaultTrue();
    
    /**
     * Find all categories ordered by name
     * 
     * @return list of all categories sorted alphabetically
     */
    @Query("SELECT c FROM Category c ORDER BY c.name ASC")
    List<Category> findAllOrderByName();
    
    /**
     * Find all non-default categories ordered by name
     * Useful for displaying user-selectable categories
     * 
     * @return list of non-default categories sorted alphabetically
     */
    @Query("SELECT c FROM Category c WHERE c.isDefault = false ORDER BY c.name ASC")
    List<Category> findAllNonDefaultOrderByName();
    
    /**
     * Check if a category exists with the given name
     * Used for validation to prevent duplicate category names
     * 
     * @param name the category name to check
     * @return true if category exists with this name, false otherwise
     */
    boolean existsByName(String name);
    
    /**
     * Check if a default category exists
     * Used during application initialization to ensure default category exists
     * 
     * @return true if a default category exists, false otherwise
     */
    boolean existsByIsDefaultTrue();
    
    /**
     * Find categories that have been used in transactions
     * Useful for analytics and reporting
     * 
     * @return list of categories that have associated transactions
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.transactions t WHERE t.deleted = false")
    List<Category> findCategoriesWithTransactions();
    
    /**
     * Find categories used by a specific user
     * Returns categories that have transactions from the user's accounts
     * 
     * @param userId the ID of the user
     * @return list of categories used by the user
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.transactions t JOIN t.account a WHERE a.user.id = :userId AND t.deleted = false ORDER BY c.name ASC")
    List<Category> findCategoriesUsedByUser(@Param("userId") Long userId);
    
    /**
     * Count transactions for a category
     * 
     * @param categoryId the ID of the category
     * @return number of non-deleted transactions in this category
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.category.id = :categoryId AND t.deleted = false")
    long countTransactionsByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * Count transactions for a category by a specific user
     * 
     * @param categoryId the ID of the category
     * @param userId the ID of the user
     * @return number of non-deleted transactions in this category by the user
     */
    @Query("SELECT COUNT(t) FROM Transaction t JOIN t.account a WHERE t.category.id = :categoryId AND a.user.id = :userId AND t.deleted = false")
    long countTransactionsByCategoryIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);
    
    /**
     * Find categories with transaction count for a user
     * Returns category information along with usage count
     * 
     * @param userId the ID of the user
     * @return list of objects containing category info and transaction counts
     */
    @Query("SELECT c.id, c.name, c.description, c.isDefault, COUNT(t) " +
           "FROM Category c LEFT JOIN c.transactions t LEFT JOIN t.account a " +
           "WHERE (a.user.id = :userId OR a.user.id IS NULL) AND (t.deleted = false OR t.deleted IS NULL) " +
           "GROUP BY c.id, c.name, c.description, c.isDefault " +
           "ORDER BY c.name ASC")
    List<Object[]> findCategoriesWithTransactionCountForUser(@Param("userId") Long userId);
    
    /**
     * Check if category can be safely deleted
     * A category can be deleted if it has no associated transactions or is not the default category
     * 
     * @param categoryId the ID of the category
     * @return true if category can be deleted, false otherwise
     */
    @Query("SELECT COUNT(t) = 0 AND c.isDefault = false FROM Category c LEFT JOIN c.transactions t WHERE c.id = :categoryId AND (t.deleted = false OR t.deleted IS NULL)")
    boolean canCategoryBeDeleted(@Param("categoryId") Long categoryId);
}