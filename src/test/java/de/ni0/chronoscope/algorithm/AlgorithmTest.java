package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.algorithm.dataprovider.DifficultyDataProvider;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.Task;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(4)));

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
        ConcreteWorkSlot firstSlot = slot(START, START.plus(Duration.ofHours(1)));
        ConcreteWorkSlot secondSlot = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
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
        ConcreteWorkSlot firstSlot = slot(START, START.plus(Duration.ofHours(1)));
        ConcreteWorkSlot secondSlot = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(4)));
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
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(4)));
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
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(4)));

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
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(1)));

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
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(1)));

        List<Scope> result = new Algorithm(List.of()).plan(
                new ArrayList<>(),
                new HashMap<>(),
                new HashMap<>(),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertEquals(0, result.size());
    }

    @Test
    void planCapsEachScopeAtMaxScopeDuration() {
        // Task: 90 min total, max scope 30 min → expect 3 × 30-min scopes in a 2 h slot
        TaskGraphNode task = node(1L, Duration.ofMinutes(90), Duration.ofMinutes(1), Duration.ofMinutes(30),
                START.plus(Duration.ofHours(4)));
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(2)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(3, result.size());
        assertScope(result.get(0), task, START, START.plus(Duration.ofMinutes(30)));
        assertScope(result.get(1), task, START.plus(Duration.ofMinutes(30)), START.plus(Duration.ofMinutes(60)));
        assertScope(result.get(2), task, START.plus(Duration.ofMinutes(60)), START.plus(Duration.ofMinutes(90)));
    }

    @Test
    void planDoesNotPlanFinishedTask() {
        // Task: 90 min total, max scope 30 min → expect 3 × 30-min scopes in a 2 h slot
        TaskGraphNode task = node(1L, Duration.ofMinutes(90), Duration.ofMinutes(1), Duration.ofMinutes(90),
                START.plus(Duration.ofHours(4)));
        task.task().setElapsed(Duration.ofMinutes(90));
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(2)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void planContinuesWhenSomeTasksAreFinishedButOthersHaveRemainingWork() {
        TaskGraphNode finishedTask = node(1L, Duration.ofMinutes(60), START.plus(Duration.ofHours(4)));
        TaskGraphNode remainingTask = node(2L, Duration.ofMinutes(60), START.plus(Duration.ofHours(4)));
        finishedTask.task().setElapsed(Duration.ofMinutes(60));
        remainingTask.task().setElapsed(Duration.ofMinutes(0));
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(2)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(
                finishedTask, 2.0,
                remainingTask, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(finishedTask, remainingTask)),
                dependencyCounts(finishedTask, remainingTask),
                remainingDurations(finishedTask, remainingTask),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertScope(result.get(0), remainingTask, START, START.plus(Duration.ofMinutes(60)));
    }

    @Test
    void planAdvancesToNextSlotWhenRemainingTimeIsBelowMinScopeDuration() {
        // Slot 1 has only 20 min; task needs min 30 min → must skip slot 1 entirely and use slot 2
        TaskGraphNode task = node(1L, Duration.ofMinutes(60), Duration.ofMinutes(30), Duration.ofMinutes(60),
                START.plus(Duration.ofHours(4)));
        ConcreteWorkSlot shortSlot = slot(START, START.plus(Duration.ofMinutes(20)));
        ConcreteWorkSlot fullSlot = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(3)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(shortSlot, fullSlot)),
                shortSlot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertScope(result.get(0), task, START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
    }

    @Test
    void planAdvancesToNextSlotWhenCurrentSlotWouldLeaveInvalidTail() {
        // Slot 1 can start the task, but a 40-min scope would leave a 5-min tail below the 30-min min scope.
        // The valid plan is to leave slot 1 unused and schedule the full task in slot 2.
        TaskGraphNode task = node(1L, Duration.ofMinutes(45), Duration.ofMinutes(30), Duration.ofMinutes(45),
                START.plus(Duration.ofHours(4)));
        ConcreteWorkSlot awkwardSlot = slot(START, START.plus(Duration.ofMinutes(40)));
        ConcreteWorkSlot fullSlot = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(awkwardSlot, fullSlot)),
                awkwardSlot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertScope(result.get(0), task, START.plus(Duration.ofHours(1)), START.plus(Duration.ofMinutes(105)));
    }

    @Test
    void planShrinksScopeToEnsureTailMeetsMinScopeDuration() {
        // Task: 45 min, min 20 min, max 30 min.
        // Naive first scope = 30 min → tail = 15 min < 20 min (min).
        // Algorithm must shrink first scope to 25 min so tail = 20 min.
        TaskGraphNode task = node(1L, Duration.ofMinutes(45), Duration.ofMinutes(20), Duration.ofMinutes(30),
                START.plus(Duration.ofHours(4)));
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(1)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new WorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertScope(result.get(0), task, START, START.plus(Duration.ofMinutes(25)));
        assertScope(result.get(1), task, START.plus(Duration.ofMinutes(25)), START.plus(Duration.ofMinutes(45)));
    }

    @Test
    void planDoesNotScheduleTaskBeforeItsStartAt() {
        // Task's startAt is two days after the first slot. The algorithm must not place any scope
        // in the early slot and must instead wait until the later slot whose start >= task.startAt.
        Instant taskStart = START.plus(Duration.ofDays(2));
        TaskGraphNode task = node(1L, Duration.ofHours(2), taskStart, taskStart.plus(Duration.ofHours(4)));
        ConcreteWorkSlot earlySlot = slot(START, START.plus(Duration.ofHours(2)));
        ConcreteWorkSlot laterSlot = slot(taskStart, taskStart.plus(Duration.ofHours(4)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(earlySlot, laterSlot)),
                earlySlot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertScope(result.get(0), task, taskStart, taskStart.plus(Duration.ofHours(2)));
    }

    @Test
    void planDoesNotScheduleTaskBeforeItsStartAt2() {
        // Task's startAt is two days after the first slot. The algorithm must not place any scope
        // in the early slot and must instead wait until the later slot whose start >= task.startAt.
        Instant taskStart = START.plus(Duration.ofHours(2));
        TaskGraphNode task = node(1L, Duration.ofHours(2), taskStart, taskStart.plus(Duration.ofHours(8)));
        ConcreteWorkSlot earlySlot = slot(START, START.plus(Duration.ofHours(8)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(earlySlot)),
                earlySlot,
                START,
                new ArrayList<>());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertScope(result.getFirst(), task, taskStart, taskStart.plus(Duration.ofHours(2)));
    }


    @Test
    void planDoesRespecComplexity() {
        TaskGraphNode task = node(1L, Duration.ofHours(4), START, START.plus(Duration.ofHours(8)));
        task.task().setDifficulty(Task.Difficulty.TRIVIAL);
        task.task().setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.task().setMaxScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        TaskGraphNode task2 = node(2L, Duration.ofHours(2), START, START.plus(Duration.ofHours(8)));
        task2.task().setDifficulty(Task.Difficulty.EXTREME);
        task2.task().setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task2.task().setMaxScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        ConcreteWorkSlot slot = slot(START, START.plus(Duration.ofHours(12)));

        List<WeightDataProvider> providers = List.of(
                new CPMDataProvider(),
                new DifficultyDataProvider()
        );
        Algorithm algorithm = new Algorithm(providers);

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task, task2)),
                dependencyCounts(task, task2),
                remainingDurations(task, task2),
                new ExhaustingWorkSlotProvider(List.of(slot)),
                slot,
                START,
                new ArrayList<>());

        assertNotNull(result);
    }

    @Test
    void planReturnsNullWhenNoSlotExistsOnOrAfterTaskStartAt() {
        // Only slot is before the task's startAt → impossible to schedule → must return null.
        Instant taskStart = START.plus(Duration.ofDays(2));
        TaskGraphNode task = node(1L, Duration.ofHours(1), taskStart, taskStart.plus(Duration.ofHours(4)));
        ConcreteWorkSlot onlySlot = slot(START, START.plus(Duration.ofHours(2)));
        Algorithm algorithm = new Algorithm(List.of(new FixedWeightProvider(Map.of(task, 1.0))));

        List<Scope> result = algorithm.plan(
                new ArrayList<>(List.of(task)),
                dependencyCounts(task),
                remainingDurations(task),
                new ExhaustingWorkSlotProvider(List.of(onlySlot)),
                onlySlot,
                START,
                new ArrayList<>());

        assertNull(result);
    }

    private static TaskGraphNode node(Long id, Duration duration, Instant deadline) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setEndAt(deadline);
        task.setStartAt(START);
        task.setMinScopeDuration(Duration.ofMinutes(1));
        task.setMaxScopeDuration(duration);
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }

    private static TaskGraphNode node(Long id, Duration duration, Instant startAt, Instant deadline) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setStartAt(startAt);
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setEndAt(deadline);
        task.setMinScopeDuration(Duration.ofMinutes(1));
        task.setMaxScopeDuration(duration);
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }

    private static TaskGraphNode node(Long id, Duration duration, Duration minScope, Duration maxScope, Instant deadline) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setEndAt(deadline);
        task.setStartAt(START);
        task.setMinScopeDuration(minScope);
        task.setMaxScopeDuration(maxScope);
        return new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>());
    }

    private static void link(TaskGraphNode dependency, TaskGraphNode dependent) {
        dependent.dependencies().add(dependency);
        dependency.dependents().add(dependent);
    }

    private static ConcreteWorkSlot slot(Instant startAt, Instant endAt) {
        return new ConcreteWorkSlot(startAt, endAt, startAt.toString());
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
            remainingDurations.put(task, task.getDuration().minus(task.task().getElapsed()));
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
        private final List<ConcreteWorkSlot> workSlots;

        private ExhaustingWorkSlotProvider(List<ConcreteWorkSlot> workSlots) {
            super(workSlots);
            this.workSlots = workSlots;
        }

        @Override
        public ConcreteWorkSlot getNextSlot(ConcreteWorkSlot currentSlot) {
            if (currentSlot == null) {
                return this.workSlots.isEmpty() ? null : this.workSlots.getFirst();
            }

            int nextIndex = this.workSlots.indexOf(currentSlot) + 1;
            return nextIndex < this.workSlots.size() ? this.workSlots.get(nextIndex) : null;
        }
    }
}
