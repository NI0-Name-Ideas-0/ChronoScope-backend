package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.WorkSlot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class WorkSlotProviderTest {

    private static final Instant START = Instant.parse("2026-04-26T08:00:00Z");

    @Test
    void getNextSlotReturnsNullWhenNoSlotsExist() {
        assertNull(new WorkSlotProvider(List.of()).getNextSlot(null));
    }

    @Test
    void getNextSlotReturnsFirstSlotWhenCurrentSlotIsNull() {
        WorkSlot first = slot(START, START.plus(Duration.ofHours(1)));
        WorkSlot second = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));

        WorkSlot result = new WorkSlotProvider(List.of(first, second)).getNextSlot(null);

        assertSame(first, result);
    }

    @Test
    void getNextSlotReturnsFollowingSlotAndWrapsAfterLastSlot() {
        WorkSlot first = slot(START, START.plus(Duration.ofHours(1)));
        WorkSlot second = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)));
        WorkSlotProvider provider = new WorkSlotProvider(List.of(first, second));

        assertSame(second, provider.getNextSlot(first));
        assertSame(first, provider.getNextSlot(second));
    }

    private static WorkSlot slot(Instant startAt, Instant endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setId(startAt.toEpochMilli());
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        return slot;
    }
}
