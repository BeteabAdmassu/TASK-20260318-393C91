package com.mindflow.security.messagecenter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BookingEventRequest(
        @NotBlank String routeNumber,
        @NotBlank String passengerPhone,
        @NotBlank String passengerIdCard,
        @NotNull Instant startTime
) {
}
