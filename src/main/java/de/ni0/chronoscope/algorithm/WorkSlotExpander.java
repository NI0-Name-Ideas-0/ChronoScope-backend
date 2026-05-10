package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.WorkSlot;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Expands recurring weekly {@link WorkSlot} definitions into concrete {@link ConcreteWorkSlot}
 * windows for a given planning horizon.
 *
 * <p>Stored times are interpreted as German local time. Java's Europe/Berlin zone rules
 * automatically apply CET/CEST offsets for winter and summer time.</p>
 */
@Component
public class WorkSlotExpander {

    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Berlin");

    /**
     * Expands all recurring slots into concrete windows between {@code now} and {@code horizon}.
     *
     * <p>Each slot recurs once per week on its {@code dayOfWeek} at its {@code startTime}/{@code endTime}.
     * Windows that end at or before {@code now} are excluded. The result is sorted by
     * {@code startAt} ascending.</p>
     *
     * @param slots   recurring work-slot definitions
     * @param now     the earliest point in time to schedule from
     * @param horizon the latest point in time to schedule up to (inclusive week boundary)
     * @return sorted list of concrete time windows
     */
    public List<ConcreteWorkSlot> expand(List<WorkSlot> slots, Instant now, Instant horizon) {
        List<ConcreteWorkSlot> result = new ArrayList<>();
        LocalDate startDate = now.atZone(PLANNING_ZONE).toLocalDate();
        LocalDate endDate = horizon.atZone(PLANNING_ZONE).toLocalDate();

        for (WorkSlot slot : slots) {
            LocalDate occurrence = startDate.with(TemporalAdjusters.nextOrSame(slot.getDayOfWeek()));

            while (!occurrence.isAfter(endDate)) {
                Instant slotStart = occurrence.atTime(slot.getStartTime()).atZone(PLANNING_ZONE).toInstant();
                Instant slotEnd = occurrence.atTime(slot.getEndTime()).atZone(PLANNING_ZONE).toInstant();

                if (slotEnd.isAfter(now)) {
                    Instant effectiveStart = slotStart.isBefore(now) ? now : slotStart;
                    result.add(new ConcreteWorkSlot(effectiveStart, slotEnd, occurrenceId(slot, occurrence)));
                }

                occurrence = occurrence.plusWeeks(1);
            }
        }

        result.sort(Comparator.comparing(ConcreteWorkSlot::startAt));
        return result;
    }

    private String occurrenceId(WorkSlot slot, LocalDate occurrence) {
        String slotId = slot.getId() == null ? Integer.toHexString(System.identityHashCode(slot)) : slot.getId().toString();
        return slotId + "@" + occurrence;
    }
}
