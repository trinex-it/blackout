package it.trinex.nnh.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NNHException extends RuntimeException {

    private final HttpStatus status;
    private final String category;
    private final String description;

    public NNHException(HttpStatus status, String category, String description) {
        super(String.format("Category: %s, Description: %s", category, description));
        this.status = status;
        this.category = category;
        this.description = description;
    }
}

