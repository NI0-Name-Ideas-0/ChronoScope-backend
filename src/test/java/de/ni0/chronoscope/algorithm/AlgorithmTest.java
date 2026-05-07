package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.WorkSlot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AlgorithmTest {

    private static final Instant START = Instant.parse("2026-04-26T08:00:00Z");

    @Test
    void planSchedulesSuccessorOnlyAfterAllDependenciesAreCompleted() {
        TaskGraphNode firstDependency = node(1L, Duration.ofHours(1), START.plus(Duration.ofHours(4)));
        TaskGraphNode secondDependency = node(2L, Duration.ofHours(1), START.plus(Duration.ofHours(4)));
        TaskGraphNode successor = node(3L, Duration.ofHours(1), START.plus(Duration.ofHours(4)));
        link(firstDependency, successor);
        link(secondDependency, successor);

        FixedWeightProvider provider = new FixedWeightProvider(Map.of(
                firstDependency, 3.0,
                secondDependency, 2.0,
                successor, 1.0));
        Algorithm algorithm = new Algorithm(List.of(provider));
        WorkSlot slot = slot(START, START.plus(Duration.ofHours(4)));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(firstDependency, secondDependency)),
                dependencyCounts(firstDependency, secondDependency, successor),
                remainingDurations(firstDependency, secondDependency, successor),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(3, result.size());
        assertScope(result.get(0), firstDependency, START, START.plus(Duration.ofHours(1)));
        assertScope(result.get(1), secondDependency, START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
        assertScope(result.get(2), successor, START.plus(Duration.ofHours(2)), START.plus(Duration.ofHours(3)));
    }

    @Test
    void planSplitsTaskAcrossConsecutiveSlots() {
        TaskGraphNode task = node(1L, Duration.ofMinutes(90), START.plus(Duration.ofHours(4)));
        WorkSlot firstSlot = slot(START, START.plus(Duration.ofHours(1)));
        WorkSlot secondSlot = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(firstSlot, secondSlot)),
                firstSlot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertScope(result.get(0), task, START, START.plus(Duration.ofHours(1)));
        assertScope(result.get(1), task, START.plus(Duration.ofHours(1)), START.plus(Duration.ofMinutes(90)));
    }

    @Test
    void planBacktracksAfterPartialScopeWhenTighterDeadlineWouldBeMissed() {
        TaskGraphNode longLooseTask = node(1L, Duration.ofHours(2), START.plus(Duration.ofHours(4)));
        TaskGraphNode urgentTask = node(2L, Duration.ofHours(1), START.plus(Duration.ofHours(1)));
        WorkSlot firstSlot = slot(START, START.plus(Duration.ofHours(1)));
        WorkSlot secondSlot = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(4)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(
                longLooseTask, 2.0,
                urgentTask, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(longLooseTask, urgentTask)),
                dependencyCounts(longLooseTask, urgentTask),
                remainingDurations(longLooseTask, urgentTask),
                new ExhaustingWorkSlotProvider(List.of(firstSlot, secondSlot)),
                firstSlot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertScope(result.get(0), urgentTask, START, START.plus(Duration.ofHours(1)));
        assertScope(result.get(1), longLooseTask, START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(3)));
    }

    @Test
    void planBacktrackingRestoresUnlockedSuccessorBeforeTryingAlternativePath() {
        TaskGraphNode dependency = node(1L, Duration.ofHours(1), START.plus(Duration.ofHours(4)));
        TaskGraphNode urgentTask = node(2L, Duration.ofHours(1), START.plus(Duration.ofHours(1)));
        TaskGraphNode successor = node(3L, Duration.ofHours(1), START.plus(Duration.ofHours(4)));
        link(dependency, successor);
        WorkSlot slot = slot(START, START.plus(Duration.ofHours(4)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(
                dependency, 2.0,
                urgentTask, 1.0,
                successor, 3.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(dependency, urgentTask)),
                dependencyCounts(dependency, urgentTask, successor),
                remainingDurations(dependency, urgentTask, successor),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(3, result.size());
        assertScope(result.get(0), urgentTask, START, START.plus(Duration.ofHours(1)));
        assertScope(result.get(1), dependency, START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
        assertScope(result.get(2), successor, START.plus(Duration.ofHours(2)), START.plus(Duration.ofHours(3)));
    }

    @Test
    void planReturnsNullWhenTaskCannotFitBeforeDeadlineFromCurrentTime() {
        TaskGraphNode impossibleTask = node(1L, Duration.ofHours(2), START.plus(Duration.ofHours(1)));
        FixedWeightProvider provider = new FixedWeightProvider(Map.of(impossibleTask, 1.0));
        WorkSlot slot = slot(START, START.plus(Duration.ofHours(4)));

        List<Scope> result = new Algorithm(List.of(provider)).plan(
                new ArrayList<>(List.of(impossibleTask)),
                dependencyCounts(impossibleTask),
                remainingDurations(impossibleTask),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNull(result);
        assertEquals(0, provider.calculateCalls);
    }

    @Test
    void planReturnsNullWhenWorkSlotsAreExhaustedBeforeTaskCompletes() {
        TaskGraphNode task = node(1L, Duration.ofHours(2), START.plus(Duration.ofHours(4)));
        WorkSlot slot = slot(START, START.plus(Duration.ofHours(1)));

        List<Scope> result = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0)))).plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNull(result);
    }

    @Test
    void planReturnsNullWhenCalledWithoutStartNodes() {
        WorkSlot slot = slot(START, START.plus(Duration.ofHours(1)));

        List<Scope> result = new Algorithm(List.of()).plan(
                new ArrayList<>(),
                new HashMap<>(),
                new HashMap<>(),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNull(result);
    }

    private static TaskGraphNode node(Long id, Duration duration, Instant deadline) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setEndAt(deadline);
        task.setStartAt(START);
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }

    private static void link(TaskGraphNode dependency, TaskGraphNode dependent) {
        dependent.dependencies().add(dependency);
        dependency.dependents().add(dependent);
    }

    private static WorkSlot slot(Instant startAt, Instant endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setId(startAt.toEpochMilli());
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        return slot;
    }

    private static Map<TaskGraphNode, Integer> dependencyCounts(TaskGraphNode... tasks) {
        Map<TaskGraphNode, Integer> dependencyCounts = new HashMap<>();
        for (TaskGraphNode task : tasks) {
            dependencyCounts.put(task, task.dependencies().size());
        }
        return dependencyCounts;
    }

    private static Map<TaskGraphNode, Duration> remainingDurations(TaskGraphNode... tasks) {
        Map<TaskGraphNode, Duration> remainingDurations = new HashMap<>();
        for (TaskGraphNode task : tasks) {
            remainingDurations.put(task, task.getDuration());
        }
        return remainingDurations;
    }

    private static void assertScope(Scope scope, TaskGraphNode task, Instant startAt, Instant endAt) {
        assertSame(task.task(), scope.getDynamicTask());
        assertEquals(startAt, scope.getStartAt());
        assertEquals(endAt, scope.getEndAt());
    }

    private static final class FixedWeightProvider implements WeightDataProvider {
        private final Map<TaskGraphNode, Double> weights;
        private int calculateCalls;

        private FixedWeightProvider(Map<TaskGraphNode, Double> weights) {
            this.weights = weights;
        }

        @Override
        public void calculate(DataProviderContext ctx, List<TaskGraphNode> tasks) {
            calculateCalls++;
        }

        @Override
        public double getWeight(TaskGraphNode task) {
            return weights.getOrDefault(task, 0.0);
        }
    }

    private static final class ExhaustingWorkSlotProvider extends WorkSlotProvider {
        private final List<WorkSlot> workSlots;

        private ExhaustingWorkSlotProvider(List<WorkSlot> workSlots) {
            super(workSlots);
            this.workSlots = workSlots;
        }

        @Override
        public WorkSlot getNextSlot(WorkSlot currentSlot) {
            if (currentSlot == null) {
                return this.workSlots.isEmpty() ? null : this.workSlots.getFirst();
            }

            int nextIndex = this.workSlots.indexOf(currentSlot) + 1;
            return nextIndex < this.workSlots.size() ? this.workSlots.get(nextIndex) : null;
        }
    }
}
