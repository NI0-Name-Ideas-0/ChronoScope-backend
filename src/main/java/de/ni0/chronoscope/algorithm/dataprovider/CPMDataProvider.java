package de.ni0.chronoscope.algorithm.dataprovider;

import de.ni0.chronoscope.algorithm.CPM;
import de.ni0.chronoscope.algorithm.DataProviderContext;
import de.ni0.chronoscope.algorithm.TaskGraphNode;
import de.ni0.chronoscope.algorithm.WeightDataProvider;

import java.time.Duration;
import java.util.List;

/**
 * DataProvider for CPM
 */
public class CPMDataProvider implements WeightDataProvider {
    private CPM cpm;
    private long minSlack;
    private long maxSlack;

    /**
     * Calculates the min and max slack of all tasks.
     * @param ctx Can be used as a source for necessary data
     * @param tasks Tasks to be weighted
     */
    @Override
    public void calculate(DataProviderContext ctx, List<TaskGraphNode> tasks) {
        this.cpm = new CPM(tasks, ctx.getCurrentTime());
        this.minSlack = Long.MAX_VALUE;
        this.maxSlack = Long.MIN_VALUE;
        for (CPM.TaskData data : this.cpm.getTaskData().values()) {
            this.minSlack = Math.min(this.minSlack, data.getSlack().toMinutes());
            this.maxSlack = Math.max(this.maxSlack, data.getSlack().toMinutes());
        }
    }

    /**
     * The weight is normalized using: (taskSlack - minSlack) / (maxSlack - minSlack)
     * @param task The task to get the weight for
     * @return The weight
     */
    @Override
    public double getWeight(TaskGraphNode task) {
        Duration slack = this.cpm.getTaskData().get(task).getSlack();
        double slackMinutes = slack.toMinutes();
        if (this.maxSlack == this.minSlack) {
            return 1;
        }
        double weight = (slackMinutes - this.minSlack) / (this.maxSlack - this.minSlack);
        return 1 - weight;
    }
}
