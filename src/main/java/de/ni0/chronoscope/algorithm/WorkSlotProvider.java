package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.OrganizationSlot;
import lombok.RequiredArgsConstructor;
import org.dmfs.rfc5545.RecurrenceSet;

import java.time.Instant;

@RequiredArgsConstructor
public class WorkSlotProvider {
    private final RecurrenceSet rset;

    public OrganizationSlot getNextSlot(Instant currentTime) {
    }
}
