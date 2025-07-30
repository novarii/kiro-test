package com.fintech.expensetracker.dto.request;

import com.fintech.expensetracker.dto.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for filtering transaction requests
 */
public class TransactionFilter {
    
    private Long accountId;
    private Long categoryId;
    private TransactionType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String description; // For partial description matching
    
    // Default constructor
    public TransactionFilter() {}
    
    // Constructor with date range
    public TransactionFilter(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Constructor with all fields
    public TransactionFilter(Long accountId, Long categoryId, TransactionType type, 
                           LocalDate startDate, LocalDate endDate, 
                           BigDecimal minAmount, BigDecimal maxAmount, String description) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
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
    
    public BigDecimal getMinAmount() {
        return minAmount;
    }
    
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }
    
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
    
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Check if any filter criteria is set
     * @return true if at least one filter is applied, false otherwise
     */
    public boolean hasFilters() {
        return accountId != null || categoryId != null || type != null ||
               startDate != null || endDate != null ||
               minAmount != null || maxAmount != null ||
               (description != null && !description.trim().isEmpty());
    }
    
    @Override
    public String toString() {
        return "TransactionFilter{" +
                "accountId=" + accountId +
                ", categoryId=" + categoryId +
                ", type=" + type +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                ", description='" + description + '\'' +
                '}';
    }
}