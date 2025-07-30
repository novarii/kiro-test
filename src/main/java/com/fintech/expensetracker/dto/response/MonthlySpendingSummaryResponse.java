package com.fintech.expensetracker.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * DTO for monthly spending summary response
 */
public class MonthlySpendingSummaryResponse {
    
    private YearMonth month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private BigDecimal savingsRate; // Percentage
    
    // Default constructor
    public MonthlySpendingSummaryResponse() {}
    
    // Constructor with all fields
    public MonthlySpendingSummaryResponse(YearMonth month, BigDecimal totalIncome, BigDecimal totalExpenses, 
                                         BigDecimal netSavings, BigDecimal savingsRate) {
        this.month = month;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netSavings = netSavings;
        this.savingsRate = savingsRate;
    }
    
    // Getters and Setters
    public YearMonth getMonth() {
        return month;
    }
    
    public void setMonth(YearMonth month) {
        this.month = month;
    }
    
    public BigDecimal getTotalIncome() {
        return totalIncome;
    }
    
    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }
    
    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }
    
    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }
    
    public BigDecimal getNetSavings() {
        return netSavings;
    }
    
    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }
    
    public BigDecimal getSavingsRate() {
        return savingsRate;
    }
    
    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }
    
    @Override
    public String toString() {
        return "MonthlySpendingSummaryResponse{" +
                "month=" + month +
                ", totalIncome=" + totalIncome +
                ", totalExpenses=" + totalExpenses +
                ", netSavings=" + netSavings +
                ", savingsRate=" + savingsRate +
                '}';
    }
}