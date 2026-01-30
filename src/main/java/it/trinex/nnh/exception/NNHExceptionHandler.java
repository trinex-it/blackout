package it.trinex.nnh.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order  // Lowest order priority by default
public class NNHExceptionHandler {

    private void logNNHException(NNHException ex) {
        log.error("{} occurred: status={}, category={}, description={}",
            ex.getClass().getSimpleName(), ex.getStatus(), ex.getCategory(), ex.getDescription());
    }

    @ExceptionHandler(NNHException.class)
    public ResponseEntity<ExceptionResponseDTO> handleNNHException(NNHException ex) {
        logNNHException(ex);

        ExceptionResponseDTO response = new ExceptionResponseDTO(ex);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }
}