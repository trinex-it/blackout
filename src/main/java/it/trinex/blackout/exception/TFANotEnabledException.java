package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class TFANotEnabledException extends BlackoutException{
    public TFANotEnabledException(String message) {
        super(HttpStatus.CONFLICT, ExceptionCategory.TFA_ERROR, message);
    }
}
