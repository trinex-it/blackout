package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BlackoutException {
    public UnauthorizedException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                message
        );
    }
}
