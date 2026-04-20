package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.WorkSlot;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class WorkSlotProvider {
    private final List<WorkSlot> workSlots;

    public WorkSlot getNextSlot(WorkSlot currentSlot) {
        if (currentSlot == null) {
            if (this.workSlots.isEmpty()) return null;
            return this.workSlots.getFirst();
        }
        return this.workSlots.get(Math.floorMod(this.workSlots.indexOf(currentSlot) + 1, this.workSlots.size()));
    }
}
