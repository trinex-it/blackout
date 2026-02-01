package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when JWT token is invalid or expired.
 */
public class InvalidTokenException extends BlackoutException {
    public InvalidTokenException(String description) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", description);
    }
}
