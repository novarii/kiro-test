package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.response.MonthlySpendingSummaryResponse;
import com.fintech.expensetracker.dto.response.SpendingByCategoryResponse;
import com.fintech.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private AnalyticsService analyticsService;
    
    private Long userId;
    private YearMonth testMonth;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @BeforeEach
    void setUp() {
        userId = 1L;
        testMonth = YearMonth.of(2024, 1);
        startDate = testMonth.atDay(1);
        endDate = testMonth.atEndOfMonth();
    }
    
    @Test
    void getMonthlySpendingSummary_WithIncomeAndExpenses_ReturnsCorrectSummary() {
        // Given
        BigDecimal income = new BigDecimal("5000.00");
        BigDecimal expenses = new BigDecimal("-2000.00"); // Negative in database
        
        when(transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(income);
        when(transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(expenses);
        
        // When
        MonthlySpendingSummaryResponse result = analyticsService.getMonthlySpendingSummary(userId, testMonth);
        
        // Then
        assertThat(result.getMonth()).isEqualTo(testMonth);
        assertThat(result.getTotalIncome()).isEqualByComparingTo(income);
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("2000.00")); // Converted to positive
        assertThat(result.getNetSavings()).isEqualByComparingTo(new BigDecimal("3000.00")); // 5000 - 2000
        assertThat(result.getSavingsRate()).isEqualByComparingTo(new BigDecimal("60.00")); // (3000/5000) * 100
    }
    
    @Test
    void getMonthlySpendingSummary_WithNoIncome_ReturnsZeroSavingsRate() {
        // Given
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = new BigDecimal("-1000.00");
        
        when(transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(income);
        when(transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(expenses);
        
        // When
        MonthlySpendingSummaryResponse result = analyticsService.getMonthlySpendingSummary(userId, testMonth);
        
        // Then
        assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getNetSavings()).isEqualByComparingTo(new BigDecimal("-1000.00"));
        assertThat(result.getSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    @Test
    void getMonthlySpendingSummary_WithNullValues_HandlesGracefully() {
        // Given
        when(transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(null);
        when(transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(null);
        
        // When
        MonthlySpendingSummaryResponse result = analyticsService.getMonthlySpendingSummary(userId, testMonth);
        
        // Then
        assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getNetSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    @Test
    void getSpendingByCategory_WithMultipleCategories_ReturnsCorrectBreakdown() {
        // Given
        Object[] category1Data = {1L, "Food", new BigDecimal("800.00")};
        Object[] category2Data = {2L, "Transportation", new BigDecimal("200.00")};
        List<Object[]> categoryData = new ArrayList<>();
        categoryData.add(category1Data);
        categoryData.add(category2Data);
        
        when(transactionRepository.getSpendingByCategoryForUserAndDateRange(userId, startDate, endDate))
                .thenReturn(categoryData);
        
        // When
        SpendingByCategoryResponse result = analyticsService.getSpendingByCategory(userId, startDate, endDate);
        
        // Then
        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getCategoryBreakdown()).hasSize(2);
        
        SpendingByCategoryResponse.CategorySpending foodSpending = result.getCategoryBreakdown().get(0);
        assertThat(foodSpending.getCategory().getName()).isEqualTo("Food");
        assertThat(foodSpending.getAmount()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(foodSpending.getPercentage()).isEqualByComparingTo(new BigDecimal("80.00"));
        
        SpendingByCategoryResponse.CategorySpending transportSpending = result.getCategoryBreakdown().get(1);
        assertThat(transportSpending.getCategory().getName()).isEqualTo("Transportation");
        assertThat(transportSpending.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(transportSpending.getPercentage()).isEqualByComparingTo(new BigDecimal("20.00"));
    }
    
    @Test
    void getSpendingByCategory_WithNoExpenses_ReturnsEmptyBreakdown() {
        // Given
        when(transactionRepository.getSpendingByCategoryForUserAndDateRange(userId, startDate, endDate))
                .thenReturn(Collections.emptyList());
        
        // When
        SpendingByCategoryResponse result = analyticsService.getSpendingByCategory(userId, startDate, endDate);
        
        // Then
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getCategoryBreakdown()).isEmpty();
    }
    
    @Test
    void calculateSavingsRate_WithPositiveIncome_ReturnsCorrectRate() {
        // Given
        BigDecimal income = new BigDecimal("4000.00");
        BigDecimal expenses = new BigDecimal("-1000.00");
        
        when(transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(income);
        when(transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(expenses);
        
        // When
        BigDecimal result = analyticsService.calculateSavingsRate(userId, startDate, endDate);
        
        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("75.00")); // (3000/4000) * 100
    }
    
    @Test
    void calculateSavingsRate_WithZeroIncome_ReturnsZero() {
        // Given
        when(transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(new BigDecimal("-500.00"));
        
        // When
        BigDecimal result = analyticsService.calculateSavingsRate(userId, startDate, endDate);
        
        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    @Test
    void getTrendAnalysis_WithMultipleMonths_ReturnsCompleteData() {
        // Given
        LocalDate trendStart = LocalDate.of(2024, 1, 1);
        LocalDate trendEnd = LocalDate.of(2024, 3, 31);
        
        Object[] jan2024 = {2024, 1, new BigDecimal("3000.00"), new BigDecimal("1500.00")};
        Object[] mar2024 = {2024, 3, new BigDecimal("3500.00"), new BigDecimal("2000.00")};
        List<Object[]> monthlyData = new ArrayList<>();
        monthlyData.add(jan2024);
        monthlyData.add(mar2024);
        
        when(transactionRepository.getMonthlySummaryForUserAndDateRange(userId, trendStart, trendEnd))
                .thenReturn(monthlyData);
        
        // When
        List<MonthlySpendingSummaryResponse> result = analyticsService.getTrendAnalysis(userId, trendStart, trendEnd);
        
        // Then
        assertThat(result).hasSize(3); // Jan, Feb (filled), Mar
        
        // January data
        MonthlySpendingSummaryResponse jan = result.get(0);
        assertThat(jan.getMonth()).isEqualTo(YearMonth.of(2024, 1));
        assertThat(jan.getTotalIncome()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(jan.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(jan.getNetSavings()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(jan.getSavingsRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        
        // February (missing month, should be filled with zeros)
        MonthlySpendingSummaryResponse feb = result.get(1);
        assertThat(feb.getMonth()).isEqualTo(YearMonth.of(2024, 2));
        assertThat(feb.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.getNetSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.getSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // March data
        MonthlySpendingSummaryResponse mar = result.get(2);
        assertThat(mar.getMonth()).isEqualTo(YearMonth.of(2024, 3));
        assertThat(mar.getTotalIncome()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(mar.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(mar.getNetSavings()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(mar.getSavingsRate()).isEqualByComparingTo(new BigDecimal("42.86"));
    }
    
    @Test
    void getTrendAnalysis_WithNoData_ReturnsZeroFilledMonths() {
        // Given
        LocalDate trendStart = LocalDate.of(2024, 1, 1);
        LocalDate trendEnd = LocalDate.of(2024, 2, 29);
        
        when(transactionRepository.getMonthlySummaryForUserAndDateRange(userId, trendStart, trendEnd))
                .thenReturn(Collections.emptyList());
        
        // When
        List<MonthlySpendingSummaryResponse> result = analyticsService.getTrendAnalysis(userId, trendStart, trendEnd);
        
        // Then
        assertThat(result).hasSize(2); // Jan and Feb
        
        result.forEach(monthData -> {
            assertThat(monthData.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(monthData.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(monthData.getNetSavings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(monthData.getSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }
    
    @Test
    void getTrendAnalysis_WithNullValues_HandlesGracefully() {
        // Given
        LocalDate trendStart = LocalDate.of(2024, 1, 1);
        LocalDate trendEnd = LocalDate.of(2024, 1, 31);
        
        Object[] janDataWithNulls = {2024, 1, null, null};
        List<Object[]> monthlyData = new ArrayList<>();
        monthlyData.add(janDataWithNulls);
        
        when(transactionRepository.getMonthlySummaryForUserAndDateRange(userId, trendStart, trendEnd))
                .thenReturn(monthlyData);
        
        // When
        List<MonthlySpendingSummaryResponse> result = analyticsService.getTrendAnalysis(userId, trendStart, trendEnd);
        
        // Then
        assertThat(result).hasSize(1);
        MonthlySpendingSummaryResponse jan = result.get(0);
        assertThat(jan.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jan.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jan.getNetSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jan.getSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    @Test
    void getSpendingByCategory_WithSingleCategory_CalculatesHundredPercent() {
        // Given
        Object[] singleCategoryData = {1L, "Food", new BigDecimal("500.00")};
        List<Object[]> categoryData = new ArrayList<>();
        categoryData.add(singleCategoryData);
        
        when(transactionRepository.getSpendingByCategoryForUserAndDateRange(userId, startDate, endDate))
                .thenReturn(categoryData);
        
        // When
        SpendingByCategoryResponse result = analyticsService.getSpendingByCategory(userId, startDate, endDate);
        
        // Then
        assertThat(result.getCategoryBreakdown()).hasSize(1);
        SpendingByCategoryResponse.CategorySpending spending = result.getCategoryBreakdown().get(0);
        assertThat(spending.getPercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
    
    @Test
    void calculateSavingsRate_WithExpensesExceedingIncome_ReturnsNegativeRate() {
        // Given
        BigDecimal income = new BigDecimal("1000.00");
        BigDecimal expenses = new BigDecimal("-1500.00");
        
        when(transactionRepository.calculateTotalIncomeByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(income);
        when(transactionRepository.calculateTotalExpensesByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(expenses);
        
        // When
        BigDecimal result = analyticsService.calculateSavingsRate(userId, startDate, endDate);
        
        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("-50.00")); // (1000-1500)/1000 * 100
    }
}