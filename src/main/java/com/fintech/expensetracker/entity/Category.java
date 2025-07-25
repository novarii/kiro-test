package com.fintech.expensetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a transaction category
 */
@Entity
@Table(name = "categories")
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank(message = "Category name is required")
    private String name;
    
    @Column
    private String description;
    
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false; // For "Uncategorized"
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();
    
    // Default constructor
    public Category() {}
    
    // Constructor with required fields
    public Category(String name) {
        this.name = name;
    }
    
    // Constructor with all fields
    public Category(String name, String description, boolean isDefault) {
        this.name = name;
        this.description = description;
        this.isDefault = isDefault;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
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
        transaction.setCategory(this);
    }
    
    // Helper method to remove transaction
    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setCategory(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return id != null && id.equals(category.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}