package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends BlackoutException {
    public PasswordMismatchException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", message);
    }
}
