package de.ni0.chronoscope.algorithm.dataprovider;

import de.ni0.chronoscope.algorithm.DataProviderContext;
import de.ni0.chronoscope.algorithm.TaskGraphNode;
import de.ni0.chronoscope.algorithm.WeightDataProvider;
import de.ni0.chronoscope.model.Scope;

import java.util.List;

public class DifficultyDataProvider implements WeightDataProvider {
    private double plannedDifficulty;
    public static final double CUTOFF_FACTOR = 0.8;

    private double calculatePlannedDifficulty(List<Scope> plannedScopes) {
        double difficulty = 0;
        double summedWeights = 0;
        for (int i = 0; i < plannedScopes.size(); i++) {
            Scope scope = plannedScopes.get(plannedScopes.size() - i - 1);
            double taskDifficulty = scope.getDynamicTask().getDifficulty().getNormalizedDifficulty();
            double weight = Math.pow(CUTOFF_FACTOR, plannedScopes.size() - i);
            summedWeights += weight;
            difficulty += weight * taskDifficulty;
        }
        return (difficulty / summedWeights);
    }

    @Override
    public void calculate(DataProviderContext ctx, List<TaskGraphNode> tasks) {
        this.plannedDifficulty = this.calculatePlannedDifficulty(ctx.getPlannedScopes());
    }

    @Override
    public double getWeight(TaskGraphNode task) {
        double taskDifficulty = task.task().getDifficulty().getNormalizedDifficulty();
        return (1 - Math.pow(Math.E, -3 * Math.abs(taskDifficulty - this.plannedDifficulty)))*2;
    }
}
