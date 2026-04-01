package it.trinex.blackout.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BlackoutException extends RuntimeException {

    private final HttpStatus status;
    private final ExceptionCategory category;
    private final String description;

    public BlackoutException(HttpStatus status, ExceptionCategory category, String description) {
        super(String.format("Category: %s, Description: %s", category, description));
        this.status = status;
        this.category = category;
        this.description = description;
    }
}

