package com.mindflow.security.monitoring;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilityServiceTest {

    @Test
    void convertsNanosecondsToMillisecondsCorrectly() {
        double millis = ObservabilityService.toMillis(35_651_584d, TimeUnit.NANOSECONDS);
        assertEquals(35.651584d, millis, 0.000001d);
    }

    @Test
    void convertsSecondsToMillisecondsCorrectly() {
        double millis = ObservabilityService.toMillis(0.75d, TimeUnit.SECONDS);
        assertEquals(750d, millis, 0.000001d);
    }
}
