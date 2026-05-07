package de.ni0.chronoscope.algorithm.dataprovider;

import de.ni0.chronoscope.algorithm.DataProviderContext;
import de.ni0.chronoscope.algorithm.TaskGraphNode;
import de.ni0.chronoscope.model.DynamicTask;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CPMDataProviderTest {

    private static final Instant START = Instant.parse("2026-04-26T08:00:00Z");

    @Test
    void givesHigherWeightToTaskWithLowerSlack() {
        TaskGraphNode urgent = node(1L, Duration.ofHours(1), START.plus(Duration.ofHours(1)));
        TaskGraphNode relaxed = node(2L, Duration.ofHours(1), START.plus(Duration.ofHours(3)));
        CPMDataProvider provider = new CPMDataProvider();

        provider.calculate(new DataProviderContext(START, new ArrayList<>()), List.of(urgent, relaxed));

        assertEquals(1.0, provider.getWeight(urgent), 0.000_001);
        assertEquals(0.0, provider.getWeight(relaxed), 0.000_001);
    }

    @Test
    void givesEqualMaxWeightWhenAllTasksHaveSameSlack() {
        TaskGraphNode first = node(1L, Duration.ofHours(1), START.plus(Duration.ofHours(2)));
        TaskGraphNode second = node(2L, Duration.ofHours(1), START.plus(Duration.ofHours(2)));
        CPMDataProvider provider = new CPMDataProvider();

        provider.calculate(new DataProviderContext(START, new ArrayList<>()), List.of(first, second));

        assertEquals(1.0, provider.getWeight(first), 0.000_001);
        assertEquals(1.0, provider.getWeight(second), 0.000_001);
    }

    private static TaskGraphNode node(Long id, Duration duration, Instant deadline) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setStartAt(START);
        task.setEndAt(deadline);
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }
}
