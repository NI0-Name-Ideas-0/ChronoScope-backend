package de.ni0.chronoscope.exception;

/**
 * Exception thrown when request data is syntactically valid but violates ChronoScope rules.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
