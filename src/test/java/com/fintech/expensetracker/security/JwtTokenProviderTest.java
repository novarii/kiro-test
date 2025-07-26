package com.fintech.expensetracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "testSecretKeyForJwtTokenGenerationThatIsLongEnoughForHS512Algorithm");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", 86400000L);
    }
    
    @Test
    void generateToken_ValidAuthentication_ReturnsToken() {
        // Given
        UserPrincipal userPrincipal = new UserPrincipal(1L, "Test User", "test@example.com", "password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        
        // When
        String token = jwtTokenProvider.generateToken(authentication);
        
        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
    
    @Test
    void generateTokenFromUserId_ValidUserId_ReturnsToken() {
        // Given
        Long userId = 1L;
        
        // When
        String token = jwtTokenProvider.generateTokenFromUserId(userId);
        
        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
    
    @Test
    void getUserIdFromToken_ValidToken_ReturnsUserId() {
        // Given
        Long expectedUserId = 1L;
        String token = jwtTokenProvider.generateTokenFromUserId(expectedUserId);
        
        // When
        Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);
        
        // Then
        assertEquals(expectedUserId, actualUserId);
    }
    
    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtTokenProvider.generateTokenFromUserId(1L);
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        // Given
        String emptyToken = "";
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void getExpirationInSeconds_ReturnsCorrectValue() {
        // When
        long expirationInSeconds = jwtTokenProvider.getExpirationInSeconds();
        
        // Then
        assertEquals(86400L, expirationInSeconds); // 24 hours
    }
}