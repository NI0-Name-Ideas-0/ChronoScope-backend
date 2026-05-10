package de.ni0.chronoscope.algorithm;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Linear provider for the ordered concrete work-slot list used during planning.
 */
@RequiredArgsConstructor
public class WorkSlotProvider {
    private final List<ConcreteWorkSlot> workSlots;

    /**
     * Returns the first slot for a new plan or the next slot after the supplied one.
     *
     * @param currentSlot current slot, or {@code null} to request the first slot
     * @return next slot, or {@code null} when no slots are available or the current slot is the last one
     */
    public ConcreteWorkSlot getNextSlot(ConcreteWorkSlot currentSlot) {
        if (currentSlot == null) {
            if (this.workSlots.isEmpty()) return null;
            return this.workSlots.getFirst();
        }
        int currentIndex = this.workSlots.indexOf(currentSlot);
        if (currentIndex < 0) {
            return null;
        }
        int nextIndex = currentIndex + 1;
        if (nextIndex >= this.workSlots.size()) {
            return null;
        }
        return this.workSlots.get(nextIndex);
    }
}
