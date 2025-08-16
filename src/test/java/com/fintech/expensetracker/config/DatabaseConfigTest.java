package com.fintech.expensetracker.config;

import com.fintech.expensetracker.entity.Category;
import com.fintech.expensetracker.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for DatabaseConfig to verify database initialization
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseConfigTest {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Test
    void testDefaultCategoryExists() {
        // Given & When - database should be initialized by DatabaseConfig
        
        // Then
        Optional<Category> defaultCategory = categoryRepository.findByIsDefaultTrue();
        assertThat(defaultCategory).isPresent();
        assertThat(defaultCategory.get().getName()).isEqualTo("Uncategorized");
        assertThat(defaultCategory.get().isDefault()).isTrue();
    }
    
    @Test
    void testEssentialCategoriesExist() {
        // Given
        String[] expectedCategories = {
            "Uncategorized", "Food & Dining", "Transportation", "Shopping", 
            "Entertainment", "Bills & Utilities", "Healthcare", "Salary"
        };
        
        // When
        List<Category> allCategories = categoryRepository.findAll();
        
        // Then
        assertThat(allCategories).hasSizeGreaterThanOrEqualTo(expectedCategories.length);
        
        for (String expectedCategory : expectedCategories) {
            assertThat(categoryRepository.existsByName(expectedCategory))
                .as("Category '%s' should exist", expectedCategory)
                .isTrue();
        }
    }
    
    @Test
    void testOnlyOneDefaultCategory() {
        // Given & When
        List<Category> defaultCategories = categoryRepository.findAll()
            .stream()
            .filter(Category::isDefault)
            .toList();
        
        // Then
        assertThat(defaultCategories).hasSize(1);
        assertThat(defaultCategories.get(0).getName()).isEqualTo("Uncategorized");
    }
    
    @Test
    void testCategoriesHaveProperDescriptions() {
        // Given & When
        Optional<Category> foodCategory = categoryRepository.findByName("Food & Dining");
        Optional<Category> salaryCategory = categoryRepository.findByName("Salary");
        
        // Then
        assertThat(foodCategory).isPresent();
        assertThat(foodCategory.get().getDescription()).isNotBlank();
        
        assertThat(salaryCategory).isPresent();
        assertThat(salaryCategory.get().getDescription()).isNotBlank();
    }
    
    @Test
    void testCategoryNamesAreUnique() {
        // Given & When
        List<Category> allCategories = categoryRepository.findAll();
        
        // Then
        long uniqueNameCount = allCategories.stream()
            .map(Category::getName)
            .distinct()
            .count();
        
        assertThat(uniqueNameCount).isEqualTo(allCategories.size());
    }
}