package com.fintech.expensetracker.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for spending by category analysis response
 */
public class SpendingByCategoryResponse {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalExpenses;
    private List<CategorySpending> categoryBreakdown;
    
    // Default constructor
    public SpendingByCategoryResponse() {}
    
    // Constructor with all fields
    public SpendingByCategoryResponse(LocalDate startDate, LocalDate endDate, BigDecimal totalExpenses, 
                                     List<CategorySpending> categoryBreakdown) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalExpenses = totalExpenses;
        this.categoryBreakdown = categoryBreakdown;
    }
    
    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }
    
    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }
    
    public List<CategorySpending> getCategoryBreakdown() {
        return categoryBreakdown;
    }
    
    public void setCategoryBreakdown(List<CategorySpending> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }
    
    @Override
    public String toString() {
        return "SpendingByCategoryResponse{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalExpenses=" + totalExpenses +
                ", categoryBreakdown=" + categoryBreakdown +
                '}';
    }
    
    /**
     * Inner class representing spending for a specific category
     */
    public static class CategorySpending {
        private CategoryResponse category;
        private BigDecimal amount;
        private BigDecimal percentage;
        private int transactionCount;
        
        // Default constructor
        public CategorySpending() {}
        
        // Constructor with all fields
        public CategorySpending(CategoryResponse category, BigDecimal amount, BigDecimal percentage, int transactionCount) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
            this.transactionCount = transactionCount;
        }
        
        // Getters and Setters
        public CategoryResponse getCategory() {
            return category;
        }
        
        public void setCategory(CategoryResponse category) {
            this.category = category;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
        
        public BigDecimal getPercentage() {
            return percentage;
        }
        
        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
        
        public int getTransactionCount() {
            return transactionCount;
        }
        
        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }
        
        @Override
        public String toString() {
            return "CategorySpending{" +
                    "category=" + category +
                    ", amount=" + amount +
                    ", percentage=" + percentage +
                    ", transactionCount=" + transactionCount +
                    '}';
        }
    }
}