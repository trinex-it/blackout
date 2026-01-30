package it.trinex.nnh.exception;

import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends NNHException {
    public AccountNotActiveException(String description) {
        super(HttpStatus.UNAUTHORIZED, "ACCOUNT_NOT_ACTIVE", description);
    }
}
