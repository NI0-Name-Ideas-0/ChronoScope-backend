package de.ni0.chronoscope.algorithm;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Removes {@link BlockedInterval} windows from a list of {@link ConcreteWorkSlot}s,
 * producing a new list of sub-slots that represent the remaining available time.
 *
 * <p>When a blocked interval overlaps a work slot the slot is split into at most two parts:
 * the free time before the block and the free time after. If the block covers the entire
 * remaining slot, that slot is dropped. Overlapping or adjacent blocked intervals from
 * different static tasks are handled correctly because the list is sorted by start time and
 * processed left-to-right.</p>
 *
 * <p>Sub-slots derived from a single source slot are assigned a {@code occurrenceId} of the
 * form {@code <originalId>#<partIndex>} so they remain distinguishable and stable within a
 * single planning run.</p>
 */
@Component
public class WorkSlotCarver {

    /**
     * Carves all blocked intervals out of the supplied work slots.
     *
     * @param slots   concrete work slots to carve (need not be sorted)
     * @param blocked blocked intervals to subtract (need not be sorted)
     * @return new list of sub-slots with all blocked time removed, sorted by start time
     */
    public List<ConcreteWorkSlot> carve(List<ConcreteWorkSlot> slots, List<BlockedInterval> blocked) {
        if (blocked.isEmpty()) {
            List<ConcreteWorkSlot> copy = new ArrayList<>(slots);
            copy.sort(Comparator.comparing(ConcreteWorkSlot::startAt));
            return copy;
        }

        List<BlockedInterval> sortedBlocked = blocked.stream()
                .sorted(Comparator.comparing(BlockedInterval::startAt))
                .toList();

        List<ConcreteWorkSlot> result = new ArrayList<>();
        for (ConcreteWorkSlot slot : slots) {
            carveSlot(slot, sortedBlocked, result);
        }
        result.sort(Comparator.comparing(ConcreteWorkSlot::startAt));
        return result;
    }

    /**
     * Carves a single work slot and appends any resulting sub-slots to {@code result}.
     *
     * <p>Works by maintaining a {@code freeStart} cursor that advances past each blocked
     * interval in order. The region {@code [freeStart, block.startAt)} before each block
     * becomes a sub-slot; the region {@code [freeStart, slot.endAt)} after the last block
     * is the final sub-slot (if non-empty).</p>
     */
    private void carveSlot(ConcreteWorkSlot slot, List<BlockedInterval> sortedBlocked, List<ConcreteWorkSlot> result) {
        Instant freeStart = slot.startAt();
        int partIndex = 0;

        for (BlockedInterval block : sortedBlocked) {
            // Skip blocks that end at or before the current free cursor.
            if (!block.endAt().isAfter(freeStart)) {
                continue;
            }
            // Stop once a block starts at or after the slot end; nothing more to carve.
            if (!block.startAt().isBefore(slot.endAt())) {
                break;
            }

            // If there is free time before this block, emit it as a sub-slot.
            if (block.startAt().isAfter(freeStart)) {
                result.add(new ConcreteWorkSlot(freeStart, block.startAt(),
                        slot.occurrenceId() + "#" + partIndex));
                partIndex++;
            }

            // Advance the cursor past this block.
            freeStart = block.endAt();

            // If the block consumed all remaining slot time, we are done with this slot.
            if (!freeStart.isBefore(slot.endAt())) {
                return;
            }
        }

        // Emit any remaining free time at the end of the slot.
        if (freeStart.isBefore(slot.endAt())) {
            result.add(new ConcreteWorkSlot(freeStart, slot.endAt(),
                    slot.occurrenceId() + "#" + partIndex));
        }
    }
}
