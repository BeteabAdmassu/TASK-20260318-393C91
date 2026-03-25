package com.mindflow.security.notification;

import com.mindflow.security.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRuleServiceTest {

    private NotificationRuleService service;

    @BeforeEach
    void setUp() {
        service = new NotificationRuleService();
    }

    @Test
    void dndAcrossMidnightSilencesNotifications() {
        UserEntity user = new UserEntity();
        user.setArrivalReminderEnabled(true);
        user.setReservationSuccessEnabled(true);
        user.setDndStart(LocalTime.of(22, 0));
        user.setDndEnd(LocalTime.of(7, 0));

        assertFalse(service.isNotificationAllowed(user, NotificationType.ARRIVAL_REMINDER, LocalTime.of(23, 30)));
        assertFalse(service.isNotificationAllowed(user, NotificationType.RESERVATION_SUCCESS, LocalTime.of(6, 59)));
        assertTrue(service.isNotificationAllowed(user, NotificationType.ARRIVAL_REMINDER, LocalTime.of(10, 0)));
    }

    @Test
    void disabledArrivalReminderBlocksArrivalType() {
        UserEntity user = new UserEntity();
        user.setArrivalReminderEnabled(false);
        user.setReservationSuccessEnabled(true);

        assertFalse(service.isNotificationAllowed(user, NotificationType.ARRIVAL_REMINDER, LocalTime.NOON));
        assertTrue(service.isNotificationAllowed(user, NotificationType.RESERVATION_SUCCESS, LocalTime.NOON));
    }
}
