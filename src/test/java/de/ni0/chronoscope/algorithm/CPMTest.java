package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.DynamicTask;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CPMTest {

    private static final Instant START = Instant.parse("2026-04-26T08:00:00Z");

    @Test
    void calculatesEarliestStartFromLatestDependencyFinish() {
        TaskGraphNode longDependency = node(1L, Duration.ofHours(2), null);
        TaskGraphNode shortDependency = node(2L, Duration.ofMinutes(30), null);
        TaskGraphNode join = node(3L, Duration.ofHours(1), START.plus(Duration.ofHours(6)));
        link(longDependency, join);
        link(shortDependency, join);

        CPM.TaskData joinData = new CPM(List.of(longDependency, shortDependency, join),
                Map.of(longDependency, longDependency.getRemaining(), shortDependency, shortDependency.getRemaining(),
                        join, join.getRemaining()),
                START)
                .getTaskData()
                .get(join);

        assertEquals(START.plus(Duration.ofHours(2)), joinData.earliestStart());
        assertEquals(START.plus(Duration.ofHours(3)), joinData.earliestFinish());
    }

    @Test
    void calculatesLatestFinishFromEarliestSuccessorConstraint() {
        TaskGraphNode root = node(1L, Duration.ofHours(1), null);
        TaskGraphNode earlySuccessor = node(2L, Duration.ofHours(1), START.plus(Duration.ofHours(3)));
        TaskGraphNode lateSuccessor = node(3L, Duration.ofHours(1), START.plus(Duration.ofHours(6)));
        link(root, earlySuccessor);
        link(root, lateSuccessor);

        CPM.TaskData rootData = new CPM(List.of(root, earlySuccessor, lateSuccessor),
                Map.of(root, root.getRemaining(), earlySuccessor, earlySuccessor.getRemaining(),
                        lateSuccessor, lateSuccessor.getRemaining()), START)
                .getTaskData()
                .get(root);

        assertEquals(START.plus(Duration.ofHours(2)), rootData.latestFinish());
        assertEquals(START.plus(Duration.ofHours(1)), rootData.latestStart());
        assertEquals(Duration.ofHours(1), rootData.getSlack());
    }

    @Test
    void leavesLatestFinishUnboundedWhenTaskHasNoDeadlineOrSuccessors() {
        TaskGraphNode task = node(1L, Duration.ofHours(1), null);

        CPM.TaskData data = new CPM(List.of(task), Map.of(task, task.getRemaining()), START).getTaskData().get(task);

        assertEquals(Instant.MAX, data.latestFinish());
        assertEquals(START, data.earliestStart());
        assertEquals(START.plus(Duration.ofHours(1)), data.earliestFinish());
    }

    private static TaskGraphNode node(Long id, Duration duration, Instant deadline) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setStartAt(START);
        task.setEndAt(deadline);
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }

    private static void link(TaskGraphNode dependency, TaskGraphNode dependent) {
        dependent.dependencies().add(dependency);
        dependency.dependents().add(dependent);
    }
}
