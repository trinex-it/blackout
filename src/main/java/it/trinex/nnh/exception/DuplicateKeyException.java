package it.trinex.nnh.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a duplicate key constraint violation occurs.
 * Typically used when attempting to insert a record with a unique key that already exists.
 */
public class DuplicateKeyException extends NNHException {

    /**
     * Creates a duplicate key exception with a custom description.
     *
     * @param description Human-readable description of what field/value is duplicated
     */
    public DuplicateKeyException(String description) {
        super(HttpStatus.CONFLICT, "DUPLICATE_KEY", description);
    }

    /**
     * Creates a duplicate key exception for a specific field and value.
     *
     * @param fieldName  The name of the field that is duplicated (e.g., "email", "username")
     * @param fieldValue The value that already exists
     */
    public DuplicateKeyException(String fieldName, String fieldValue) {
        super(HttpStatus.CONFLICT, "DUPLICATE_KEY",
              String.format("An entry with %s '%s' already exists", fieldName, fieldValue));
    }
}
