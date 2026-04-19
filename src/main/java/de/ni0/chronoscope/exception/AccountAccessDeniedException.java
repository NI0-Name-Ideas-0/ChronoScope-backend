package de.ni0.chronoscope.exception;

public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
