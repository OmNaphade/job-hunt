package org.omnaphade.user_service.exception;

import org.omnaphade.user_service.dtos.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO("Access denied: insufficient permissions", 403, java.time.LocalDateTime.now()));
    }


    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {

        log.error("âŒ Resource not found: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ErrorResponseDTO(ex.getMessage(), 404, LocalDateTime.now()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicate(DuplicateResourceException ex) {

        log.error("âš ï¸ Duplicate resource: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ErrorResponseDTO(ex.getMessage(), 409, LocalDateTime.now()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(BadRequestException ex) {

        log.error("Bad request: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ErrorResponseDTO(ex.getMessage(), 400, LocalDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {

        return new ResponseEntity<>(
                new ErrorResponseDTO("Uploaded file exceeds the maximum allowed size", 400, LocalDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {

        log.error("ðŸ’¥ Unexpected error", ex);

        return new ResponseEntity<>(
                new ErrorResponseDTO("Internal Server Error", 500, LocalDateTime.now()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
