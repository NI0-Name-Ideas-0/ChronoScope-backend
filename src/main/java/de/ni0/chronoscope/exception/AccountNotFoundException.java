package de.ni0.chronoscope.exception;

/**
 * Exception thrown when an account lookup fails.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Account not found");
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
