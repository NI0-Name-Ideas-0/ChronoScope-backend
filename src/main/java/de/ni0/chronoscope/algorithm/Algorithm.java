package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;
import org.springframework.util.comparator.Comparators;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Algorithm {

    static void main() {
        Algorithm algorithm = new Algorithm();
        List<Task> tasks = new ArrayList<>();
        Task task = new Task(0,0,
                Duration.of(2, ChronoUnit.DAYS),
                Instant.now(),
                Instant.now().plus(3, ChronoUnit.DAYS),
                new ArrayList<>(),
                new ArrayList<>());
        Task task3 = new Task(1, 0,
                Duration.of(1, ChronoUnit.DAYS),
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.DAYS),
                new ArrayList<>(),
                new ArrayList<>());
        Task task4 = new Task(2, 0,
                Duration.of(2, ChronoUnit.DAYS),
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.DAYS),
                new ArrayList<>(),
                new ArrayList<>());
        task3.dependencies().add(task4);
        task4.successors().add(task3);
        Task task5 = new Task(3, 0,
                Duration.of(2, ChronoUnit.DAYS),
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.DAYS),
                new ArrayList<>(),
                new ArrayList<>());
        task4.dependencies().add(task5);
        task5.successors().add(task4);
        tasks.add(task);
        tasks.add(task3);
        tasks.add(task4);
        tasks.add(task5);

        algorithm.computeCPM(tasks);
    }

    public List<Scope> plan(List<Task> allTasks, List<Task> tasks,
                            List<OrganizationSlot> slots, int slotIndex,
                            Duration elapsedSlotTime) {
        OrganizationSlot slot = slots.get(slotIndex);
        Instant currentTime = slot.getStart().plus(elapsedSlotTime);
        Duration remaining = slot.getDuration().minus(elapsedSlotTime);

        CPM cpm = new CPM(allTasks, currentTime);
        HashMap<Task, Long> taskWeights = new HashMap<>();
        Queue<Task> taskQueue = new PriorityQueue<>(Comparator.comparingLong(taskWeights::get));
        for (Task task : tasks) {
            taskWeights.put(task, getWeight(cpm, task));
            taskQueue.add(task);
        }

        Task task = taskQueue.poll();

        List<Scope> scopes = new ArrayList<>();

        List<Task> nextTasks = new ArrayList<>(tasks);
        nextTasks.remove(task);

        List<Scope> nextResult = plan(allTasks, );
        scopes.addAll(nextResult);

        return scopes;
    }

    public long getWeight(CPM cpm, Task task) {
        return cpm.getTaskData().get(task).getSlack().toMinutes();
    }

    public void computeCPM(List<Task> tasks) {
        CPM cpm = new CPM(tasks);
        Map<Task, CPM.TaskData> taskData = cpm.getTaskData();
        for (CPM.TaskData value : taskData.values()) {
            System.out.println(value.getSlack());
        }
    }

}
