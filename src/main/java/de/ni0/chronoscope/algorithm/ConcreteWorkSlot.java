package de.ni0.chronoscope.algorithm;

import java.time.Instant;

/**
 * A concrete time window produced by expanding a recurring {@link de.ni0.chronoscope.model.WorkSlot}
 * definition for a specific week.
 *
 * <p>This is the unit that {@link Algorithm} and {@link WorkSlotProvider} operate on.</p>
 *
 * @param startAt start of the concrete window (inclusive)
 * @param endAt   end of the concrete window (exclusive)
 * @param occurrenceId stable unique id for this expanded occurrence
 */
public record ConcreteWorkSlot(Instant startAt, Instant endAt, String occurrenceId) {
}
