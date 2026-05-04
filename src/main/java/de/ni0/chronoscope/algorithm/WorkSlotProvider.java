package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.WorkSlot;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Cyclic provider for the ordered work-slot list used during planning.
 */
@RequiredArgsConstructor
public class WorkSlotProvider {
    private final List<WorkSlot> workSlots;

    /**
     * Returns the first slot for a new plan or the next slot after the supplied one.
     *
     * @param currentSlot current slot, or {@code null} to request the first slot
     * @return next slot, or {@code null} when no slots are available
     */
    public WorkSlot getNextSlot(WorkSlot currentSlot) {
        if (currentSlot == null) {
            if (this.workSlots.isEmpty()) return null;
            return this.workSlots.getFirst();
        }
        return this.workSlots.get(Math.floorMod(this.workSlots.indexOf(currentSlot) + 1, this.workSlots.size()));
    }
}
