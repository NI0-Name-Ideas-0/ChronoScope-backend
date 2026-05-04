package de.ni0.chronoscope.exception;

/**
 * Exception used by API endpoints that are intentionally exposed but not implemented yet.
 */
public class ApiNotImplementedException extends RuntimeException {
    public ApiNotImplementedException() {
        super("Not implemented yet");
    }
}
