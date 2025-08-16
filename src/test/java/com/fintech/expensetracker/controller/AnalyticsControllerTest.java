package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.response.CategoryResponse;
import com.fintech.expensetracker.dto.response.MonthlySpendingSummaryResponse;
import com.fintech.expensetracker.dto.response.SpendingByCategoryResponse;
import com.fintech.expensetracker.security.UserPrincipal;
import com.fintech.expensetracker.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsController with security context
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {
    
    @Mock
    private AnalyticsService analyticsService;
    
    @InjectMocks
    private AnalyticsController analyticsController;
    
    private UserPrincipal userPrincipal;
    private MonthlySpendingSummaryResponse monthlySummary;
    private SpendingByCategoryResponse categorySpending;
    private List<MonthlySpendingSummaryResponse> trendData;
    
    @BeforeEach
    void setUp() {
        // Setup test user principal
        userPrincipal = new UserPrincipal(1L, "Test User", "test@example.com", "password");
        
        // Setup test monthly summary
        monthlySummary = new MonthlySpendingSummaryResponse(
                YearMonth.of(2024, 1),
                new BigDecimal("5000.00"), // income
                new BigDecimal("3500.00"), // expenses
                new BigDecimal("1500.00"), // net savings
                new BigDecimal("30.00")    // savings rate
        );
        
        // Setup test category spending
        CategoryResponse foodCategory = new CategoryResponse(1L, "Food", "Food and dining", false);
        CategoryResponse transportCategory = new CategoryResponse(2L, "Transport", "Transportation costs", false);
        
        List<SpendingByCategoryResponse.CategorySpending> categoryBreakdown = Arrays.asList(
                new SpendingByCategoryResponse.CategorySpending(
                        foodCategory, new BigDecimal("1500.00"), new BigDecimal("42.86"), 15),
                new SpendingByCategoryResponse.CategorySpending(
                        transportCategory, new BigDecimal("2000.00"), new BigDecimal("57.14"), 8)
        );
        
        categorySpending = new SpendingByCategoryResponse(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                new BigDecimal("3500.00"),
                categoryBreakdown
        );
        
        // Setup test trend data
        trendData = Arrays.asList(
                new MonthlySpendingSummaryResponse(
                        YearMonth.of(2024, 1),
                        new BigDecimal("5000.00"),
                        new BigDecimal("3500.00"),
                        new BigDecimal("1500.00"),
                        new BigDecimal("30.00")
                ),
                new MonthlySpendingSummaryResponse(
                        YearMonth.of(2024, 2),
                        new BigDecimal("5200.00"),
                        new BigDecimal("3800.00"),
                        new BigDecimal("1400.00"),
                        new BigDecimal("26.92")
                )
        );
    }
    
    @Test
    void getMonthlySpendingSummary_ValidMonth_ReturnsMonthlySpendingSummary() {
        // Given
        YearMonth month = YearMonth.of(2024, 1);
        when(analyticsService.getMonthlySpendingSummary(1L, month)).thenReturn(monthlySummary);
        
        // When
        ResponseEntity<MonthlySpendingSummaryResponse> response = 
                analyticsController.getMonthlySpendingSummary(month, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(YearMonth.of(2024, 1), response.getBody().getMonth());
        assertEquals(new BigDecimal("5000.00"), response.getBody().getTotalIncome());
        assertEquals(new BigDecimal("3500.00"), response.getBody().getTotalExpenses());
        assertEquals(new BigDecimal("1500.00"), response.getBody().getNetSavings());
        assertEquals(new BigDecimal("30.00"), response.getBody().getSavingsRate());
        verify(analyticsService).getMonthlySpendingSummary(1L, month);
    }
    
    @Test
    void getMonthlySpendingSummary_NoTransactionsInMonth_ReturnsZeroValues() {
        // Given
        YearMonth month = YearMonth.of(2024, 12);
        MonthlySpendingSummaryResponse emptyMonth = new MonthlySpendingSummaryResponse(
                month, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(analyticsService.getMonthlySpendingSummary(1L, month)).thenReturn(emptyMonth);
        
        // When
        ResponseEntity<MonthlySpendingSummaryResponse> response = 
                analyticsController.getMonthlySpendingSummary(month, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(month, response.getBody().getMonth());
        assertEquals(BigDecimal.ZERO, response.getBody().getTotalIncome());
        assertEquals(BigDecimal.ZERO, response.getBody().getTotalExpenses());
        assertEquals(BigDecimal.ZERO, response.getBody().getNetSavings());
        assertEquals(BigDecimal.ZERO, response.getBody().getSavingsRate());
        verify(analyticsService).getMonthlySpendingSummary(1L, month);
    }
    
    @Test
    void getSpendingByCategory_ValidDateRange_ReturnsSpendingByCategory() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        when(analyticsService.getSpendingByCategory(1L, startDate, endDate)).thenReturn(categorySpending);
        
        // When
        ResponseEntity<SpendingByCategoryResponse> response = 
                analyticsController.getSpendingByCategory(startDate, endDate, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(startDate, response.getBody().getStartDate());
        assertEquals(endDate, response.getBody().getEndDate());
        assertEquals(new BigDecimal("3500.00"), response.getBody().getTotalExpenses());
        assertEquals(2, response.getBody().getCategoryBreakdown().size());
        
        // Verify first category
        SpendingByCategoryResponse.CategorySpending firstCategory = response.getBody().getCategoryBreakdown().get(0);
        assertEquals("Food", firstCategory.getCategory().getName());
        assertEquals(new BigDecimal("1500.00"), firstCategory.getAmount());
        assertEquals(new BigDecimal("42.86"), firstCategory.getPercentage());
        
        verify(analyticsService).getSpendingByCategory(1L, startDate, endDate);
    }
    
    @Test
    void getSpendingByCategory_InvalidDateRange_ThrowsIllegalArgumentException() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1); // End date before start date
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            analyticsController.getSpendingByCategory(startDate, endDate, userPrincipal);
        });
        
        assertEquals("Start date must be before or equal to end date", exception.getMessage());
        verify(analyticsService, never()).getSpendingByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }
    
    @Test
    void getSpendingByCategory_SameDateRange_ReturnsSpendingByCategory() {
        // Given
        LocalDate sameDate = LocalDate.of(2024, 1, 15);
        when(analyticsService.getSpendingByCategory(1L, sameDate, sameDate)).thenReturn(categorySpending);
        
        // When
        ResponseEntity<SpendingByCategoryResponse> response = 
                analyticsController.getSpendingByCategory(sameDate, sameDate, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(analyticsService).getSpendingByCategory(1L, sameDate, sameDate);
    }
    
    @Test
    void getSavingsRate_ValidDateRange_ReturnsSavingsRate() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        BigDecimal savingsRate = new BigDecimal("30.00");
        when(analyticsService.calculateSavingsRate(1L, startDate, endDate)).thenReturn(savingsRate);
        
        // When
        ResponseEntity<AnalyticsController.SavingsRateResponse> response = 
                analyticsController.getSavingsRate(startDate, endDate, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(startDate, response.getBody().getStartDate());
        assertEquals(endDate, response.getBody().getEndDate());
        assertEquals(savingsRate, response.getBody().getSavingsRate());
        verify(analyticsService).calculateSavingsRate(1L, startDate, endDate);
    }
    
    @Test
    void getSavingsRate_InvalidDateRange_ThrowsIllegalArgumentException() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1); // End date before start date
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            analyticsController.getSavingsRate(startDate, endDate, userPrincipal);
        });
        
        assertEquals("Start date must be before or equal to end date", exception.getMessage());
        verify(analyticsService, never()).calculateSavingsRate(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }
    
    @Test
    void getSavingsRate_NoIncomeInPeriod_ReturnsZeroSavingsRate() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        BigDecimal zeroSavingsRate = BigDecimal.ZERO;
        when(analyticsService.calculateSavingsRate(1L, startDate, endDate)).thenReturn(zeroSavingsRate);
        
        // When
        ResponseEntity<AnalyticsController.SavingsRateResponse> response = 
                analyticsController.getSavingsRate(startDate, endDate, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BigDecimal.ZERO, response.getBody().getSavingsRate());
        verify(analyticsService).calculateSavingsRate(1L, startDate, endDate);
    }
    
    @Test
    void getTrendAnalysis_ValidDateRange_ReturnsTrendAnalysis() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);
        when(analyticsService.getTrendAnalysis(1L, startDate, endDate)).thenReturn(trendData);
        
        // When
        ResponseEntity<AnalyticsController.TrendAnalysisResponse> response = 
                analyticsController.getTrendAnalysis(startDate, endDate, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(startDate, response.getBody().getStartDate());
        assertEquals(endDate, response.getBody().getEndDate());
        assertEquals(2, response.getBody().getMonthlyData().size());
        
        // Verify first month data
        MonthlySpendingSummaryResponse firstMonth = response.getBody().getMonthlyData().get(0);
        assertEquals(YearMonth.of(2024, 1), firstMonth.getMonth());
        assertEquals(new BigDecimal("5000.00"), firstMonth.getTotalIncome());
        assertEquals(new BigDecimal("3500.00"), firstMonth.getTotalExpenses());
        
        // Verify second month data
        MonthlySpendingSummaryResponse secondMonth = response.getBody().getMonthlyData().get(1);
        assertEquals(YearMonth.of(2024, 2), secondMonth.getMonth());
        assertEquals(new BigDecimal("5200.00"), secondMonth.getTotalIncome());
        assertEquals(new BigDecimal("3800.00"), secondMonth.getTotalExpenses());
        
        verify(analyticsService).getTrendAnalysis(1L, startDate, endDate);
    }
    
    @Test
    void getTrendAnalysis_InvalidDateRange_ThrowsIllegalArgumentException() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31); // End date before start date
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            analyticsController.getTrendAnalysis(startDate, endDate, userPrincipal);
        });
        
        assertEquals("Start date must be before or equal to end date", exception.getMessage());
        verify(analyticsService, never()).getTrendAnalysis(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }
    
    @Test
    void getTrendAnalysis_SingleMonth_ReturnsSingleMonthTrend() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        List<MonthlySpendingSummaryResponse> singleMonthData = Arrays.asList(trendData.get(0));
        when(analyticsService.getTrendAnalysis(1L, startDate, endDate)).thenReturn(singleMonthData);
        
        // When
        ResponseEntity<AnalyticsController.TrendAnalysisResponse> response = 
                analyticsController.getTrendAnalysis(startDate, endDate, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getMonthlyData().size());
        verify(analyticsService).getTrendAnalysis(1L, startDate, endDate);
    }
    
    @Test
    void getMonthlySpendingSummary_ServiceThrowsException_ExceptionPropagated() {
        // Given
        YearMonth month = YearMonth.of(2024, 1);
        when(analyticsService.getMonthlySpendingSummary(1L, month))
                .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            analyticsController.getMonthlySpendingSummary(month, userPrincipal);
        });
        
        assertEquals("Database error", exception.getMessage());
        verify(analyticsService).getMonthlySpendingSummary(1L, month);
    }
    
    @Test
    void getSpendingByCategory_ServiceThrowsException_ExceptionPropagated() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        when(analyticsService.getSpendingByCategory(1L, startDate, endDate))
                .thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            analyticsController.getSpendingByCategory(startDate, endDate, userPrincipal);
        });
        
        assertEquals("Service error", exception.getMessage());
        verify(analyticsService).getSpendingByCategory(1L, startDate, endDate);
    }
    
    @Test
    void getSavingsRate_ServiceThrowsException_ExceptionPropagated() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        when(analyticsService.calculateSavingsRate(1L, startDate, endDate))
                .thenThrow(new RuntimeException("Calculation error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            analyticsController.getSavingsRate(startDate, endDate, userPrincipal);
        });
        
        assertEquals("Calculation error", exception.getMessage());
        verify(analyticsService).calculateSavingsRate(1L, startDate, endDate);
    }
    
    @Test
    void getTrendAnalysis_ServiceThrowsException_ExceptionPropagated() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);
        when(analyticsService.getTrendAnalysis(1L, startDate, endDate))
                .thenThrow(new RuntimeException("Analysis error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            analyticsController.getTrendAnalysis(startDate, endDate, userPrincipal);
        });
        
        assertEquals("Analysis error", exception.getMessage());
        verify(analyticsService).getTrendAnalysis(1L, startDate, endDate);
    }
}