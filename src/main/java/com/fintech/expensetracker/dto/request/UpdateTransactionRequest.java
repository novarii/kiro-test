package com.fintech.expensetracker.dto.request;

import com.fintech.expensetracker.dto.TransactionType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating transaction requests
 */
public class UpdateTransactionRequest {
    
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String description;
    private LocalDate transactionDate;
    private Long accountId;
    private Long categoryId;
    private TransactionType type; // INCOME or EXPENSE
    
    // Default constructor
    public UpdateTransactionRequest() {}
    
    // Constructor with all fields
    public UpdateTransactionRequest(BigDecimal amount, String description, LocalDate transactionDate, 
                                   Long accountId, Long categoryId, TransactionType type) {
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
    }
    
    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
    
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
    
    @Override
    public String toString() {
        return "UpdateTransactionRequest{" +
                "amount=" + amount +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", accountId=" + accountId +
                ", categoryId=" + categoryId +
                ", type=" + type +
                '}';
    }
}