package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class InvalidRecoveryCodeException extends BlackoutException{
    public InvalidRecoveryCodeException(String message) {
        super(HttpStatus.BAD_REQUEST, ExceptionCategory.TFA_ERROR, message);
    }
}
