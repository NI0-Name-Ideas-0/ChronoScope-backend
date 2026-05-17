package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.StaticTask;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticTaskExpanderTest {

    private final StaticTaskExpander expander = new StaticTaskExpander();

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static StaticTask task(long id, Instant startAt, Instant endAt, String rrule) {
        StaticTask t = new StaticTask();
        t.setId(id);
        t.setStartAt(startAt);
        t.setEndAt(endAt);
        t.setRrule(rrule);
        t.setIsBlocker(false);
        t.setName("test-static");
        t.setDescription("");
        t.setDifficulty(de.ni0.chronoscope.model.Task.Difficulty.TRIVIAL);
        return t;
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    void expand_returnsEmptyListWhenNoTasks() {
        Instant now = Instant.parse("2026-01-05T08:00:00Z");
        Instant horizon = Instant.parse("2026-01-12T08:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(), now, horizon);

        assertTrue(result.isEmpty());
    }

    @Test
    void expand_skipsTasksWithZeroDuration() {
        Instant ts = Instant.parse("2026-01-05T09:00:00Z");
        StaticTask t = task(1L, ts, ts, "FREQ=DAILY"); // zero duration

        Instant now = Instant.parse("2026-01-05T08:00:00Z");
        Instant horizon = Instant.parse("2026-01-12T08:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertTrue(result.isEmpty());
    }

    @Test
    void expand_throwsForInvalidRrule() {
        Instant startAt = Instant.parse("2026-01-05T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-05T10:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "NOT_VALID_RRULE");

        assertThrows(IllegalArgumentException.class,
                () -> expander.expand(List.of(t), startAt, endAt.plusSeconds(3600)));
    }

    @Test
    void expand_dailyRrule_generatesOccurrencesWithinHorizon() {
        // First occurrence: 2026-01-05 09:00–10:00 UTC; recurs daily.
        Instant startAt = Instant.parse("2026-01-05T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-05T10:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "FREQ=DAILY");

        Instant now = Instant.parse("2026-01-05T08:00:00Z");
        Instant horizon = Instant.parse("2026-01-12T00:00:00Z"); // 7 days

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        // Occurrences on Jan 5–11 (Jan 12 09:00 is outside the horizon).
        assertEquals(7, result.size());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T10:00:00Z"), result.get(0).endAt());
        assertEquals(Instant.parse("2026-01-11T09:00:00Z"), result.get(6).startAt());
    }

    @Test
    void expand_excludesOccurrencesCompletelyBeforeNow() {
        // Single-day occurrence strictly before now.
        Instant startAt = Instant.parse("2026-01-04T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-04T10:00:00Z");
        // Use COUNT=1 so there is exactly one occurrence.
        StaticTask t = task(1L, startAt, endAt, "FREQ=DAILY;COUNT=1");

        Instant now = Instant.parse("2026-01-05T08:00:00Z"); // day after the only occurrence
        Instant horizon = Instant.parse("2026-01-10T08:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertTrue(result.isEmpty(), "Occurrence that ended before now should be excluded");
    }

    @Test
    void expand_includesOngoingOccurrenceStartedBeforeNow() {
        // Occurrence started at 09:00; now is 09:30 (still ongoing).
        Instant startAt = Instant.parse("2026-01-05T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-05T10:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "FREQ=DAILY;COUNT=1");

        Instant now = Instant.parse("2026-01-05T09:30:00Z"); // 30 min into the occurrence
        Instant horizon = Instant.parse("2026-01-05T12:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertEquals(1, result.size());
        assertEquals(startAt, result.get(0).startAt());
        assertEquals(endAt, result.get(0).endAt());
    }

    @Test
    void expand_countLimitedRrule_stopsAtCount() {
        Instant startAt = Instant.parse("2026-01-05T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-05T10:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "FREQ=DAILY;COUNT=3");

        Instant now = Instant.parse("2026-01-05T08:00:00Z");
        Instant horizon = Instant.parse("2026-01-15T00:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        // COUNT=3 means Jan 5, Jan 6, Jan 7 only.
        assertEquals(3, result.size());
    }

    @Test
    void expand_weeklyRrule_generatesCorrectDays() {
        // 2026-01-05 is a Monday; FREQ=WEEKLY;BYDAY=MO produces every Monday.
        Instant startAt = Instant.parse("2026-01-05T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-05T10:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "FREQ=WEEKLY;BYDAY=MO");

        Instant now = Instant.parse("2026-01-05T08:00:00Z");
        Instant horizon = Instant.parse("2026-01-27T00:00:00Z"); // spans 3 weeks

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        // Mondays: Jan 5, Jan 12, Jan 19 (Jan 26 09:00 is before horizon, so 4 total)
        assertEquals(4, result.size());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-12T09:00:00Z"), result.get(1).startAt());
        assertEquals(Instant.parse("2026-01-19T09:00:00Z"), result.get(2).startAt());
        assertEquals(Instant.parse("2026-01-26T09:00:00Z"), result.get(3).startAt());
    }

    // ---------------------------------------------------------------------------
    // Empty rrule (single one-time occurrence)
    // ---------------------------------------------------------------------------

    @Test
    void expand_emptyRrule_returnsSingleOccurrenceOverlappingNow() {
        Instant startAt = Instant.parse("2026-05-18T08:00:00Z");
        Instant endAt = Instant.parse("2026-05-18T09:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "");

        Instant now = Instant.parse("2026-05-18T07:00:00Z");
        Instant horizon = Instant.parse("2026-05-19T00:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertEquals(1, result.size());
        assertEquals(startAt, result.get(0).startAt());
        assertEquals(endAt, result.get(0).endAt());
    }

    @Test
    void expand_emptyRrule_excludesOccurrenceCompletelyBeforeNow() {
        Instant startAt = Instant.parse("2026-05-17T08:00:00Z");
        Instant endAt = Instant.parse("2026-05-17T09:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "");

        Instant now = Instant.parse("2026-05-18T07:00:00Z"); // day after
        Instant horizon = Instant.parse("2026-05-20T00:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertTrue(result.isEmpty());
    }

    @Test
    void expand_emptyRrule_excludesOccurrenceCompletelyAfterHorizon() {
        Instant startAt = Instant.parse("2026-05-25T08:00:00Z");
        Instant endAt = Instant.parse("2026-05-25T09:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "");

        Instant now = Instant.parse("2026-05-18T07:00:00Z");
        Instant horizon = Instant.parse("2026-05-20T00:00:00Z"); // before startAt

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------------------
    // Full iCal property format: DTSTART:\nRRULE:
    // ---------------------------------------------------------------------------

    @Test
    void expand_fullIcalFormat_parsesCorrectly() {
        // DTSTART is 2026-05-18T08:00Z; occurrences repeat daily until 2026-05-23T22:00Z.
        // Duration from task: 1 hour.
        Instant startAt = Instant.parse("2026-05-18T08:00:00Z");
        Instant endAt = Instant.parse("2026-05-18T09:00:00Z");
        String rrule = "DTSTART:20260518T080000Z\nRRULE:FREQ=DAILY;INTERVAL=1;UNTIL=20260523T220000Z";
        StaticTask t = task(1L, startAt, endAt, rrule);

        Instant now = Instant.parse("2026-05-18T07:00:00Z");
        Instant horizon = Instant.parse("2026-05-25T00:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        // UNTIL=20260523T220000Z means last occurrence start that is ≤ UNTIL.
        // Daily from May 18 to May 23 → 6 occurrences (18, 19, 20, 21, 22, 23).
        assertEquals(6, result.size());
        assertEquals(Instant.parse("2026-05-18T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-05-18T09:00:00Z"), result.get(0).endAt());
        assertEquals(Instant.parse("2026-05-23T08:00:00Z"), result.get(5).startAt());
    }

    @Test
    void expand_rruleWithRrulePrefix_parsesCorrectly() {
        // Single-line "RRULE:..." format (no DTSTART line).
        Instant startAt = Instant.parse("2026-01-05T09:00:00Z");
        Instant endAt = Instant.parse("2026-01-05T10:00:00Z");
        StaticTask t = task(1L, startAt, endAt, "RRULE:FREQ=DAILY;COUNT=2");

        Instant now = Instant.parse("2026-01-05T08:00:00Z");
        Instant horizon = Instant.parse("2026-01-10T00:00:00Z");

        List<BlockedInterval> result = expander.expand(List.of(t), now, horizon);

        assertEquals(2, result.size());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-06T09:00:00Z"), result.get(1).startAt());
    }
}
