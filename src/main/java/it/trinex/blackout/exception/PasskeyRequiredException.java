package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class PasskeyRequiredException extends BlackoutException {
    public PasskeyRequiredException(String message) {
        super(
                HttpStatus.FORBIDDEN,
                ExceptionCategory.PASSKEY,
                message
        );
    }
}
