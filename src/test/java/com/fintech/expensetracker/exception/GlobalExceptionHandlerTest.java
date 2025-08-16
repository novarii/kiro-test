package com.fintech.expensetracker.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private WebRequest webRequest;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        webRequest = mock(WebRequest.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequest() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("testObject", "testField", "Test error message");
        
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError));
        when(ex.getMessage()).thenReturn("Validation failed");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleValidationExceptions(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input data");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getDetails()).containsEntry("testField", "Test error message");
    }

    @Test
    void handleBadRequestException_ShouldReturnBadRequest() {
        // Given
        BadRequestException ex = new BadRequestException("Invalid request data");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleBadRequestException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request data");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleResourceNotFoundException_ShouldReturnNotFound() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Resource Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleValidationException_WithValidationErrors_ShouldReturnBadRequest() {
        // Given
        Map<String, String> validationErrors = new HashMap<>();
        validationErrors.put("amount", "Amount must be positive");
        validationErrors.put("description", "Description is required");
        ValidationException ex = new ValidationException("Validation failed", validationErrors);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleValidationException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getDetails()).containsEntry("amount", "Amount must be positive");
        assertThat(response.getBody().getDetails()).containsEntry("description", "Description is required");
    }

    @Test
    void handleValidationException_WithoutValidationErrors_ShouldReturnBadRequest() {
        // Given
        ValidationException ex = new ValidationException("Simple validation error");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleValidationException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Simple validation error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void handleConstraintViolationException_ShouldReturnBadRequest() {
        // Given
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(propertyPath.toString()).thenReturn("amount");
        when(violation.getMessage()).thenReturn("must be positive");
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));
        when(ex.getMessage()).thenReturn("Constraint violation");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleConstraintViolationException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Constraint validation failed");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getDetails()).containsEntry("amount", "must be positive");
    }

    @Test
    void handleMethodArgumentTypeMismatchException_ShouldReturnBadRequest() {
        // Given
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
        when(ex.getValue()).thenReturn("invalid_value");
        when(ex.getName()).thenReturn("id");
        when(ex.getRequiredType()).thenReturn((Class) Long.class);
        when(ex.getMessage()).thenReturn("Type mismatch");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleMethodArgumentTypeMismatchException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Invalid Parameter");
        assertThat(response.getBody().getMessage()).contains("Invalid value 'invalid_value' for parameter 'id'");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleDataIntegrityViolationException_ShouldReturnConflict() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique index violation");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleDataIntegrityViolationException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Data Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("A record with this information already exists");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleDataIntegrityViolationException_GenericMessage_ShouldReturnConflict() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Some other constraint violation");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleDataIntegrityViolationException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Data Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("Data integrity constraint violation");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // Given
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAccessDeniedException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Access Denied");
        assertThat(response.getBody().getMessage()).isEqualTo("You don't have permission to access this resource");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleAuthenticationException_BadCredentials_ShouldReturnUnauthorized() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAuthenticationException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Authentication Failed");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials provided");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleGlobalException_ShouldReturnInternalServerError() {
        // Given
        RuntimeException ex = new RuntimeException("Unexpected error");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleGlobalException(ex, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void errorResponse_AllConstructors_ShouldWorkCorrectly() {
        // Test constructor without details
        GlobalExceptionHandler.ErrorResponse response1 = new GlobalExceptionHandler.ErrorResponse(
            null, 400, "Bad Request", "Test message", "/api/test"
        );
        
        assertThat(response1.getStatus()).isEqualTo(400);
        assertThat(response1.getError()).isEqualTo("Bad Request");
        assertThat(response1.getMessage()).isEqualTo("Test message");
        assertThat(response1.getPath()).isEqualTo("/api/test");
        assertThat(response1.getDetails()).isNull();

        // Test constructor with details
        Map<String, String> details = new HashMap<>();
        details.put("field", "error");
        
        GlobalExceptionHandler.ErrorResponse response2 = new GlobalExceptionHandler.ErrorResponse(
            null, 400, "Bad Request", "Test message", "/api/test", details
        );
        
        assertThat(response2.getDetails()).isEqualTo(details);
    }
}