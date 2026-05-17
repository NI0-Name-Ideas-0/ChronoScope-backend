package de.ni0.chronoscope.algorithm;

import java.time.Instant;

/**
 * An immutable time window occupied by a static-task occurrence.
 *
 * <p>The interval is right-open: {@code startAt} is inclusive, {@code endAt} is exclusive.
 * Used by {@link WorkSlotCarver} to carve out unavailable time from concrete work slots
 * before dynamic-task planning runs.</p>
 *
 * @param startAt start of the blocked window (inclusive)
 * @param endAt   end of the blocked window (exclusive)
 */
public record BlockedInterval(Instant startAt, Instant endAt) {
}
