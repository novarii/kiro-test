package com.fintech.expensetracker.service;

import com.fintech.expensetracker.dto.request.LoginRequest;
import com.fintech.expensetracker.dto.request.UserRegistrationRequest;
import com.fintech.expensetracker.dto.response.AuthResponse;
import com.fintech.expensetracker.dto.response.UserResponse;
import com.fintech.expensetracker.entity.User;
import com.fintech.expensetracker.exception.BadRequestException;
import com.fintech.expensetracker.exception.ResourceNotFoundException;
import com.fintech.expensetracker.repository.UserRepository;
import com.fintech.expensetracker.security.JwtTokenProvider;
import com.fintech.expensetracker.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private UserRegistrationRequest registrationRequest;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("encodedPassword");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setName("Test User");
        registrationRequest.setPassword("password123");
        
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }
    
    @Test
    void registerUser_ValidRequest_ReturnsAuthResponse() {
        // Given
        when(userRepository.existsByEmail(registrationRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registrationRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateTokenFromUserId(testUser.getId())).thenReturn("jwt-token");
        
        // When
        AuthResponse response = userService.registerUser(registrationRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getName()).isEqualTo("Test User");
        
        verify(userRepository).existsByEmail(registrationRequest.getEmail());
        verify(passwordEncoder).encode(registrationRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateTokenFromUserId(testUser.getId());
    }
    
    @Test
    void registerUser_EmailAlreadyExists_ThrowsBadRequestException() {
        // Given
        when(userRepository.existsByEmail(registrationRequest.getEmail())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email address is already in use");
        
        verify(userRepository).existsByEmail(registrationRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateTokenFromUserId(anyLong());
    }
    
    @Test
    void authenticateUser_ValidCredentials_ReturnsAuthResponse() {
        // Given
        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        
        // When
        AuthResponse response = userService.authenticateUser(loginRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }
    
    @Test
    void authenticateUser_InvalidCredentials_ThrowsBadCredentialsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // When & Then
        assertThatThrownBy(() -> userService.authenticateUser(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(userRepository, never()).findByEmail(anyString());
    }
    
    @Test
    void getUserById_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.getUserById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void getUserById_NonExistingUser_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void getUserByEmail_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.getUserByEmail("test@example.com");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
        
        verify(userRepository).findByEmail("test@example.com");
    }
    
    @Test
    void getUserByEmail_NonExistingUser_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserByEmail("test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with email: test@example.com");
        
        verify(userRepository).findByEmail("test@example.com");
    }
    
    @Test
    void getUserResponseById_ExistingUser_ReturnsUserResponse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        UserResponse result = userService.getUserResponseById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void getUserResponseById_NonExistingUser_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserResponseById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        
        // When
        boolean result = userService.existsByEmail("test@example.com");
        
        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("test@example.com");
    }
    
    @Test
    void existsByEmail_NonExistingEmail_ReturnsFalse() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        
        // When
        boolean result = userService.existsByEmail("test@example.com");
        
        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail("test@example.com");
    }
    
    @Test
    void validateAndGetUser_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.validateAndGetUser(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void validateAndGetUser_NonExistingUser_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.validateAndGetUser(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void getUserWithAccounts_ExistingUser_ReturnsUserWithAccounts() {
        // Given
        when(userRepository.findByIdWithAccounts(1L)).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.getUserWithAccounts(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).findByIdWithAccounts(1L);
    }
    
    @Test
    void getUserWithAccounts_NonExistingUser_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findByIdWithAccounts(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserWithAccounts(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 1");
        
        verify(userRepository).findByIdWithAccounts(1L);
    }
    
    @Test
    void registerUser_SavedUserHasCorrectProperties() {
        // Given
        when(userRepository.existsByEmail(registrationRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registrationRequest.getPassword())).thenReturn("encodedPassword");
        when(jwtTokenProvider.generateTokenFromUserId(anyLong())).thenReturn("jwt-token");
        
        // Capture the user being saved
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            savedUser.setCreatedAt(LocalDateTime.now());
            savedUser.setUpdatedAt(LocalDateTime.now());
            return savedUser;
        });
        
        // When
        AuthResponse response = userService.registerUser(registrationRequest);
        
        // Then
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getName().equals("Test User") &&
            user.getPassword().equals("encodedPassword")
        ));
    }
    
    @Test
    void authenticateUser_UserNotFoundAfterAuthentication_ThrowsResourceNotFoundException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.authenticateUser(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with email: test@example.com");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }
}