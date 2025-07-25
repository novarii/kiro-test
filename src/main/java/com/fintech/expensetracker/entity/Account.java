package com.fintech.expensetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a financial account (checking, savings, credit, investment)
 */
@Entity
@Table(name = "accounts")
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank(message = "Account name is required")
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Account type is required")
    private AccountType type;
    
    @Column(name = "initial_balance", precision = 19, scale = 2)
    @DecimalMin(value = "0.00", message = "Initial balance must be non-negative")
    private BigDecimal initialBalance = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();
    
    // Default constructor
    public Account() {}
    
    // Constructor with required fields
    public Account(String name, AccountType type, User user) {
        this.name = name;
        this.type = type;
        this.user = user;
    }
    
    // Constructor with all fields
    public Account(String name, AccountType type, BigDecimal initialBalance, User user) {
        this.name = name;
        this.type = type;
        this.initialBalance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    // Helper method to add transaction
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setAccount(this);
    }
    
    // Helper method to remove transaction
    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setAccount(null);
    }
    
    /**
     * Calculate current balance from initial balance plus all non-deleted transactions
     * @return current account balance
     */
    public BigDecimal getCurrentBalance() {
        BigDecimal transactionSum = transactions.stream()
                .filter(transaction -> !transaction.isDeleted())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return initialBalance.add(transactionSum);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", initialBalance=" + initialBalance +
                ", deleted=" + deleted +
                ", createdAt=" + createdAt +
                '}';
    }
}