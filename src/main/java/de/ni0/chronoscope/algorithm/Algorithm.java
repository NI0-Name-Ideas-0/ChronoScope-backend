package de.ni0.chronoscope.algorithm;

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

    private record AlgTask(Task task, int dependencies) {

    }

    public void computeCPM(List<Task> tasks) {
        CPM cpm = new CPM(tasks);
        Map<Task, CPM.TaskData> taskData = cpm.getTaskData();
        for (CPM.TaskData value : taskData.values()) {
            System.out.println(value.getSlack());
        }
    }

}
