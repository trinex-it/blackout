package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class TFAAlreadyEnabledException extends BlackoutException{
    public TFAAlreadyEnabledException(String message) {
        super(HttpStatus.CONFLICT, ExceptionCategory.TFA_ERROR, message);
    }
}
