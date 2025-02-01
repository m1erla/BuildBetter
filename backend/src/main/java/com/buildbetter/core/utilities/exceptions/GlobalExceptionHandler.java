package com.renovatipoint.core.utilities.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Set;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<String> handleTransactionSystemException(TransactionSystemException ex) {
        logger.error("Transaction could not be completed", ex);

        Throwable cause = ex.getRootCause();
        if (cause instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) cause;
            Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();
            StringBuilder errorMessage = new StringBuilder();
            for (ConstraintViolation<?> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            logger.error("Constraint violations: {}", errorMessage);
            return ResponseEntity.badRequest().body("Validation error: " + errorMessage);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }
}
