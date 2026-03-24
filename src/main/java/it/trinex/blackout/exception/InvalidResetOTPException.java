package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class InvalidResetOTPException extends BlackoutException {
    public InvalidResetOTPException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_OTP", message);
    }
}
