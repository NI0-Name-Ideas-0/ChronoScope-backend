package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.DynamicTask;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TaskGraphNodeTest {

    private static final Instant START = Instant.parse("2026-04-26T08:00:00Z");

    @Test
    void equalsAndHashCodeUseTaskId() {
        TaskGraphNode first = node(1L);
        TaskGraphNode sameId = node(1L);
        TaskGraphNode differentId = node(2L);

        assertEquals(first, first);
        assertEquals(first, sameId);
        assertEquals(first.hashCode(), sameId.hashCode());
        assertNotEquals(first, differentId);
        assertNotEquals(first, "1");
    }

    @Test
    void toStringUsesTaskIdAndFallsBackToNullText() {
        assertEquals("1", node(1L).toString());
        assertEquals("null", node(null).toString());
    }

    @Test
    void delegatesTimeAndDurationAccessorsToTask() {
        TaskGraphNode node = node(1L);

        assertEquals(START, node.getStartAt());
        assertEquals(START.plus(Duration.ofHours(2)), node.getEndAt());
        assertEquals(Duration.ofHours(1), node.getDuration());
        assertSame(node.task(), node.task());
    }

    @Test
    void delegatesScopeDurationConstraintsToTask() {
        DynamicTask task = new DynamicTask();
        task.setId(1L);
        task.setStartAt(START);
        task.setEndAt(START.plus(Duration.ofHours(2)));
        task.setDuration(Duration.ofHours(1));
        task.setMinScopeDuration(Duration.ofMinutes(15));
        task.setMaxScopeDuration(Duration.ofMinutes(45));
        TaskGraphNode node = new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());

        assertEquals(Duration.ofMinutes(15), node.getMinScopeDuration());
        assertEquals(Duration.ofMinutes(45), node.getMaxScopeDuration());
    }

    private static TaskGraphNode node(Long id) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setStartAt(START);
        task.setEndAt(START.plus(Duration.ofHours(2)));
        task.setDuration(Duration.ofHours(1));
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }
}
