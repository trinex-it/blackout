package it.trinex.blackout.exception;

import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends BlackoutException {
    public AccountNotActiveException(String description) {
        super(HttpStatus.UNAUTHORIZED, "ACCOUNT_NOT_ACTIVE", description);
    }
}
