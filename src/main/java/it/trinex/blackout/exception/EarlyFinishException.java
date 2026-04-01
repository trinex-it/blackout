package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class EarlyFinishException extends BlackoutException {
    public EarlyFinishException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                ExceptionCategory.PASSKEY,
                message
        );
    }
}
