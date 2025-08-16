package com.fintech.expensetracker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Utility class for database migrations and maintenance operations
 * Provides methods for schema updates and data migrations
 */
@Component
public class DatabaseMigrationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationUtil.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Check if a table exists in the database
     * 
     * @param tableName the name of the table to check
     * @return true if table exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception e) {
            logger.warn("Error checking if table {} exists: {}", tableName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a column exists in a table
     * 
     * @param tableName the name of the table
     * @param columnName the name of the column to check
     * @return true if column exists, false otherwise
     */
    public boolean columnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                tableName.toUpperCase(), columnName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception e) {
            logger.warn("Error checking if column {}.{} exists: {}", tableName, columnName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an index exists
     * 
     * @param indexName the name of the index to check
     * @return true if index exists, false otherwise
     */
    public boolean indexExists(String indexName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, indexName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception e) {
            logger.warn("Error checking if index {} exists: {}", indexName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute a DDL statement safely
     * 
     * @param sql the DDL statement to execute
     * @param description description of the operation for logging
     * @return true if successful, false otherwise
     */
    public boolean executeDDL(String sql, String description) {
        try {
            logger.info("Executing DDL: {}", description);
            jdbcTemplate.execute(sql);
            logger.info("Successfully executed: {}", description);
            return true;
        } catch (Exception e) {
            logger.error("Error executing DDL ({}): {}", description, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get database statistics for monitoring
     * 
     * @return map containing database statistics
     */
    public Map<String, Object> getDatabaseStatistics() {
        try {
            String sql = """
                SELECT 
                    (SELECT COUNT(*) FROM users) as user_count,
                    (SELECT COUNT(*) FROM accounts WHERE deleted = false) as active_accounts,
                    (SELECT COUNT(*) FROM transactions WHERE deleted = false) as active_transactions,
                    (SELECT COUNT(*) FROM categories) as category_count
                """;
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return result.isEmpty() ? Map.of() : result.get(0);
        } catch (Exception e) {
            logger.error("Error getting database statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * Optimize database by updating statistics and rebuilding indexes
     * This method can be called periodically for maintenance
     */
    public void optimizeDatabase() {
        try {
            logger.info("Starting database optimization...");
            
            // For H2, we can analyze tables to update statistics
            String[] tables = {"users", "accounts", "transactions", "categories"};
            for (String table : tables) {
                try {
                    jdbcTemplate.execute("ANALYZE TABLE " + table);
                    logger.debug("Analyzed table: {}", table);
                } catch (Exception e) {
                    logger.warn("Could not analyze table {}: {}", table, e.getMessage());
                }
            }
            
            logger.info("Database optimization completed");
        } catch (Exception e) {
            logger.error("Error during database optimization: {}", e.getMessage());
        }
    }
    
    /**
     * Validate database integrity
     * Checks for orphaned records and constraint violations
     * 
     * @return true if database is consistent, false otherwise
     */
    public boolean validateDatabaseIntegrity() {
        try {
            logger.info("Validating database integrity...");
            boolean isValid = true;
            
            // Check for orphaned accounts (accounts without users)
            String orphanedAccountsSql = "SELECT COUNT(*) FROM accounts a LEFT JOIN users u ON a.user_id = u.id WHERE u.id IS NULL";
            Integer orphanedAccounts = jdbcTemplate.queryForObject(orphanedAccountsSql, Integer.class);
            if (orphanedAccounts != null && orphanedAccounts > 0) {
                logger.warn("Found {} orphaned accounts", orphanedAccounts);
                isValid = false;
            }
            
            // Check for orphaned transactions (transactions without accounts)
            String orphanedTransactionsSql = "SELECT COUNT(*) FROM transactions t LEFT JOIN accounts a ON t.account_id = a.id WHERE a.id IS NULL";
            Integer orphanedTransactions = jdbcTemplate.queryForObject(orphanedTransactionsSql, Integer.class);
            if (orphanedTransactions != null && orphanedTransactions > 0) {
                logger.warn("Found {} orphaned transactions", orphanedTransactions);
                isValid = false;
            }
            
            // Check for transactions without categories
            String transactionsWithoutCategorySql = "SELECT COUNT(*) FROM transactions t LEFT JOIN categories c ON t.category_id = c.id WHERE c.id IS NULL";
            Integer transactionsWithoutCategory = jdbcTemplate.queryForObject(transactionsWithoutCategorySql, Integer.class);
            if (transactionsWithoutCategory != null && transactionsWithoutCategory > 0) {
                logger.warn("Found {} transactions without valid categories", transactionsWithoutCategory);
                isValid = false;
            }
            
            // Check if default category exists
            String defaultCategorySql = "SELECT COUNT(*) FROM categories WHERE is_default = true";
            Integer defaultCategories = jdbcTemplate.queryForObject(defaultCategorySql, Integer.class);
            if (defaultCategories == null || defaultCategories == 0) {
                logger.error("No default category found");
                isValid = false;
            } else if (defaultCategories > 1) {
                logger.warn("Multiple default categories found: {}", defaultCategories);
                isValid = false;
            }
            
            if (isValid) {
                logger.info("Database integrity validation passed");
            } else {
                logger.error("Database integrity validation failed");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error during database integrity validation: {}", e.getMessage());
            return false;
        }
    }
}