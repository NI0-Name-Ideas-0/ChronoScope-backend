package de.ni0.chronoscope.algorithm;

import lombok.Data;

import java.time.Instant;

/**
 * Context value object passed to task-weight providers during a planning step.
 */
@Data
public class DataProviderContext {
    private final Instant currentTime;
}
