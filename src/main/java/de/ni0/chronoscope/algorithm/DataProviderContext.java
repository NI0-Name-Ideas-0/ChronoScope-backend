package de.ni0.chronoscope.algorithm;

import lombok.Data;

import java.time.Instant;

@Data
public class DataProviderContext {
    private final Instant currentTime;
}
