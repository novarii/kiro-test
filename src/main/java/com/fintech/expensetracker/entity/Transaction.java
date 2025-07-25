package com.fintech.expensetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a financial transaction (income or expense)
 */
@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount; // Positive for income, negative for expenses
    
    @Column(nullable = false)
    @NotBlank(message = "Description is required")
    private String description;
    
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Category is required")
    private Category category;
    
    // Default constructor
    public Transaction() {}
    
    // Constructor with required fields
    public Transaction(BigDecimal amount, String description, LocalDate transactionDate, Account account, Category category) {
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate != null ? transactionDate : LocalDate.now();
        this.account = account;
        this.category = category;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
        this.transactionDate = transactionDate != null ? transactionDate : LocalDate.now();
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
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    /**
     * Determines if this transaction is an income (positive amount) or expense (negative amount)
     * @return true if income, false if expense
     */
    public boolean isIncome() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Determines if this transaction is an expense (negative amount)
     * @return true if expense, false if income
     */
    public boolean isExpense() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Get the absolute value of the transaction amount
     * @return absolute amount
     */
    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return id != null && id.equals(that.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", deleted=" + deleted +
                ", createdAt=" + createdAt +
                '}';
    }
}