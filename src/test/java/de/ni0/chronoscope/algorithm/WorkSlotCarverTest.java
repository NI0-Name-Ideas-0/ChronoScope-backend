package de.ni0.chronoscope.algorithm;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkSlotCarverTest {

    private final WorkSlotCarver carver = new WorkSlotCarver();

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static ConcreteWorkSlot slot(String start, String end) {
        return new ConcreteWorkSlot(Instant.parse(start), Instant.parse(end), start);
    }

    private static BlockedInterval blocked(String start, String end) {
        return new BlockedInterval(Instant.parse(start), Instant.parse(end));
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    void carve_returnsSlotsCopyWhenNoBlockedIntervals() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z")
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, List.of());

        assertEquals(1, result.size());
        assertEquals(slots.get(0).startAt(), result.get(0).startAt());
        assertEquals(slots.get(0).endAt(), result.get(0).endAt());
    }

    @Test
    void carve_removesFullyCoveredSlot() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z")
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T07:00:00Z", "2026-01-05T11:00:00Z") // covers entire slot
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertTrue(result.isEmpty(), "Fully covered slot should be removed");
    }

    @Test
    void carve_splitsSlotWithMiddleBlock() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T12:00:00Z") // 4-hour slot
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T09:00:00Z", "2026-01-05T10:00:00Z") // 1-hour block in middle
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertEquals(2, result.size());
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).endAt());
        assertEquals(Instant.parse("2026-01-05T10:00:00Z"), result.get(1).startAt());
        assertEquals(Instant.parse("2026-01-05T12:00:00Z"), result.get(1).endAt());
    }

    @Test
    void carve_trimsSlotStartWhenBlockOverlapsBeginning() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T12:00:00Z")
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T07:00:00Z", "2026-01-05T09:00:00Z") // overlaps first hour
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertEquals(1, result.size());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T12:00:00Z"), result.get(0).endAt());
    }

    @Test
    void carve_trimsSlotEndWhenBlockOverlapsEnd() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T12:00:00Z")
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T11:00:00Z", "2026-01-05T13:00:00Z") // overlaps last hour
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertEquals(1, result.size());
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T11:00:00Z"), result.get(0).endAt());
    }

    @Test
    void carve_handlesMultipleBlocksInOneSlot() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T18:00:00Z") // 10-hour slot
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T09:00:00Z", "2026-01-05T10:00:00Z"),
                blocked("2026-01-05T12:00:00Z", "2026-01-05T13:00:00Z"),
                blocked("2026-01-05T16:00:00Z", "2026-01-05T17:00:00Z")
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertEquals(4, result.size());
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).endAt());
        assertEquals(Instant.parse("2026-01-05T10:00:00Z"), result.get(1).startAt());
        assertEquals(Instant.parse("2026-01-05T12:00:00Z"), result.get(1).endAt());
        assertEquals(Instant.parse("2026-01-05T13:00:00Z"), result.get(2).startAt());
        assertEquals(Instant.parse("2026-01-05T16:00:00Z"), result.get(2).endAt());
        assertEquals(Instant.parse("2026-01-05T17:00:00Z"), result.get(3).startAt());
        assertEquals(Instant.parse("2026-01-05T18:00:00Z"), result.get(3).endAt());
    }

    @Test
    void carve_handlesBlockSpanningMultipleSlots() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z"),
                slot("2026-01-05T12:00:00Z", "2026-01-05T14:00:00Z")
        );
        // Block spans across both slots (ends inside the second slot).
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T09:00:00Z", "2026-01-05T13:00:00Z")
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertEquals(2, result.size());
        // First slot: 08:00–09:00 survives
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).endAt());
        // Second slot: 13:00–14:00 survives
        assertEquals(Instant.parse("2026-01-05T13:00:00Z"), result.get(1).startAt());
        assertEquals(Instant.parse("2026-01-05T14:00:00Z"), result.get(1).endAt());
    }

    @Test
    void carve_resultIsSortedByStart() {
        // Provide slots out of order to verify the result is sorted.
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T14:00:00Z", "2026-01-05T16:00:00Z"),
                slot("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z")
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, List.of());

        assertEquals(2, result.size());
        assertTrue(result.get(0).startAt().isBefore(result.get(1).startAt()));
    }

    @Test
    void carve_handlesOverlappingBlockedIntervals() {
        // Two overlapping blocked intervals acting on the same slot.
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T12:00:00Z")
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T09:00:00Z", "2026-01-05T11:00:00Z"),
                blocked("2026-01-05T10:00:00Z", "2026-01-05T11:30:00Z") // overlaps with first
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        // 08:00–09:00 free; 09:00–11:30 blocked (second block eaten by first); 11:30–12:00 free
        assertEquals(2, result.size());
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), result.get(0).endAt());
        assertEquals(Instant.parse("2026-01-05T11:30:00Z"), result.get(1).startAt());
        assertEquals(Instant.parse("2026-01-05T12:00:00Z"), result.get(1).endAt());
    }

    @Test
    void carve_blockExactlyAtSlotBoundaries_removesSlot() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z")
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z") // exact same bounds
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertTrue(result.isEmpty());
    }

    @Test
    void carve_blockEntirelyAfterSlot_slotIsUnchanged() {
        List<ConcreteWorkSlot> slots = List.of(
                slot("2026-01-05T08:00:00Z", "2026-01-05T10:00:00Z")
        );
        List<BlockedInterval> blocked = List.of(
                blocked("2026-01-05T11:00:00Z", "2026-01-05T12:00:00Z") // after slot
        );

        List<ConcreteWorkSlot> result = carver.carve(slots, blocked);

        assertEquals(1, result.size());
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), result.get(0).startAt());
        assertEquals(Instant.parse("2026-01-05T10:00:00Z"), result.get(0).endAt());
    }
}
