package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.WorkSlot;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WorkSlotExpanderTest {

    private final WorkSlotExpander expander = new WorkSlotExpander();

    @Test
    void expandConvertsWinterBerlinLocalTimeToUtcInstant() {
        List<ConcreteWorkSlot> result = expander.expand(
                List.of(slot(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                Instant.parse("2026-01-05T00:00:00Z"),
                Instant.parse("2026-01-05T23:00:00Z"));

        assertEquals(1, result.size());
        assertEquals(Instant.parse("2026-01-05T07:00:00Z"), result.getFirst().startAt());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.getFirst().endAt());
    }

    @Test
    void expandConvertsSummerBerlinLocalTimeToUtcInstant() {
        List<ConcreteWorkSlot> result = expander.expand(
                List.of(slot(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                Instant.parse("2026-07-06T00:00:00Z"),
                Instant.parse("2026-07-06T23:00:00Z"));

        assertEquals(1, result.size());
        assertEquals(Instant.parse("2026-07-06T06:00:00Z"), result.getFirst().startAt());
        assertEquals(Instant.parse("2026-07-06T08:00:00Z"), result.getFirst().endAt());
    }

    @Test
    void expandClampsCurrentlyActiveSlotStartToNow() {
        Instant now = Instant.parse("2026-07-06T06:30:00Z");

        List<ConcreteWorkSlot> result = expander.expand(
                List.of(slot(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                now,
                Instant.parse("2026-07-06T23:00:00Z"));

        assertEquals(1, result.size());
        assertEquals(now, result.getFirst().startAt());
        assertEquals(Instant.parse("2026-07-06T08:00:00Z"), result.getFirst().endAt());
    }

    @Test
    void expandExcludesExpiredOccurrences() {
        List<ConcreteWorkSlot> result = expander.expand(
                List.of(slot(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                Instant.parse("2026-07-06T08:00:00Z"),
                Instant.parse("2026-07-06T23:00:00Z"));

        assertEquals(List.of(), result);
    }

    @Test
    void expandGivesDuplicateSourceSlotsDistinctOccurrenceIds() {
        WorkSlot first = slot(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));
        WorkSlot second = slot(2L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        List<ConcreteWorkSlot> result = expander.expand(
                List.of(first, second),
                Instant.parse("2026-07-06T00:00:00Z"),
                Instant.parse("2026-07-06T23:00:00Z"));

        assertEquals(2, result.size());
        assertEquals(result.getFirst().startAt(), result.get(1).startAt());
        assertEquals(result.getFirst().endAt(), result.get(1).endAt());
        assertNotEquals(result.getFirst().occurrenceId(), result.get(1).occurrenceId());
    }

    private WorkSlot slot(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        WorkSlot slot = new WorkSlot();
        slot.setId(id);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        return slot;
    }
}
