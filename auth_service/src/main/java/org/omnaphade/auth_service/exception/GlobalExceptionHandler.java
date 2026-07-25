package org.omnaphade.auth_service.exception;

import org.omnaphade.auth_service.dtos.ErrorResponseDTO;
import org.omnaphade.auth_service.exception.AuthenticationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import java.time.ZoneOffset;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        log.warn("🚫 Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(
            new ErrorResponseDTO("Access denied: insufficient permissions", 403, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleSpringAuthFailed(AuthenticationException ex) {
        return new ResponseEntity<>(
            new ErrorResponseDTO(ex.getMessage(), 401, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthFailed(AuthenticationFailedException ex) {

        log.error("🔒 Authentication failed: {}", ex.getMessage());

        return new ResponseEntity<>(
            new ErrorResponseDTO(ex.getMessage(), 401, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {

        log.error("❌ Resource not found: {}", ex.getMessage());

        return new ResponseEntity<>(
            new ErrorResponseDTO(ex.getMessage(), 404, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicate(DuplicateResourceException ex) {

        log.error("⚠️ Duplicate resource: {}", ex.getMessage());

        return new ResponseEntity<>(
            new ErrorResponseDTO(ex.getMessage(), 409, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        log.warn("Validation failed: {}", errors);
        return new ResponseEntity<>(
            new ErrorResponseDTO("Validation failed: " + errors, 400, LocalDateTime.now(ZoneOffset.UTC)),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(
            new ErrorResponseDTO(ex.getMessage(), 400, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {

        log.error("💥 Unexpected error", ex);

        return new ResponseEntity<>(
            new ErrorResponseDTO("Internal Server Error", 500, LocalDateTime.now(ZoneOffset.UTC)),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
