package com.mindflow.security.notification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NotificationPreferencesRequest(
        @NotNull Boolean arrivalReminderEnabled,
        @NotNull Boolean reservationSuccessEnabled,
        @NotNull @Min(1) @Max(120) Integer reminderLeadMinutes,
        Boolean dndEnabled,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "dndStart must be HH:mm") String dndStart,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "dndEnd must be HH:mm") String dndEnd
) {
}
