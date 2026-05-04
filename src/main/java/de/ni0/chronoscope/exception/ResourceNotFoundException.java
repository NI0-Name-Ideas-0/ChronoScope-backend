package de.ni0.chronoscope.exception;

/**
 * Exception thrown when a requested resource cannot be found within the caller's access boundary.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super("Resource not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
