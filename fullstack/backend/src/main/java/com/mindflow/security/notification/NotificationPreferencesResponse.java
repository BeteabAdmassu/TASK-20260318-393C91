package com.mindflow.security.notification;

public record NotificationPreferencesResponse(
        String username,
        boolean arrivalReminderEnabled,
        boolean reservationSuccessEnabled,
        int reminderLeadMinutes,
        boolean dndEnabled,
        String dndStart,
        String dndEnd,
        boolean dndActiveNow
) {
}
