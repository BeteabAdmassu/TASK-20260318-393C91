package com.mindflow.security.notification;

import com.mindflow.security.user.UserEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class NotificationRuleService {

    public boolean isNotificationAllowed(UserEntity user, NotificationType type, LocalTime now) {
        if (type == NotificationType.ARRIVAL_REMINDER && !user.isArrivalReminderEnabled()) {
            return false;
        }
        if (type == NotificationType.RESERVATION_SUCCESS && !user.isReservationSuccessEnabled()) {
            return false;
        }
        return !isWithinDndWindow(user, now);
    }

    public boolean isWithinDndWindow(UserEntity user, LocalTime now) {
        if (user.getDndStart() == null || user.getDndEnd() == null) {
            return false;
        }
        LocalTime start = user.getDndStart();
        LocalTime end = user.getDndEnd();

        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }
}
