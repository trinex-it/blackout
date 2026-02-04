package it.trinex.blackout.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order  // Lowest order priority by default
public class BlackoutExceptionHandler {

    private void logBlackoutException(BlackoutException ex) {
        log.error("{} occurred: status={}, category={}, description={}",
            ex.getClass().getSimpleName(), ex.getStatus(), ex.getCategory(), ex.getDescription());
    }

    @ExceptionHandler(BlackoutException.class)
    public ResponseEntity<ExceptionResponseDTO> handleBlackoutException(BlackoutException ex) {
        logBlackoutException(ex);

        ExceptionResponseDTO response = new ExceptionResponseDTO(ex);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ExceptionResponseDTO> handleDuplicateKeyException(DuplicateKeyException ex) {
        logBlackoutException(ex);

        ExceptionResponseDTO response = new ExceptionResponseDTO(ex);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Intercetta gli errori di validazione (@Valid sui DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Itera su tutti i campi che hanno fallito la validazione
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage(); // Questo è il tuo message="..."
            errors.put(fieldName, errorMessage);
        });

        // Costruisci una risposta custom
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("messages", errors); // Qui avrai { "email": "Serve la mail", "password": "..." }

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponseDTO> handleAccessDeniedException(AccessDeniedException ex) {
        BlackoutException myEx = new BlackoutException(HttpStatus.FORBIDDEN, "AUTHORIZATION", "User is not authorized to access this resource");

        logBlackoutException(myEx);

        ExceptionResponseDTO response = new ExceptionResponseDTO(myEx);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}