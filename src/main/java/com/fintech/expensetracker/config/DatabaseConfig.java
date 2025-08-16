package com.fintech.expensetracker.config;

import com.fintech.expensetracker.entity.Category;
import com.fintech.expensetracker.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database configuration class that handles database initialization
 * and ensures default data is properly set up
 */
@Configuration
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    /**
     * Command line runner to ensure default categories are properly initialized
     * This runs after the SQL scripts and provides additional validation
     */
    @Bean
    @Order(1)
    public CommandLineRunner initializeDefaultData(CategoryRepository categoryRepository) {
        return args -> {
            initializeDefaultCategories(categoryRepository);
        };
    }
    
    /**
     * Initialize default categories if they don't exist
     * This method ensures that the default "Uncategorized" category exists
     * and provides fallback initialization if SQL scripts fail
     */
    @Transactional
    private void initializeDefaultCategories(CategoryRepository categoryRepository) {
        logger.info("Initializing default categories...");
        
        try {
            // Check if default category exists
            if (!categoryRepository.existsByIsDefaultTrue()) {
                logger.warn("Default category not found, creating 'Uncategorized' category");
                Category defaultCategory = new Category(
                    "Uncategorized", 
                    "Default category for transactions without a specific category", 
                    true
                );
                categoryRepository.save(defaultCategory);
                logger.info("Created default 'Uncategorized' category");
            } else {
                logger.info("Default category already exists");
            }
            
            // Verify essential categories exist with descriptions
            String[][] essentialCategories = {
                {"Food & Dining", "Restaurants, groceries, and food-related expenses"},
                {"Transportation", "Gas, public transport, car maintenance, and travel expenses"},
                {"Shopping", "Clothing, electronics, and general retail purchases"},
                {"Entertainment", "Movies, games, subscriptions, and recreational activities"},
                {"Bills & Utilities", "Electricity, water, internet, phone, and other utility bills"},
                {"Healthcare", "Medical expenses, pharmacy, insurance, and health-related costs"},
                {"Education", "Tuition, books, courses, and educational expenses"},
                {"Home & Garden", "Home improvement, furniture, gardening, and household items"},
                {"Personal Care", "Haircuts, cosmetics, gym memberships, and personal services"},
                {"Insurance", "Life, health, auto, and other insurance premiums"},
                {"Taxes", "Income tax, property tax, and other tax payments"},
                {"Investments", "Stock purchases, retirement contributions, and investment fees"},
                {"Gifts & Donations", "Charitable donations, gifts, and contributions"},
                {"Travel", "Vacation expenses, hotels, flights, and travel-related costs"},
                {"Business", "Business meals, office supplies, and work-related expenses"},
                {"Salary", "Regular employment income and wages"},
                {"Freelance", "Freelance work and contract income"},
                {"Investment Income", "Dividends, interest, and capital gains"},
                {"Rental Income", "Income from rental properties"},
                {"Business Income", "Income from business operations"},
                {"Bonus", "Work bonuses and performance incentives"},
                {"Refunds", "Tax refunds, purchase returns, and reimbursements"},
                {"Other Income", "Miscellaneous income sources"}
            };
            
            int createdCount = 0;
            for (String[] categoryData : essentialCategories) {
                String categoryName = categoryData[0];
                String categoryDescription = categoryData[1];
                if (!categoryRepository.existsByName(categoryName)) {
                    Category category = new Category(categoryName, categoryDescription, false);
                    categoryRepository.save(category);
                    createdCount++;
                    logger.debug("Created missing category: {}", categoryName);
                }
            }
            
            if (createdCount > 0) {
                logger.info("Created {} missing essential categories", createdCount);
            }
            
            long totalCategories = categoryRepository.count();
            logger.info("Database initialization complete. Total categories: {}", totalCategories);
            
        } catch (Exception e) {
            logger.error("Error during default category initialization", e);
            throw new RuntimeException("Failed to initialize default categories", e);
        }
    }
}