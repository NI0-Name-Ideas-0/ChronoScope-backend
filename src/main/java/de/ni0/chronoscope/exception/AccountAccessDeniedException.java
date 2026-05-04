package de.ni0.chronoscope.exception;

/**
 * Exception thrown when an authenticated identity tries to use an account or organization it cannot access.
 */
public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
