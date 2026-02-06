package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BlackoutException{
    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "AUTHORIZATION", message);
    }
}
