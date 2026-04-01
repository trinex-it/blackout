package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class PasskeyNotFoundException extends BlackoutException{
    public PasskeyNotFoundException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                ExceptionCategory.PASSKEY,
                message
        );
    }
}
