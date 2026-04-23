package de.ni0.chronoscope.algorithm;

import java.util.List;

/**
 * Represents any kind of Data that should influence the decision-making of the next chosen task.
 * The {@link WeightDataProvider#calculate(DataProviderContext, List)} method is called for each
 * Tree-Node. The {@link WeightDataProvider#getWeight(Task)} method is then used to sort the tasks.
 *
 */
public interface WeightDataProvider {

    /**
     * Calculates the weight for each task.
     * @param ctx Can be used as a source for necessary data
     * @param tasks Tasks to be weighted
     */
    void calculate(DataProviderContext ctx, List<Task> tasks);

    /**
     * Called when sorting the tasks in the algorithm
     * @param task The task to get the weight for
     * @return MUST ALWAYS RETURN A VALUE BETWEEN 0 AND 1. THIS IS NORMALIZED.
     *         0 means low priority, 1 means high priority
     */
    double getWeight(Task task);

}
