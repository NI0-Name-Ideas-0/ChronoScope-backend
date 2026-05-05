package de.ni0.chronoscope.exception;

/**
 * Exception thrown when the planner cannot fit all required work into the available slots.
 */
public class InsufficientSlotsException extends RuntimeException {
    public InsufficientSlotsException(String message) {
        super(message);
    }
}
