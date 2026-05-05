package de.ni0.chronoscope.algorithm;

import java.util.List;

/**
 * Provides normalized task weights used by the planner when choosing the next task branch.
 *
 * <p>{@link #calculate(DataProviderContext, List)} is called before every sort so providers can
 * precompute graph-wide data for the current planning cursor.</p>
 */
public interface WeightDataProvider {

    /**
     * Precomputes data needed to weight the currently ready tasks.
     *
     * @param ctx current planner context
     * @param tasks tasks that may be selected next
     */
    void calculate(DataProviderContext ctx, List<TaskGraphNode> tasks);

    /**
     * Returns the normalized priority for a task.
     *
     * @param task task to score
     * @return a value from {@code 0.0} (low priority) to {@code 1.0} (high priority)
     */
    double getWeight(TaskGraphNode task);

}
