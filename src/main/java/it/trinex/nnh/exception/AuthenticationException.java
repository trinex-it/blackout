package it.trinex.nnh.exception;

/**
 * Base authentication exception.
 *
 * <p>Thrown when authentication fails for any reason (invalid credentials, inactive account, etc.).</p>
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
