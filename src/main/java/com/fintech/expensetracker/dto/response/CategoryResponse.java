package com.fintech.expensetracker.dto.response;

/**
 * DTO for category response data
 */
public class CategoryResponse {
    
    private Long id;
    private String name;
    private String description;
    private boolean isDefault;
    
    // Default constructor
    public CategoryResponse() {}
    
    // Constructor with all fields
    public CategoryResponse(Long id, String name, String description, boolean isDefault) {
        this.id = id;
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
    
    @Override
    public String toString() {
        return "CategoryResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}