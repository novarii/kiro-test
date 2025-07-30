package com.fintech.expensetracker.dto.request;

import com.fintech.expensetracker.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for creating new account requests
 */
public class CreateAccountRequest {
    
    @NotBlank(message = "Account name is required")
    private String name;
    
    @NotNull(message = "Account type is required")
    private AccountType type;
    
    @DecimalMin(value = "0.00", message = "Initial balance must be non-negative")
    private BigDecimal initialBalance = BigDecimal.ZERO;
    
    // Default constructor
    public CreateAccountRequest() {}
    
    // Constructor with required fields
    public CreateAccountRequest(String name, AccountType type) {
        this.name = name;
        this.type = type;
    }
    
    // Constructor with all fields
    public CreateAccountRequest(String name, AccountType type, BigDecimal initialBalance) {
        this.name = name;
        this.type = type;
        this.initialBalance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
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
        this.initialBalance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return "CreateAccountRequest{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", initialBalance=" + initialBalance +
                '}';
    }
}