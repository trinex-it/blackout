package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class TokenRevokedException extends BlackoutException {
    public TokenRevokedException(String message) {
        super(HttpStatus.BAD_REQUEST, ExceptionCategory.UNAUTHORIZED, message);
    }
}