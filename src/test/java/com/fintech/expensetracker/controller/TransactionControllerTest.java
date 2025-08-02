package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.TransactionType;
import com.fintech.expensetracker.dto.request.CreateTransactionRequest;
import com.fintech.expensetracker.dto.request.TransactionFilter;
import com.fintech.expensetracker.dto.request.UpdateTransactionRequest;
import com.fintech.expensetracker.dto.response.AccountResponse;
import com.fintech.expensetracker.dto.response.CategoryResponse;
import com.fintech.expensetracker.dto.response.TransactionResponse;
import com.fintech.expensetracker.entity.AccountType;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.security.UserPrincipal;
import com.fintech.expensetracker.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionController with security context and filtering
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {
    
    @Mock
    private TransactionService transactionService;
    
    @InjectMocks
    private TransactionController transactionController;
    
    private UserPrincipal userPrincipal;
    private TransactionResponse incomeTransaction;
    private TransactionResponse expenseTransaction;
    private CreateTransactionRequest createTransactionRequest;
    private UpdateTransactionRequest updateTransactionRequest;
    private AccountResponse accountResponse;
    private CategoryResponse categoryResponse;
    
    @BeforeEach
    void setUp() {
        // Setup test user principal
        userPrincipal = new UserPrincipal(1L, "Test User", "test@example.com", "password");
        
        // Setup test account and category responses
        accountResponse = new AccountResponse(
                1L,
                "Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1000.00"),
                new BigDecimal("1250.50"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        categoryResponse = new CategoryResponse(
                1L,
                "Food",
                "Food and dining expenses",
                false
        );
        
        // Setup test transaction responses
        incomeTransaction = new TransactionResponse(
                1L,
                new BigDecimal("2500.00"),
                "Salary payment",
                LocalDate.now(),
                accountResponse,
                categoryResponse,
                TransactionType.INCOME,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        expenseTransaction = new TransactionResponse(
                2L,
                new BigDecimal("-50.00"),
                "Grocery shopping",
                LocalDate.now().minusDays(1),
                accountResponse,
                categoryResponse,
                TransactionType.EXPENSE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1)
        );
        
        // Setup test requests
        createTransactionRequest = new CreateTransactionRequest(
                new BigDecimal("100.00"),
                "Test transaction",
                1L,
                TransactionType.EXPENSE
        );
        
        updateTransactionRequest = new UpdateTransactionRequest(
                new BigDecimal("150.00"),
                "Updated transaction",
                LocalDate.now(),
                1L,
                1L,
                TransactionType.EXPENSE
        );
    }
    
    @Test
    void getUserTransactions_NoFilters_ReturnsPagedTransactions() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(incomeTransaction, expenseTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20)))
                .thenReturn(page);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getUserTransactions(
                userPrincipal, null, null, null, null, null, null, null, 0, 20);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals("Salary payment", response.getBody().getContent().get(0).getDescription());
        assertEquals("Grocery shopping", response.getBody().getContent().get(1).getDescription());
        verify(transactionService).getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20));
    }
    
    @Test
    void getUserTransactions_WithFilters_ReturnsFilteredTransactions() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(expenseTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(10)))
                .thenReturn(page);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getUserTransactions(
                userPrincipal, 1L, 1L, "2025-01-01", "2025-01-31", "10.00", "100.00", "grocery", 0, 10);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("Grocery shopping", response.getBody().getContent().get(0).getDescription());
        verify(transactionService).getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(10));
    }
    
    @Test
    void getUserTransactions_InvalidDateFormat_IgnoresInvalidDates() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(incomeTransaction, expenseTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20)))
                .thenReturn(page);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getUserTransactions(
                userPrincipal, null, null, "invalid-date", "also-invalid", null, null, null, 0, 20);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        verify(transactionService).getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20));
    }
    
    @Test
    void getTransactionById_ValidTransactionId_ReturnsTransaction() {
        // Given
        when(transactionService.getTransactionById(1L, 1L)).thenReturn(incomeTransaction);
        
        // When
        ResponseEntity<TransactionResponse> response = transactionController.getTransactionById(1L, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Salary payment", response.getBody().getDescription());
        assertEquals(TransactionType.INCOME, response.getBody().getType());
        verify(transactionService).getTransactionById(1L, 1L);
    }
    
    @Test
    void getTransactionById_TransactionNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(transactionService.getTransactionById(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Transaction not found with ID: 999"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            transactionController.getTransactionById(999L, userPrincipal);
        });
        
        assertEquals("Transaction not found with ID: 999", exception.getMessage());
        verify(transactionService).getTransactionById(999L, 1L);
    }
    
    @Test
    void createTransaction_ValidRequest_ReturnsCreatedTransaction() {
        // Given
        TransactionResponse createdTransaction = new TransactionResponse(
                3L,
                new BigDecimal("-100.00"),
                "Test transaction",
                LocalDate.now(),
                accountResponse,
                categoryResponse,
                TransactionType.EXPENSE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq(1L)))
                .thenReturn(createdTransaction);
        
        // When
        ResponseEntity<TransactionResponse> response = transactionController.createTransaction(
                createTransactionRequest, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3L, response.getBody().getId());
        assertEquals("Test transaction", response.getBody().getDescription());
        assertEquals(TransactionType.EXPENSE, response.getBody().getType());
        verify(transactionService).createTransaction(any(CreateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void createTransaction_InvalidAccount_ThrowsResourceNotFoundException() {
        // Given
        when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            transactionController.createTransaction(createTransactionRequest, userPrincipal);
        });
        
        assertEquals("Account not found with ID: 999", exception.getMessage());
        verify(transactionService).createTransaction(any(CreateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void createTransaction_InvalidAmount_ThrowsBadRequestException() {
        // Given
        when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq(1L)))
                .thenThrow(new BadRequestException("Amount must be greater than zero"));
        
        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            transactionController.createTransaction(createTransactionRequest, userPrincipal);
        });
        
        assertEquals("Amount must be greater than zero", exception.getMessage());
        verify(transactionService).createTransaction(any(CreateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void updateTransaction_ValidRequest_ReturnsUpdatedTransaction() {
        // Given
        TransactionResponse updatedTransaction = new TransactionResponse(
                1L,
                new BigDecimal("-150.00"),
                "Updated transaction",
                LocalDate.now(),
                accountResponse,
                categoryResponse,
                TransactionType.EXPENSE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
        when(transactionService.updateTransaction(eq(1L), any(UpdateTransactionRequest.class), eq(1L)))
                .thenReturn(updatedTransaction);
        
        // When
        ResponseEntity<TransactionResponse> response = transactionController.updateTransaction(
                1L, updateTransactionRequest, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Updated transaction", response.getBody().getDescription());
        assertEquals(new BigDecimal("-150.00"), response.getBody().getAmount());
        verify(transactionService).updateTransaction(eq(1L), any(UpdateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void updateTransaction_TransactionNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(transactionService.updateTransaction(eq(999L), any(UpdateTransactionRequest.class), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Transaction not found with ID: 999"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            transactionController.updateTransaction(999L, updateTransactionRequest, userPrincipal);
        });
        
        assertEquals("Transaction not found with ID: 999", exception.getMessage());
        verify(transactionService).updateTransaction(eq(999L), any(UpdateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void deleteTransaction_ValidTransactionId_ReturnsNoContent() {
        // Given - service method doesn't throw exception
        doNothing().when(transactionService).softDeleteTransaction(1L, 1L);
        
        // When
        ResponseEntity<Void> response = transactionController.deleteTransaction(1L, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(transactionService).softDeleteTransaction(1L, 1L);
    }
    
    @Test
    void deleteTransaction_TransactionNotFound_ThrowsResourceNotFoundException() {
        // Given
        doThrow(new ResourceNotFoundException("Transaction not found with ID: 999"))
                .when(transactionService).softDeleteTransaction(999L, 1L);
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            transactionController.deleteTransaction(999L, userPrincipal);
        });
        
        assertEquals("Transaction not found with ID: 999", exception.getMessage());
        verify(transactionService).softDeleteTransaction(999L, 1L);
    }
    
    @Test
    void getAccountTransactions_ValidAccountId_ReturnsPagedTransactions() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(incomeTransaction, expenseTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getAccountTransactions(1L, 1L, 0, 20)).thenReturn(page);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getAccountTransactions(
                1L, userPrincipal, 0, 20);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals("Salary payment", response.getBody().getContent().get(0).getDescription());
        assertEquals("Grocery shopping", response.getBody().getContent().get(1).getDescription());
        verify(transactionService).getAccountTransactions(1L, 1L, 0, 20);
    }
    
    @Test
    void getAccountTransactions_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(transactionService.getAccountTransactions(999L, 1L, 0, 20))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            transactionController.getAccountTransactions(999L, userPrincipal, 0, 20);
        });
        
        assertEquals("Account not found with ID: 999", exception.getMessage());
        verify(transactionService).getAccountTransactions(999L, 1L, 0, 20);
    }
    
    @Test
    void getAccountTransactions_CustomPagination_ReturnsCorrectPage() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(expenseTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getAccountTransactions(1L, 1L, 1, 5)).thenReturn(page);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getAccountTransactions(
                1L, userPrincipal, 1, 5);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(transactionService).getAccountTransactions(1L, 1L, 1, 5);
    }
    
    @Test
    void getUserTransactions_EmptyResult_ReturnsEmptyPage() {
        // Given
        Page<TransactionResponse> emptyPage = new PageImpl<>(Arrays.asList());
        when(transactionService.getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20)))
                .thenReturn(emptyPage);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getUserTransactions(
                userPrincipal, null, null, null, null, null, null, null, 0, 20);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getContent().size());
        verify(transactionService).getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20));
    }
    
    @Test
    void createTransaction_ServiceThrowsException_ExceptionPropagated() {
        // Given
        when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq(1L)))
                .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionController.createTransaction(createTransactionRequest, userPrincipal);
        });
        
        assertEquals("Database error", exception.getMessage());
        verify(transactionService).createTransaction(any(CreateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void updateTransaction_ServiceThrowsException_ExceptionPropagated() {
        // Given
        when(transactionService.updateTransaction(eq(1L), any(UpdateTransactionRequest.class), eq(1L)))
                .thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionController.updateTransaction(1L, updateTransactionRequest, userPrincipal);
        });
        
        assertEquals("Service error", exception.getMessage());
        verify(transactionService).updateTransaction(eq(1L), any(UpdateTransactionRequest.class), eq(1L));
    }
    
    @Test
    void getUserTransactions_InvalidAmountFormat_IgnoresInvalidAmounts() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(incomeTransaction, expenseTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20)))
                .thenReturn(page);
        
        // When
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getUserTransactions(
                userPrincipal, null, null, null, null, "invalid-amount", "also-invalid", null, 0, 20);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        verify(transactionService).getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20));
    }
    
    @Test
    void getUserTransactions_DefaultPagination_UsesCorrectDefaults() {
        // Given
        List<TransactionResponse> transactions = Arrays.asList(incomeTransaction);
        Page<TransactionResponse> page = new PageImpl<>(transactions);
        when(transactionService.getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20)))
                .thenReturn(page);
        
        // When - not providing page and size parameters
        ResponseEntity<Page<TransactionResponse>> response = transactionController.getUserTransactions(
                userPrincipal, null, null, null, null, null, null, null, 0, 20);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService).getUserTransactions(eq(1L), any(TransactionFilter.class), eq(0), eq(20));
    }
}