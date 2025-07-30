package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.request.LoginRequest;
import com.fintech.expensetracker.dto.request.UserRegistrationRequest;
import com.fintech.expensetracker.dto.response.AuthResponse;
import com.fintech.expensetracker.dto.response.UserResponse;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthController
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private AuthController authController;
    
    private UserRegistrationRequest validRegistrationRequest;
    private LoginRequest validLoginRequest;
    private AuthResponse authResponse;
    private UserResponse userResponse;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        validRegistrationRequest = new UserRegistrationRequest(
            "test@example.com",
            "password123",
            "Test User"
        );
        
        validLoginRequest = new LoginRequest(
            "test@example.com",
            "password123"
        );
        
        userResponse = new UserResponse(
            1L,
            "test@example.com",
            "Test User",
            LocalDateTime.now()
        );
        
        authResponse = new AuthResponse(
            "jwt-token-here",
            userResponse
        );
    }
    
    @Test
    void registerUser_ValidRequest_ReturnsCreatedWithAuthResponse() {
        // Given
        when(userService.registerUser(any(UserRegistrationRequest.class)))
            .thenReturn(authResponse);
        
        // When
        ResponseEntity<AuthResponse> response = authController.registerUser(validRegistrationRequest);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token-here", response.getBody().getToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals(86400L, response.getBody().getExpiresIn());
        assertEquals(1L, response.getBody().getUser().getId());
        assertEquals("test@example.com", response.getBody().getUser().getEmail());
        assertEquals("Test User", response.getBody().getUser().getName());
    }
    
    @Test
    void registerUser_EmailAlreadyExists_ThrowsBadRequestException() {
        // Given
        when(userService.registerUser(any(UserRegistrationRequest.class)))
            .thenThrow(new BadRequestException("Email address is already in use"));
        
        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authController.registerUser(validRegistrationRequest);
        });
        
        assertEquals("Email address is already in use", exception.getMessage());
    }
    
    @Test
    void authenticateUser_ValidCredentials_ReturnsOkWithAuthResponse() {
        // Given
        when(userService.authenticateUser(any(LoginRequest.class)))
            .thenReturn(authResponse);
        
        // When
        ResponseEntity<AuthResponse> response = authController.authenticateUser(validLoginRequest);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token-here", response.getBody().getToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals(86400L, response.getBody().getExpiresIn());
        assertEquals(1L, response.getBody().getUser().getId());
        assertEquals("test@example.com", response.getBody().getUser().getEmail());
        assertEquals("Test User", response.getBody().getUser().getName());
    }
    
    @Test
    void authenticateUser_InvalidCredentials_ThrowsBadCredentialsException() {
        // Given
        when(userService.authenticateUser(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("Invalid email or password"));
        
        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authController.authenticateUser(validLoginRequest);
        });
        
        assertEquals("Invalid email or password", exception.getMessage());
    }
    
    @Test
    void registerUser_ServiceThrowsException_ExceptionPropagated() {
        // Given
        when(userService.registerUser(any(UserRegistrationRequest.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.registerUser(validRegistrationRequest);
        });
        
        assertEquals("Database error", exception.getMessage());
    }
    
    @Test
    void authenticateUser_ServiceThrowsException_ExceptionPropagated() {
        // Given
        when(userService.authenticateUser(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Authentication service error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.authenticateUser(validLoginRequest);
        });
        
        assertEquals("Authentication service error", exception.getMessage());
    }
}