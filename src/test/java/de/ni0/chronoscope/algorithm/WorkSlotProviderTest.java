package de.ni0.chronoscope.algorithm;

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
        ConcreteWorkSlot first = slot(START, START.plus(Duration.ofHours(1)), "first");
        ConcreteWorkSlot second = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)), "second");

        ConcreteWorkSlot result = new WorkSlotProvider(List.of(first, second)).getNextSlot(null);

        assertSame(first, result);
    }

    @Test
    void getNextSlotReturnsFollowingSlotAndNullAfterLastSlot() {
        ConcreteWorkSlot first = slot(START, START.plus(Duration.ofHours(1)), "first");
        ConcreteWorkSlot second = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)), "second");
        WorkSlotProvider provider = new WorkSlotProvider(List.of(first, second));

        assertSame(second, provider.getNextSlot(first));
        assertNull(provider.getNextSlot(second));
    }

    @Test
    void getNextSlotDistinguishesDuplicateWindowsByOccurrenceId() {
        ConcreteWorkSlot first = slot(START, START.plus(Duration.ofHours(1)), "first");
        ConcreteWorkSlot second = slot(START, START.plus(Duration.ofHours(1)), "second");
        WorkSlotProvider provider = new WorkSlotProvider(List.of(first, second));

        assertSame(second, provider.getNextSlot(first));
        assertNull(provider.getNextSlot(second));
    }

    @Test
    void getNextSlotReturnsNullWhenCurrentSlotIsUnknown() {
        ConcreteWorkSlot known = slot(START, START.plus(Duration.ofHours(1)), "known");
        ConcreteWorkSlot unknown = slot(START.plus(Duration.ofHours(1)), START.plus(Duration.ofHours(2)), "unknown");

        assertNull(new WorkSlotProvider(List.of(known)).getNextSlot(unknown));
    }

    private static ConcreteWorkSlot slot(Instant startAt, Instant endAt, String occurrenceId) {
        return new ConcreteWorkSlot(startAt, endAt, occurrenceId);
    }
}
