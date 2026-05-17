package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.Scope;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Context value object passed to task-weight providers during a planning step.
 */
@Data
public class DataProviderContext {
    private final Instant currentTime;
    private final List<Scope> plannedScopes;
    private final Map<TaskGraphNode, Duration> remainingTaskDurations;
}
