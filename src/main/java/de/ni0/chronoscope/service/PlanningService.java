package de.ni0.chronoscope.service;

import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.OrganizationSlotRepository;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlanningService {

    private OrganizationSlotRepository organizationSlotRepository;
    private TaskRepository taskRepository;
    private ScopeRepository scopeRepository;

    @Transactional
    public List<Scope> planScopes() {
        List<OrganizationSlot> slots = this.organizationSlotRepository.findAll();
        List<Task> tasks = this.taskRepository.findAll();

        Duration totalTaskDuration = tasks.stream()
                .map(Task::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
        Duration totalSlotDuration = slots.stream()
                .map(OrganizationSlot::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        if (totalTaskDuration.compareTo(totalSlotDuration) > 0) {
            throw new InsufficientSlotsException(
                    "Total task duration " + totalTaskDuration + " exceeds total slot duration " + totalSlotDuration);
        }

        List<Scope> scopes = new ArrayList<>();

        int currentSlot = 0;
        Duration slotDurationLeft = slots.get(currentSlot).getDuration();
        Instant nextScopeBeginTime = slots.get(currentSlot).getStart();

        for (Task task : tasks) {
            Duration durationLeft = task.getDuration();

            while (!durationLeft.isZero()) {
                if (slotDurationLeft.isZero()) {
                    currentSlot++;
                    slotDurationLeft = slots.get(currentSlot).getDuration();
                    nextScopeBeginTime = slots.get(currentSlot).getStart();
                }

                Duration scopeDuration = durationLeft.compareTo(slotDurationLeft) <= 0
                        ? durationLeft
                        : slotDurationLeft;

                scopes.add(new Scope(task, nextScopeBeginTime, scopeDuration));

                durationLeft = durationLeft.minus(scopeDuration);
                nextScopeBeginTime = nextScopeBeginTime.plus(scopeDuration);
                slotDurationLeft = slotDurationLeft.minus(scopeDuration);
            }
        }

        this.scopeRepository.deleteAll();
        this.scopeRepository.saveAll(scopes);

        return scopes;
    }
}
