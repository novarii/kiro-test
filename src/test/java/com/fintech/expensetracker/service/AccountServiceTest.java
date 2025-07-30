package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.request.CreateAccountRequest;
import com.fintech.expensetracker.dto.request.UpdateAccountRequest;
import com.fintech.expensetracker.dto.response.AccountResponse;
import com.fintech.expensetracker.entity.Account;
import com.fintech.expensetracker.entity.AccountType;
import com.fintech.expensetracker.entity.Transaction;
import com.fintech.expensetracker.entity.User;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.repository.AccountRepository;
import com.fintech.expensetracker.repository.TransactionRepository;
import com.fintech.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountService
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    private User testUser;
    private Account testAccount;
    private CreateAccountRequest createRequest;
    private UpdateAccountRequest updateRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setType(AccountType.CHECKING);
        testAccount.setInitialBalance(new BigDecimal("1000.00"));
        testAccount.setUser(testUser);
        testAccount.setDeleted(false);
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());
        
        createRequest = new CreateAccountRequest();
        createRequest.setName("New Account");
        createRequest.setType(AccountType.SAVINGS);
        createRequest.setInitialBalance(new BigDecimal("500.00"));
        
        updateRequest = new UpdateAccountRequest();
        updateRequest.setName("Updated Account");
        updateRequest.setType(AccountType.CREDIT);
        updateRequest.setInitialBalance(new BigDecimal("2000.00"));
    }
    
    @Test
    void createAccount_ValidRequest_ReturnsAccountResponse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(BigDecimal.ZERO);
        
        // When
        AccountResponse response = accountService.createAccount(createRequest, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Account");
        assertThat(response.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.getInitialBalance()).isEqualTo(new BigDecimal("1000.00"));
        
        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    void createAccount_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(createRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    void getUserAccounts_ValidUser_ReturnsAccountList() {
        // Given
        Account account2 = new Account();
        account2.setId(2L);
        account2.setName("Second Account");
        account2.setType(AccountType.SAVINGS);
        account2.setInitialBalance(new BigDecimal("2000.00"));
        account2.setUser(testUser);
        account2.setCreatedAt(LocalDateTime.now());
        account2.setUpdatedAt(LocalDateTime.now());
        
        List<Account> accounts = Arrays.asList(testAccount, account2);
        
        when(userRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(accounts);
        when(transactionRepository.calculateAccountBalanceFromTransactions(anyLong(), eq(1L)))
                .thenReturn(BigDecimal.ZERO);
        
        // When
        List<AccountResponse> responses = accountService.getUserAccounts(1L);
        
        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        
        verify(userRepository).existsById(1L);
        verify(accountRepository).findByUserIdAndDeletedFalse(1L);
    }
    
    @Test
    void getUserAccounts_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> accountService.getUserAccounts(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).existsById(1L);
        verify(accountRepository, never()).findByUserIdAndDeletedFalse(anyLong());
    }
    
    @Test
    void getUserAccounts_NoAccounts_ReturnsEmptyList() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());
        
        // When
        List<AccountResponse> responses = accountService.getUserAccounts(1L);
        
        // Then
        assertThat(responses).isEmpty();
        
        verify(userRepository).existsById(1L);
        verify(accountRepository).findByUserIdAndDeletedFalse(1L);
    }
    
    @Test
    void updateAccount_ValidRequest_ReturnsUpdatedAccount() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(BigDecimal.ZERO);
        
        // When
        AccountResponse response = accountService.updateAccount(1L, updateRequest, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(accountRepository).save(testAccount);
        
        // Verify account was updated
        assertThat(testAccount.getName()).isEqualTo("Updated Account");
        assertThat(testAccount.getType()).isEqualTo(AccountType.CREDIT);
        assertThat(testAccount.getInitialBalance()).isEqualTo(new BigDecimal("2000.00"));
    }
    
    @Test
    void updateAccount_PartialUpdate_UpdatesOnlyProvidedFields() {
        // Given
        UpdateAccountRequest partialRequest = new UpdateAccountRequest();
        partialRequest.setName("Partially Updated");
        // type and initialBalance are null
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(BigDecimal.ZERO);
        
        AccountType originalType = testAccount.getType();
        BigDecimal originalBalance = testAccount.getInitialBalance();
        
        // When
        AccountResponse response = accountService.updateAccount(1L, partialRequest, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(testAccount.getName()).isEqualTo("Partially Updated");
        assertThat(testAccount.getType()).isEqualTo(originalType); // Should remain unchanged
        assertThat(testAccount.getInitialBalance()).isEqualTo(originalBalance); // Should remain unchanged
        
        verify(accountRepository).save(testAccount);
    }
    
    @Test
    void updateAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(1L, updateRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with ID: 1 for user ID: 1");
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    void updateAccount_EmptyName_DoesNotUpdateName() {
        // Given
        UpdateAccountRequest requestWithEmptyName = new UpdateAccountRequest();
        requestWithEmptyName.setName("   "); // Empty/whitespace name
        requestWithEmptyName.setType(AccountType.SAVINGS);
        
        String originalName = testAccount.getName();
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(BigDecimal.ZERO);
        
        // When
        accountService.updateAccount(1L, requestWithEmptyName, 1L);
        
        // Then
        assertThat(testAccount.getName()).isEqualTo(originalName); // Should remain unchanged
        assertThat(testAccount.getType()).isEqualTo(AccountType.SAVINGS); // Should be updated
    }
    
    @Test
    void softDeleteAccount_ValidAccount_DeletesAccount() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.existsByAccountIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        
        // When
        accountService.softDeleteAccount(1L, 1L);
        
        // Then
        assertThat(testAccount.isDeleted()).isTrue();
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(transactionRepository).existsByAccountIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(accountRepository).save(testAccount);
    }
    
    @Test
    void softDeleteAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> accountService.softDeleteAccount(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with ID: 1 for user ID: 1");
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(transactionRepository, never()).existsByAccountIdAndUserIdAndDeletedFalse(anyLong(), anyLong());
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    void softDeleteAccount_HasTransactions_ThrowsBadRequestException() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.existsByAccountIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> accountService.softDeleteAccount(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete account with existing transactions. Account ID: 1");
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(transactionRepository).existsByAccountIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(accountRepository, never()).save(any(Account.class));
        
        // Verify account was not deleted
        assertThat(testAccount.isDeleted()).isFalse();
    }
    
    @Test
    void calculateAccountBalance_ValidAccount_ReturnsCorrectBalance() {
        // Given
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal transactionSum = new BigDecimal("250.50");
        BigDecimal expectedBalance = new BigDecimal("1250.50");
        
        testAccount.setInitialBalance(initialBalance);
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(transactionSum);
        
        // When
        BigDecimal balance = accountService.calculateAccountBalance(1L, 1L);
        
        // Then
        assertThat(balance).isEqualTo(expectedBalance);
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(transactionRepository).calculateAccountBalanceFromTransactions(1L, 1L);
    }
    
    @Test
    void calculateAccountBalance_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> accountService.calculateAccountBalance(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with ID: 1 for user ID: 1");
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(transactionRepository, never()).calculateAccountBalanceFromTransactions(anyLong(), anyLong());
    }
    
    @Test
    void getAccountById_ValidAccount_ReturnsAccountResponse() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(BigDecimal.ZERO);
        
        // When
        AccountResponse response = accountService.getAccountById(1L, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Account");
        assertThat(response.getType()).isEqualTo(AccountType.CHECKING);
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
    }
    
    @Test
    void getAccountById_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> accountService.getAccountById(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with ID: 1 for user ID: 1");
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
    }
    
    @Test
    void accountExistsForUser_AccountExists_ReturnsTrue() {
        // Given
        when(accountRepository.existsByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(true);
        
        // When
        boolean exists = accountService.accountExistsForUser(1L, 1L);
        
        // Then
        assertThat(exists).isTrue();
        
        verify(accountRepository).existsByIdAndUserIdAndDeletedFalse(1L, 1L);
    }
    
    @Test
    void accountExistsForUser_AccountDoesNotExist_ReturnsFalse() {
        // Given
        when(accountRepository.existsByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);
        
        // When
        boolean exists = accountService.accountExistsForUser(1L, 1L);
        
        // Then
        assertThat(exists).isFalse();
        
        verify(accountRepository).existsByIdAndUserIdAndDeletedFalse(1L, 1L);
    }
    
    @Test
    void getTotalBalanceForUser_ValidUser_ReturnsTotalBalance() {
        // Given
        BigDecimal expectedTotal = new BigDecimal("5000.00");
        
        when(userRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.calculateTotalBalanceForUser(1L)).thenReturn(expectedTotal);
        
        // When
        BigDecimal totalBalance = accountService.getTotalBalanceForUser(1L);
        
        // Then
        assertThat(totalBalance).isEqualTo(expectedTotal);
        
        verify(userRepository).existsById(1L);
        verify(accountRepository).calculateTotalBalanceForUser(1L);
    }
    
    @Test
    void getTotalBalanceForUser_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> accountService.getTotalBalanceForUser(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).existsById(1L);
        verify(accountRepository, never()).calculateTotalBalanceForUser(anyLong());
    }
    
    @Test
    void getTotalBalanceForUser_NullBalance_ReturnsZero() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.calculateTotalBalanceForUser(1L)).thenReturn(null);
        
        // When
        BigDecimal totalBalance = accountService.getTotalBalanceForUser(1L);
        
        // Then
        assertThat(totalBalance).isEqualTo(BigDecimal.ZERO);
        
        verify(userRepository).existsById(1L);
        verify(accountRepository).calculateTotalBalanceForUser(1L);
    }
    
    @Test
    void mapToAccountResponse_WithTransactions_CalculatesBalanceFromTransactions() {
        // Given
        Transaction transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setDeleted(false);
        
        Transaction transaction2 = new Transaction();
        transaction2.setAmount(new BigDecimal("-50.00"));
        transaction2.setDeleted(false);
        
        Transaction deletedTransaction = new Transaction();
        deletedTransaction.setAmount(new BigDecimal("200.00"));
        deletedTransaction.setDeleted(true);
        
        testAccount.setTransactions(Arrays.asList(transaction1, transaction2, deletedTransaction));
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        
        // When
        AccountResponse response = accountService.getAccountById(1L, 1L);
        
        // Then
        // Expected: 1000.00 (initial) + 100.00 - 50.00 = 1050.00 (deleted transaction ignored)
        assertThat(response.getCurrentBalance()).isEqualTo(new BigDecimal("1050.00"));
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
    }
    
    @Test
    void createAccount_WithNullInitialBalance_DefaultsToZero() {
        // Given
        createRequest.setInitialBalance(null);
        
        Account savedAccount = new Account();
        savedAccount.setId(1L);
        savedAccount.setName("New Account");
        savedAccount.setType(AccountType.SAVINGS);
        savedAccount.setInitialBalance(BigDecimal.ZERO);
        savedAccount.setUser(testUser);
        savedAccount.setCreatedAt(LocalDateTime.now());
        savedAccount.setUpdatedAt(LocalDateTime.now());
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionRepository.calculateAccountBalanceFromTransactions(1L, 1L))
                .thenReturn(BigDecimal.ZERO);
        
        // When
        AccountResponse response = accountService.createAccount(createRequest, 1L);
        
        // Then
        assertThat(response.getInitialBalance()).isEqualTo(BigDecimal.ZERO);
        
        verify(accountRepository).save(argThat(account -> 
                account.getInitialBalance().equals(BigDecimal.ZERO)));
    }
}