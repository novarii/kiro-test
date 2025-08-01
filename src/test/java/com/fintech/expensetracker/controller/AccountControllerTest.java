package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.request.CreateAccountRequest;
import com.fintech.expensetracker.dto.request.UpdateAccountRequest;
import com.fintech.expensetracker.dto.response.AccountResponse;
import com.fintech.expensetracker.entity.AccountType;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.security.UserPrincipal;
import com.fintech.expensetracker.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountController with security context
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    
    @Mock
    private AccountService accountService;
    
    @InjectMocks
    private AccountController accountController;
    
    private UserPrincipal userPrincipal;
    private AccountResponse accountResponse1;
    private AccountResponse accountResponse2;
    private CreateAccountRequest createAccountRequest;
    private UpdateAccountRequest updateAccountRequest;
    
    @BeforeEach
    void setUp() {
        // Setup test user principal
        userPrincipal = new UserPrincipal(1L, "Test User", "test@example.com", "password");
        
        // Setup test account responses
        accountResponse1 = new AccountResponse(
                1L,
                "Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1000.00"),
                new BigDecimal("1250.50"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        accountResponse2 = new AccountResponse(
                2L,
                "Savings Account",
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                new BigDecimal("5125.75"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        // Setup test requests
        createAccountRequest = new CreateAccountRequest(
                "New Account",
                AccountType.CHECKING,
                new BigDecimal("500.00")
        );
        
        updateAccountRequest = new UpdateAccountRequest(
                "Updated Account Name",
                AccountType.SAVINGS,
                new BigDecimal("1500.00")
        );
    }
    
    @Test
    void getUserAccounts_AuthenticatedUser_ReturnsAccountList() {
        // Given
        List<AccountResponse> accounts = Arrays.asList(accountResponse1, accountResponse2);
        when(accountService.getUserAccounts(1L)).thenReturn(accounts);
        
        // When
        ResponseEntity<List<AccountResponse>> response = accountController.getUserAccounts(userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Checking Account", response.getBody().get(0).getName());
        assertEquals("Savings Account", response.getBody().get(1).getName());
        verify(accountService).getUserAccounts(1L);
    }
    
    @Test
    void getAccountById_ValidAccountId_ReturnsAccount() {
        // Given
        when(accountService.getAccountById(1L, 1L)).thenReturn(accountResponse1);
        
        // When
        ResponseEntity<AccountResponse> response = accountController.getAccountById(1L, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Checking Account", response.getBody().getName());
        assertEquals(AccountType.CHECKING, response.getBody().getType());
        verify(accountService).getAccountById(1L, 1L);
    }
    
    @Test
    void getAccountById_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountService.getAccountById(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999 for user ID: 1"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            accountController.getAccountById(999L, userPrincipal);
        });
        
        assertEquals("Account not found with ID: 999 for user ID: 1", exception.getMessage());
        verify(accountService).getAccountById(999L, 1L);
    }
    
    @Test
    void createAccount_ValidRequest_ReturnsCreatedAccount() {
        // Given
        AccountResponse createdAccount = new AccountResponse(
                3L,
                "New Account",
                AccountType.CHECKING,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(accountService.createAccount(any(CreateAccountRequest.class), eq(1L)))
                .thenReturn(createdAccount);
        
        // When
        ResponseEntity<AccountResponse> response = accountController.createAccount(createAccountRequest, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3L, response.getBody().getId());
        assertEquals("New Account", response.getBody().getName());
        assertEquals(AccountType.CHECKING, response.getBody().getType());
        verify(accountService).createAccount(any(CreateAccountRequest.class), eq(1L));
    }
    
    @Test
    void updateAccount_ValidRequest_ReturnsUpdatedAccount() {
        // Given
        AccountResponse updatedAccount = new AccountResponse(
                1L,
                "Updated Account Name",
                AccountType.SAVINGS,
                new BigDecimal("1500.00"),
                new BigDecimal("1750.50"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(accountService.updateAccount(eq(1L), any(UpdateAccountRequest.class), eq(1L)))
                .thenReturn(updatedAccount);
        
        // When
        ResponseEntity<AccountResponse> response = accountController.updateAccount(1L, updateAccountRequest, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Updated Account Name", response.getBody().getName());
        assertEquals(AccountType.SAVINGS, response.getBody().getType());
        verify(accountService).updateAccount(eq(1L), any(UpdateAccountRequest.class), eq(1L));
    }
    
    @Test
    void updateAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountService.updateAccount(eq(999L), any(UpdateAccountRequest.class), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999 for user ID: 1"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            accountController.updateAccount(999L, updateAccountRequest, userPrincipal);
        });
        
        assertEquals("Account not found with ID: 999 for user ID: 1", exception.getMessage());
        verify(accountService).updateAccount(eq(999L), any(UpdateAccountRequest.class), eq(1L));
    }
    
    @Test
    void deleteAccount_ValidAccountId_ReturnsNoContent() {
        // Given - service method doesn't throw exception
        doNothing().when(accountService).softDeleteAccount(1L, 1L);
        
        // When
        ResponseEntity<Void> response = accountController.deleteAccount(1L, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(accountService).softDeleteAccount(1L, 1L);
    }
    
    @Test
    void deleteAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        doThrow(new ResourceNotFoundException("Account not found with ID: 999 for user ID: 1"))
                .when(accountService).softDeleteAccount(999L, 1L);
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            accountController.deleteAccount(999L, userPrincipal);
        });
        
        assertEquals("Account not found with ID: 999 for user ID: 1", exception.getMessage());
        verify(accountService).softDeleteAccount(999L, 1L);
    }
    
    @Test
    void deleteAccount_AccountHasTransactions_ThrowsBadRequestException() {
        // Given
        doThrow(new BadRequestException("Cannot delete account with existing transactions. Account ID: 1"))
                .when(accountService).softDeleteAccount(1L, 1L);
        
        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            accountController.deleteAccount(1L, userPrincipal);
        });
        
        assertEquals("Cannot delete account with existing transactions. Account ID: 1", exception.getMessage());
        verify(accountService).softDeleteAccount(1L, 1L);
    }
    
    @Test
    void getAccountBalance_ValidAccountId_ReturnsBalance() {
        // Given
        BigDecimal balance = new BigDecimal("1250.50");
        when(accountService.calculateAccountBalance(1L, 1L)).thenReturn(balance);
        
        // When
        ResponseEntity<AccountController.BalanceResponse> response = accountController.getAccountBalance(1L, userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getAccountId());
        assertEquals(balance, response.getBody().getBalance());
        verify(accountService).calculateAccountBalance(1L, 1L);
    }
    
    @Test
    void getAccountBalance_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountService.calculateAccountBalance(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999 for user ID: 1"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            accountController.getAccountBalance(999L, userPrincipal);
        });
        
        assertEquals("Account not found with ID: 999 for user ID: 1", exception.getMessage());
        verify(accountService).calculateAccountBalance(999L, 1L);
    }
    
    @Test
    void getTotalBalance_AuthenticatedUser_ReturnsTotalBalance() {
        // Given
        BigDecimal totalBalance = new BigDecimal("6376.25");
        when(accountService.getTotalBalanceForUser(1L)).thenReturn(totalBalance);
        
        // When
        ResponseEntity<AccountController.TotalBalanceResponse> response = accountController.getTotalBalance(userPrincipal);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getUserId());
        assertEquals(totalBalance, response.getBody().getTotalBalance());
        verify(accountService).getTotalBalanceForUser(1L);
    }
    
    @Test
    void getTotalBalance_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountService.getTotalBalanceForUser(1L))
                .thenThrow(new ResourceNotFoundException("User not found with ID: 1"));
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            accountController.getTotalBalance(userPrincipal);
        });
        
        assertEquals("User not found with ID: 1", exception.getMessage());
        verify(accountService).getTotalBalanceForUser(1L);
    }
    
    @Test
    void createAccount_ServiceThrowsException_ExceptionPropagated() {
        // Given
        when(accountService.createAccount(any(CreateAccountRequest.class), eq(1L)))
                .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountController.createAccount(createAccountRequest, userPrincipal);
        });
        
        assertEquals("Database error", exception.getMessage());
        verify(accountService).createAccount(any(CreateAccountRequest.class), eq(1L));
    }
    
    @Test
    void updateAccount_ServiceThrowsException_ExceptionPropagated() {
        // Given
        when(accountService.updateAccount(eq(1L), any(UpdateAccountRequest.class), eq(1L)))
                .thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountController.updateAccount(1L, updateAccountRequest, userPrincipal);
        });
        
        assertEquals("Service error", exception.getMessage());
        verify(accountService).updateAccount(eq(1L), any(UpdateAccountRequest.class), eq(1L));
    }
}