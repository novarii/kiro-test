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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for user management and authentication operations
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }
    
    /**
     * Register a new user with encrypted password
     * 
     * @param registrationRequest the user registration data
     * @return AuthResponse containing JWT token and user information
     * @throws BadRequestException if email already exists
     */
    public AuthResponse registerUser(UserRegistrationRequest registrationRequest) {
        logger.info("Attempting to register user with email: {}", registrationRequest.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", registrationRequest.getEmail());
            throw new BadRequestException("Email address is already in use");
        }
        
        // Create new user with encrypted password
        User user = new User();
        user.setEmail(registrationRequest.getEmail());
        user.setName(registrationRequest.getName());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        
        // Save user to database
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", savedUser.getId());
        
        // Generate JWT token
        String token = jwtTokenProvider.generateTokenFromUserId(savedUser.getId());
        
        // Create response
        UserResponse userResponse = convertToUserResponse(savedUser);
        return new AuthResponse(token, userResponse);
    }
    
    /**
     * Authenticate user and generate JWT token
     * 
     * @param loginRequest the login credentials
     * @return AuthResponse containing JWT token and user information
     * @throws BadCredentialsException if credentials are invalid
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        logger.info("Attempting to authenticate user with email: {}", loginRequest.getEmail());
        
        try {
            // Authenticate user credentials
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(authentication);
            
            // Get user details
            User user = getUserByEmail(loginRequest.getEmail());
            UserResponse userResponse = convertToUserResponse(user);
            
            logger.info("User authenticated successfully: {}", loginRequest.getEmail());
            return new AuthResponse(token, userResponse);
            
        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for email: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }
    
    /**
     * Get user by ID
     * 
     * @param userId the user ID
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        logger.debug("Fetching user by ID: {}", userId);
        
        return userRepository.findById(userId)
            .orElseThrow(() -> {
                logger.warn("User not found with ID: {}", userId);
                return new ResourceNotFoundException("User not found with ID: " + userId);
            });
    }
    
    /**
     * Get user by email
     * 
     * @param email the user email
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        
        return userRepository.findByEmail(email)
            .orElseThrow(() -> {
                logger.warn("User not found with email: {}", email);
                return new ResourceNotFoundException("User not found with email: " + email);
            });
    }
    
    /**
     * Get user response by ID
     * 
     * @param userId the user ID
     * @return UserResponse DTO
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(Long userId) {
        User user = getUserById(userId);
        return convertToUserResponse(user);
    }
    
    /**
     * Check if user exists by email
     * 
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Validate user exists and return user entity
     * Used for authorization checks in other services
     * 
     * @param userId the user ID to validate
     * @return User entity if valid
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User validateAndGetUser(Long userId) {
        return getUserById(userId);
    }
    
    /**
     * Get user with accounts loaded
     * 
     * @param userId the user ID
     * @return User entity with accounts
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserWithAccounts(Long userId) {
        logger.debug("Fetching user with accounts by ID: {}", userId);
        
        return userRepository.findByIdWithAccounts(userId)
            .orElseThrow(() -> {
                logger.warn("User not found with ID: {}", userId);
                return new ResourceNotFoundException("User not found with ID: " + userId);
            });
    }
    
    /**
     * Convert User entity to UserResponse DTO
     * 
     * @param user the User entity
     * @return UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getCreatedAt()
        );
    }
}