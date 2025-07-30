package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.response.CategoryResponse;
import com.fintech.expensetracker.dto.response.MonthlySpendingSummaryResponse;
import com.fintech.expensetracker.dto.response.SpendingByCategoryResponse;
import com.fintech.expensetracker.entity.Category;
import com.fintech.expensetracker.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for financial analytics and calculations
 * Provides monthly summaries, category analysis, savings rate calculations, and trend analysis
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Calculate monthly spending summary for a specific month
     * 
     * @param userId the ID of the user
     * @param month the year-month to analyze
     * @return monthly spending summary with income, expenses, savings, and savings rate
     */
    public MonthlySpendingSummaryResponse getMonthlySpendingSummary(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        
        // Calculate total income (positive amounts)
        BigDecimal totalIncome = transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate);
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }
        
        // Calculate total expenses (negative amounts, but we want absolute value)
        BigDecimal totalExpenses = transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate);
        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }
        // Convert to positive value for display
        totalExpenses = totalExpenses.abs();
        
        // Calculate net savings (income - expenses)
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        
        // Calculate savings rate as percentage
        BigDecimal savingsRate = calculateSavingsRate(totalIncome, totalExpenses);
        
        return new MonthlySpendingSummaryResponse(month, totalIncome, totalExpenses, netSavings, savingsRate);
    }
    
    /**
     * Get spending analysis by category for a date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return spending breakdown by category with percentages
     */
    public SpendingByCategoryResponse getSpendingByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        // Get spending data by category from repository
        List<Object[]> categorySpendingData = transactionRepository.getSpendingByCategoryForUserAndDateRange(userId, startDate, endDate);
        
        // Calculate total expenses for percentage calculations
        BigDecimal totalExpenses = categorySpendingData.stream()
                .map(data -> (BigDecimal) data[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Convert to CategorySpending objects
        List<SpendingByCategoryResponse.CategorySpending> categoryBreakdown = categorySpendingData.stream()
                .map(data -> {
                    Long categoryId = (Long) data[0];
                    String categoryName = (String) data[1];
                    BigDecimal amount = (BigDecimal) data[2];
                    
                    // Calculate percentage
                    BigDecimal percentage = BigDecimal.ZERO;
                    if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = amount.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    
                    // Create category response
                    CategoryResponse categoryResponse = new CategoryResponse(categoryId, categoryName, null, false);
                    
                    // For now, we'll set transaction count to 0 as it requires additional query
                    // This can be optimized later by modifying the repository query
                    return new SpendingByCategoryResponse.CategorySpending(categoryResponse, amount, percentage, 0);
                })
                .collect(Collectors.toList());
        
        return new SpendingByCategoryResponse(startDate, endDate, totalExpenses, categoryBreakdown);
    }
    
    /**
     * Calculate savings rate for a specific date range
     * 
     * @param userId the ID of the user
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return savings rate as percentage
     */
    public BigDecimal calculateSavingsRate(Long userId, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate);
        
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        // Convert expenses to positive value
        totalExpenses = totalExpenses.abs();
        
        return calculateSavingsRate(totalIncome, totalExpenses);
    }
    
    /**
     * Get trend analysis comparing multiple months
     * 
     * @param userId the ID of the user
     * @param startDate the start date for the analysis period
     * @param endDate the end date for the analysis period
     * @return list of monthly summaries for trend analysis
     */
    public List<MonthlySpendingSummaryResponse> getTrendAnalysis(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> monthlySummaryData = transactionRepository.getMonthlySummaryForUserAndDateRange(userId, startDate, endDate);
        
        List<MonthlySpendingSummaryResponse> trendData = new ArrayList<>();
        
        for (Object[] data : monthlySummaryData) {
            Integer year = (Integer) data[0];
            Integer month = (Integer) data[1];
            BigDecimal income = (BigDecimal) data[2];
            BigDecimal expenses = (BigDecimal) data[3];
            
            if (income == null) income = BigDecimal.ZERO;
            if (expenses == null) expenses = BigDecimal.ZERO;
            
            YearMonth yearMonth = YearMonth.of(year, month);
            BigDecimal netSavings = income.subtract(expenses);
            BigDecimal savingsRate = calculateSavingsRate(income, expenses);
            
            trendData.add(new MonthlySpendingSummaryResponse(yearMonth, income, expenses, netSavings, savingsRate));
        }
        
        // Fill in missing months with zero values if needed
        return fillMissingMonths(trendData, startDate, endDate);
    }
    
    /**
     * Helper method to calculate savings rate percentage
     * 
     * @param totalIncome total income amount
     * @param totalExpenses total expenses amount (should be positive)
     * @return savings rate as percentage (0-100)
     */
    private BigDecimal calculateSavingsRate(BigDecimal totalIncome, BigDecimal totalExpenses) {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        return netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Helper method to fill in missing months with zero values for complete trend analysis
     * 
     * @param existingData existing monthly data
     * @param startDate start date of the period
     * @param endDate end date of the period
     * @return complete list with missing months filled with zero values
     */
    private List<MonthlySpendingSummaryResponse> fillMissingMonths(List<MonthlySpendingSummaryResponse> existingData, 
                                                                  LocalDate startDate, LocalDate endDate) {
        List<MonthlySpendingSummaryResponse> completeData = new ArrayList<>();
        
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        
        YearMonth current = start;
        while (!current.isAfter(end)) {
            final YearMonth currentMonth = current;
            
            // Find existing data for this month
            MonthlySpendingSummaryResponse existingMonth = existingData.stream()
                    .filter(data -> data.getMonth().equals(currentMonth))
                    .findFirst()
                    .orElse(null);
            
            if (existingMonth != null) {
                completeData.add(existingMonth);
            } else {
                // Create zero-value entry for missing month
                completeData.add(new MonthlySpendingSummaryResponse(
                        currentMonth, 
                        BigDecimal.ZERO, 
                        BigDecimal.ZERO, 
                        BigDecimal.ZERO, 
                        BigDecimal.ZERO
                ));
            }
            
            current = current.plusMonths(1);
        }
        
        return completeData;
    }
}