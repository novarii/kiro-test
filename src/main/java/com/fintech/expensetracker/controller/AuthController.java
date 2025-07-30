package com.fintech.expensetracker.controller;

import com.fintech.expensetracker.dto.request.LoginRequest;
import com.fintech.expensetracker.dto.request.UserRegistrationRequest;
import com.fintech.expensetracker.dto.response.AuthResponse;
import com.fintech.expensetracker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final UserService userService;
    
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Register a new user
     * 
     * @param registrationRequest the user registration data
     * @return ResponseEntity containing AuthResponse with JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        logger.info("Registration request received for email: {}", registrationRequest.getEmail());
        
        AuthResponse authResponse = userService.registerUser(registrationRequest);
        
        logger.info("User registered successfully: {}", registrationRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }
    
    /**
     * Authenticate user and generate JWT token
     * 
     * @param loginRequest the login credentials
     * @return ResponseEntity containing AuthResponse with JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login request received for email: {}", loginRequest.getEmail());
        
        AuthResponse authResponse = userService.authenticateUser(loginRequest);
        
        logger.info("User authenticated successfully: {}", loginRequest.getEmail());
        return ResponseEntity.ok(authResponse);
    }
}