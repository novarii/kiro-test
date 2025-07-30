package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.TransactionType;
import com.fintech.expensetracker.dto.request.CreateTransactionRequest;
import com.fintech.expensetracker.dto.request.TransactionFilter;
import com.fintech.expensetracker.dto.request.UpdateTransactionRequest;
import com.fintech.expensetracker.dto.response.TransactionResponse;
import com.fintech.expensetracker.entity.Account;
import com.fintech.expensetracker.entity.AccountType;
import com.fintech.expensetracker.entity.Category;
import com.fintech.expensetracker.entity.Transaction;
import com.fintech.expensetracker.entity.User;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.repository.AccountRepository;
import com.fintech.expensetracker.repository.CategoryRepository;
import com.fintech.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private TransactionService transactionService;
    
    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private Category defaultCategory;
    private Transaction testTransaction;
    
    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        
        // Set up test account
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setType(AccountType.CHECKING);
        testAccount.setInitialBalance(BigDecimal.valueOf(1000));
        testAccount.setUser(testUser);
        testAccount.setDeleted(false);
        
        // Set up test category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");
        testCategory.setDescription("Food expenses");
        testCategory.setDefault(false);
        
        // Set up default category
        defaultCategory = new Category();
        defaultCategory.setId(2L);
        defaultCategory.setName("Uncategorized");
        defaultCategory.setDescription("Default category for uncategorized transactions");
        defaultCategory.setDefault(true);
        
        // Set up test transaction
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(BigDecimal.valueOf(-50.00));
        testTransaction.setDescription("Grocery shopping");
        testTransaction.setTransactionDate(LocalDate.now());
        testTransaction.setAccount(testAccount);
        testTransaction.setCategory(testCategory);
        testTransaction.setDeleted(false);
        testTransaction.setCreatedAt(LocalDateTime.now());
        testTransaction.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    void createTransaction_ValidExpenseRequest_ReturnsTransactionResponse() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Grocery shopping");
        request.setTransactionDate(LocalDate.now());
        request.setAccountId(1L);
        request.setCategoryId(1L);
        request.setType(TransactionType.EXPENSE);
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(testTransaction);
        
        // When
        TransactionResponse response = transactionService.createTransaction(request, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(-50.00));
        assertThat(response.getDescription()).isEqualTo("Grocery shopping");
        assertThat(response.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.getAccount().getId()).isEqualTo(1L);
        assertThat(response.getCategory().getId()).isEqualTo(1L);
        
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_ValidIncomeRequest_ReturnsTransactionResponse() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(1000.00));
        request.setDescription("Salary");
        request.setAccountId(1L);
        request.setCategoryId(1L);
        request.setType(TransactionType.INCOME);
        
        Transaction incomeTransaction = new Transaction();
        incomeTransaction.setId(2L);
        incomeTransaction.setAmount(BigDecimal.valueOf(1000.00));
        incomeTransaction.setDescription("Salary");
        incomeTransaction.setTransactionDate(LocalDate.now());
        incomeTransaction.setAccount(testAccount);
        incomeTransaction.setCategory(testCategory);
        incomeTransaction.setCreatedAt(LocalDateTime.now());
        incomeTransaction.setUpdatedAt(LocalDateTime.now());
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(incomeTransaction);
        
        // When
        TransactionResponse response = transactionService.createTransaction(request, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(1000.00));
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
        
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_WithoutCategory_UsesDefaultCategory() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Grocery shopping");
        request.setAccountId(1L);
        request.setType(TransactionType.EXPENSE);
        // No categoryId set
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIsDefaultTrue())
                .thenReturn(Optional.of(defaultCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(testTransaction);
        
        // When
        TransactionResponse response = transactionService.createTransaction(request, 1L);
        
        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).findByIsDefaultTrue();
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_NoDefaultCategoryExists_CreatesDefaultCategory() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Grocery shopping");
        request.setAccountId(1L);
        request.setType(TransactionType.EXPENSE);
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIsDefaultTrue())
                .thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(defaultCategory);
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(testTransaction);
        
        // When
        TransactionResponse response = transactionService.createTransaction(request, 1L);
        
        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).findByIsDefaultTrue();
        verify(categoryRepository).save(any(Category.class));
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Grocery shopping");
        request.setAccountId(999L);
        request.setType(TransactionType.EXPENSE);
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
        
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_CategoryNotFound_ThrowsResourceNotFoundException() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Grocery shopping");
        request.setAccountId(1L);
        request.setCategoryId(999L);
        request.setType(TransactionType.EXPENSE);
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found with ID: 999");
        
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    
    @Test
    void updateTransaction_ValidRequest_ReturnsUpdatedTransaction() {
        // Given
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(75.00));
        request.setDescription("Updated grocery shopping");
        request.setType(TransactionType.EXPENSE);
        
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(testTransaction);
        
        // When
        TransactionResponse response = transactionService.updateTransaction(1L, request, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void updateTransaction_TransactionNotFound_ThrowsResourceNotFoundException() {
        // Given
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAmount(BigDecimal.valueOf(75.00));
        request.setType(TransactionType.EXPENSE);
        
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.updateTransaction(999L, request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 999");
        
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    
    @Test
    void updateTransaction_UpdateAccount_ValidatesAccountOwnership() {
        // Given
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAccountId(2L);
        
        Account newAccount = new Account();
        newAccount.setId(2L);
        newAccount.setName("New Account");
        newAccount.setUser(testUser);
        
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testTransaction));
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(2L, 1L))
                .thenReturn(Optional.of(newAccount));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(testTransaction);
        
        // When
        TransactionResponse response = transactionService.updateTransaction(1L, request, 1L);
        
        // Then
        assertThat(response).isNotNull();
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(2L, 1L);
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void getUserTransactions_NoFilter_ReturnsAllUserTransactions() {
        // Given
        List<Transaction> transactions = Arrays.asList(testTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        
        when(transactionRepository.findByUserIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                .thenReturn(transactionPage);
        
        // When
        Page<TransactionResponse> result = transactionService.getUserTransactions(1L, null, 0, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        
        verify(transactionRepository).findByUserIdAndDeletedFalse(eq(1L), any(Pageable.class));
    }
    
    @Test
    void getUserTransactions_WithAccountFilter_ReturnsFilteredTransactions() {
        // Given
        TransactionFilter filter = new TransactionFilter();
        filter.setAccountId(1L);
        
        List<Transaction> transactions = Arrays.asList(testTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        
        when(transactionRepository.findByAccountIdAndUserIdAndDeletedFalse(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(transactionPage);
        
        // When
        Page<TransactionResponse> result = transactionService.getUserTransactions(1L, filter, 0, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        verify(transactionRepository).findByAccountIdAndUserIdAndDeletedFalse(eq(1L), eq(1L), any(Pageable.class));
    }
    
    @Test
    void getUserTransactions_WithCategoryFilter_ReturnsFilteredTransactions() {
        // Given
        TransactionFilter filter = new TransactionFilter();
        filter.setCategoryId(1L);
        
        List<Transaction> transactions = Arrays.asList(testTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        
        when(transactionRepository.findByUserIdAndCategoryIdAndDeletedFalse(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(transactionPage);
        
        // When
        Page<TransactionResponse> result = transactionService.getUserTransactions(1L, filter, 0, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        verify(transactionRepository).findByUserIdAndCategoryIdAndDeletedFalse(eq(1L), eq(1L), any(Pageable.class));
    }
    
    @Test
    void getUserTransactions_WithDateRangeFilter_ReturnsFilteredTransactions() {
        // Given
        TransactionFilter filter = new TransactionFilter();
        filter.setStartDate(LocalDate.now().minusDays(7));
        filter.setEndDate(LocalDate.now());
        
        List<Transaction> transactions = Arrays.asList(testTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        
        when(transactionRepository.findByUserIdAndDateRangeAndDeletedFalse(
                eq(1L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(transactionPage);
        
        // When
        Page<TransactionResponse> result = transactionService.getUserTransactions(1L, filter, 0, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        verify(transactionRepository).findByUserIdAndDateRangeAndDeletedFalse(
                eq(1L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }
    
    @Test
    void getTransactionById_ValidId_ReturnsTransaction() {
        // Given
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testTransaction));
        
        // When
        TransactionResponse response = transactionService.getTransactionById(1L, 1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDescription()).isEqualTo("Grocery shopping");
    }
    
    @Test
    void getTransactionById_TransactionNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 999");
    }
    
    @Test
    void softDeleteTransaction_ValidId_MarksTransactionAsDeleted() {
        // Given
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(testTransaction);
        
        // When
        transactionService.softDeleteTransaction(1L, 1L);
        
        // Then
        verify(transactionRepository).save(argThat(transaction -> 
                transaction.isDeleted() == true));
    }
    
    @Test
    void softDeleteTransaction_TransactionNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.softDeleteTransaction(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 999");
        
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    
    @Test
    void getAccountTransactions_ValidAccount_ReturnsTransactions() {
        // Given
        List<Transaction> transactions = Arrays.asList(testTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndUserIdAndDeletedFalse(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(transactionPage);
        
        // When
        Page<TransactionResponse> result = transactionService.getAccountTransactions(1L, 1L, 0, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccount().getId()).isEqualTo(1L);
        
        verify(accountRepository).findByIdAndUserIdAndDeletedFalse(1L, 1L);
        verify(transactionRepository).findByAccountIdAndUserIdAndDeletedFalse(eq(1L), eq(1L), any(Pageable.class));
    }
    
    @Test
    void getAccountTransactions_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByIdAndUserIdAndDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.getAccountTransactions(999L, 1L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
        
        verify(transactionRepository, never()).findByAccountIdAndUserIdAndDeletedFalse(anyLong(), anyLong(), any(Pageable.class));
    }
}