package com.fintech.expensetracker.dto.request;

import com.fintech.expensetracker.dto.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating new transaction requests
 */
public class CreateTransactionRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private LocalDate transactionDate; // Defaults to today if not provided
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    private Long categoryId; // Defaults to "Uncategorized" if not provided
    
    @NotNull(message = "Transaction type is required")
    private TransactionType type; // INCOME or EXPENSE
    
    // Default constructor
    public CreateTransactionRequest() {}
    
    // Constructor with required fields
    public CreateTransactionRequest(BigDecimal amount, String description, Long accountId, TransactionType type) {
        this.amount = amount;
        this.description = description;
        this.accountId = accountId;
        this.type = type;
        this.transactionDate = LocalDate.now();
    }
    
    // Constructor with all fields
    public CreateTransactionRequest(BigDecimal amount, String description, LocalDate transactionDate, 
                                   Long accountId, Long categoryId, TransactionType type) {
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate != null ? transactionDate : LocalDate.now();
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
        return "CreateTransactionRequest{" +
                "amount=" + amount +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", accountId=" + accountId +
                ", categoryId=" + categoryId +
                ", type=" + type +
                '}';
    }
}