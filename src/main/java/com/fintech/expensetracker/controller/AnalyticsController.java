package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.response.MonthlySpendingSummaryResponse;
import com.fintech.expensetracker.dto.response.SpendingByCategoryResponse;
import com.fintech.expensetracker.security.UserPrincipal;
import com.fintech.expensetracker.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * REST controller for financial analytics operations
 * Provides endpoints for spending summaries, category analysis, savings rate calculations, and trend analysis
 */
@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnalyticsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    
    private final AnalyticsService analyticsService;
    
    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    /**
     * Get monthly spending summary for a specific month
     * 
     * @param month the year-month in format YYYY-MM (e.g., 2024-01)
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing monthly spending summary
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySpendingSummaryResponse> getMonthlySpendingSummary(
            @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Retrieving monthly spending summary for user ID: {} and month: {}", 
                   userPrincipal.getId(), month);
        
        MonthlySpendingSummaryResponse summary = analyticsService.getMonthlySpendingSummary(
                userPrincipal.getId(), month);
        
        logger.info("Retrieved monthly spending summary for user ID: {} and month: {} - Income: {}, Expenses: {}, Savings: {}", 
                   userPrincipal.getId(), month, summary.getTotalIncome(), 
                   summary.getTotalExpenses(), summary.getNetSavings());
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get spending analysis by category for a date range
     * 
     * @param startDate the start date (inclusive) in format YYYY-MM-DD
     * @param endDate the end date (inclusive) in format YYYY-MM-DD
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing spending breakdown by category
     */
    @GetMapping("/spending-by-category")
    public ResponseEntity<SpendingByCategoryResponse> getSpendingByCategory(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        logger.info("Retrieving spending by category for user ID: {} from {} to {}", 
                   userPrincipal.getId(), startDate, endDate);
        
        SpendingByCategoryResponse categorySpending = analyticsService.getSpendingByCategory(
                userPrincipal.getId(), startDate, endDate);
        
        logger.info("Retrieved spending by category for user ID: {} - Total expenses: {}, Categories: {}", 
                   userPrincipal.getId(), categorySpending.getTotalExpenses(), 
                   categorySpending.getCategoryBreakdown().size());
        
        return ResponseEntity.ok(categorySpending);
    }
    
    /**
     * Calculate savings rate for a specific date range
     * 
     * @param startDate the start date (inclusive) in format YYYY-MM-DD
     * @param endDate the end date (inclusive) in format YYYY-MM-DD
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing savings rate as percentage
     */
    @GetMapping("/savings-rate")
    public ResponseEntity<SavingsRateResponse> getSavingsRate(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        logger.info("Calculating savings rate for user ID: {} from {} to {}", 
                   userPrincipal.getId(), startDate, endDate);
        
        BigDecimal savingsRate = analyticsService.calculateSavingsRate(
                userPrincipal.getId(), startDate, endDate);
        
        SavingsRateResponse response = new SavingsRateResponse(startDate, endDate, savingsRate);
        
        logger.info("Calculated savings rate for user ID: {} from {} to {}: {}%", 
                   userPrincipal.getId(), startDate, endDate, savingsRate);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get trend analysis comparing multiple months
     * 
     * @param startDate the start date for the analysis period in format YYYY-MM-DD
     * @param endDate the end date for the analysis period in format YYYY-MM-DD
     * @param userPrincipal the authenticated user
     * @return ResponseEntity containing list of monthly summaries for trend analysis
     */
    @GetMapping("/trends")
    public ResponseEntity<TrendAnalysisResponse> getTrendAnalysis(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        logger.info("Retrieving trend analysis for user ID: {} from {} to {}", 
                   userPrincipal.getId(), startDate, endDate);
        
        List<MonthlySpendingSummaryResponse> trendData = analyticsService.getTrendAnalysis(
                userPrincipal.getId(), startDate, endDate);
        
        TrendAnalysisResponse response = new TrendAnalysisResponse(startDate, endDate, trendData);
        
        logger.info("Retrieved trend analysis for user ID: {} from {} to {} - {} months of data", 
                   userPrincipal.getId(), startDate, endDate, trendData.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Inner class for savings rate response
     */
    public static class SavingsRateResponse {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal savingsRate;
        
        public SavingsRateResponse(LocalDate startDate, LocalDate endDate, BigDecimal savingsRate) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.savingsRate = savingsRate;
        }
        
        public LocalDate getStartDate() {
            return startDate;
        }
        
        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }
        
        public LocalDate getEndDate() {
            return endDate;
        }
        
        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
        
        public BigDecimal getSavingsRate() {
            return savingsRate;
        }
        
        public void setSavingsRate(BigDecimal savingsRate) {
            this.savingsRate = savingsRate;
        }
    }
    
    /**
     * Inner class for trend analysis response
     */
    public static class TrendAnalysisResponse {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<MonthlySpendingSummaryResponse> monthlyData;
        
        public TrendAnalysisResponse(LocalDate startDate, LocalDate endDate, 
                                   List<MonthlySpendingSummaryResponse> monthlyData) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.monthlyData = monthlyData;
        }
        
        public LocalDate getStartDate() {
            return startDate;
        }
        
        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }
        
        public LocalDate getEndDate() {
            return endDate;
        }
        
        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
        
        public List<MonthlySpendingSummaryResponse> getMonthlyData() {
            return monthlyData;
        }
        
        public void setMonthlyData(List<MonthlySpendingSummaryResponse> monthlyData) {
            this.monthlyData = monthlyData;
        }
    }
}