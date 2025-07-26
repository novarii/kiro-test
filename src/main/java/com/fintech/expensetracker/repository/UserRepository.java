package com.fintech.expensetracker.repository;

import com.fintech.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations
 * Provides email lookup methods for authentication and user management
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email address
     * Used for authentication and user lookup
     * 
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user exists with the given email
     * Used for registration validation to prevent duplicate emails
     * 
     * @param email the email address to check
     * @return true if user exists with this email, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find user by email with accounts eagerly loaded
     * Useful when we need user data along with their accounts
     * 
     * @param email the email address to search for
     * @return Optional containing the user with accounts if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.email = :email")
    Optional<User> findByEmailWithAccounts(@Param("email") String email);
    
    /**
     * Find user by ID with accounts eagerly loaded
     * Useful when we need user data along with their accounts
     * 
     * @param id the user ID to search for
     * @return Optional containing the user with accounts if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.id = :id")
    Optional<User> findByIdWithAccounts(@Param("id") Long id);
}