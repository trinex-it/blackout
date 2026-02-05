package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class InvalidTOTPCodeException extends BlackoutException{
    public InvalidTOTPCodeException(String message) {
        super(HttpStatus.UNAUTHORIZED, "2FA_ERROR", message);
    }
}
