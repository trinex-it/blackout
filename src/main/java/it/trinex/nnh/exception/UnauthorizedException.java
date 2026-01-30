package it.trinex.nnh.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends NNHException{
    public UnauthorizedException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                message
        );
    }
}
