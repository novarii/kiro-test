package com.fintech.expensetracker.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RuntimeException {
    
    private Map<String, String> validationErrors;
    private List<String> errorMessages;
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }
    
    public ValidationException(String message, List<String> errorMessages) {
        super(message);
        this.errorMessages = errorMessages;
    }
    
    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
    
    public List<String> getErrorMessages() {
        return errorMessages;
    }
}