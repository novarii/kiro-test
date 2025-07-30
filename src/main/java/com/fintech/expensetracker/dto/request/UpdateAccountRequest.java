package com.fintech.expensetracker.dto.request;

import com.fintech.expensetracker.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * DTO for updating account requests
 */
public class UpdateAccountRequest {
    
    private String name;
    private AccountType type;
    
    @DecimalMin(value = "0.00", message = "Initial balance must be non-negative")
    private BigDecimal initialBalance;
    
    // Default constructor
    public UpdateAccountRequest() {}
    
    // Constructor with all fields
    public UpdateAccountRequest(String name, AccountType type, BigDecimal initialBalance) {
        this.name = name;
        this.type = type;
        this.initialBalance = initialBalance;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public AccountType getType() {
        return type;
    }
    
    public void setType(AccountType type) {
        this.type = type;
    }
    
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
    
    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
    
    @Override
    public String toString() {
        return "UpdateAccountRequest{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", initialBalance=" + initialBalance +
                '}';
    }
}