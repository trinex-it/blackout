package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class PasswordlessEnabledException extends BlackoutException {
    public PasswordlessEnabledException(String message) {
        super(
                HttpStatus.FORBIDDEN,
                "PASSKEY",
                message
        );
    }
}
