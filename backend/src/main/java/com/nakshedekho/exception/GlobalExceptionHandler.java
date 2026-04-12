package com.nakshedekho.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        org.springframework.dao.DataAccessException.class,
        jakarta.persistence.PersistenceException.class,
        org.hibernate.HibernateException.class
    })
    public ResponseEntity<Map<String, String>> handleDatabaseExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "We encountered a temporary system issue. Please refresh the page and try again."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage();
        // If the error message looks like an internal Java/Hibernate technical error, sanitize it.
        if (msg != null && (msg.contains("com.nakshedekho") || msg.contains("Null value") || msg.contains("Exception") || msg.contains("nested exception"))) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong on our end. Please try again or contact support if the issue persists."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", msg != null ? msg : "Invalid request. Please check your inputs."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed. Please check your inputs.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", errorMsg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An internal error occurred. Please try again."));
    }
}
