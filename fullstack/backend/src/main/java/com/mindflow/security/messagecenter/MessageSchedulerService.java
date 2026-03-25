package com.mindflow.security.messagecenter;

import com.mindflow.security.notification.NotificationRuleService;
import com.mindflow.security.notification.NotificationType;
import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class MessageSchedulerService {

    private final BookingEventRepository bookingRepository;
    private final UserRepository userRepository;
    private final MessageQueueService queueService;
    private final NotificationRuleService notificationRuleService;

    public MessageSchedulerService(BookingEventRepository bookingRepository,
                                   UserRepository userRepository,
                                   MessageQueueService queueService,
                                   NotificationRuleService notificationRuleService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.queueService = queueService;
        this.notificationRuleService = notificationRuleService;
    }

    @Scheduled(fixedDelayString = "${app.message.scheduler-ms:60000}")
    @Transactional
    public void generateReservationSuccessMessages() {
        for (BookingEventEntity booking : bookingRepository.findByReservationSuccessSentFalse()) {
            queueService.enqueue(
                    booking.getUsername(),
                    MessageType.RESERVATION_SUCCESS,
                    "Reservation Confirmed",
                    "Reservation for route " + booking.getRouteNumber() + " confirmed. Reference token " + shortToken(booking.getPassengerPhoneToken())
            );
            booking.setReservationSuccessSent(true);
            bookingRepository.save(booking);
        }
    }

    @Scheduled(fixedDelayString = "${app.message.scheduler-ms:60000}")
    @Transactional
    public void generateArrivalReminders() {
        Instant now = Instant.now();
        for (BookingEventEntity booking : bookingRepository.findByArrivalReminderSentFalseAndStartTimeBefore(now.plusSeconds(3600))) {
            Optional<UserEntity> userOpt = userRepository.findByUsername(booking.getUsername());
            if (userOpt.isEmpty()) {
                continue;
            }
            UserEntity user = userOpt.get();
            Instant trigger = booking.getStartTime().minusSeconds((long) user.getReminderLeadMinutes() * 60L);
            if (now.isBefore(trigger)) {
                continue;
            }
            LocalTime currentLocal = LocalTime.ofInstant(now, ZoneId.systemDefault());
            boolean allowed = notificationRuleService.isNotificationAllowed(user, NotificationType.ARRIVAL_REMINDER, currentLocal);
            if (allowed) {
                queueService.enqueue(
                        booking.getUsername(),
                        MessageType.ARRIVAL_REMINDER,
                        "Arrival Reminder",
                        "Your bus on route " + booking.getRouteNumber() + " arrives in " + user.getReminderLeadMinutes() + " minutes. Ref " + shortToken(booking.getPassengerPhoneToken())
                );
            }
            booking.setArrivalReminderSent(true);
            bookingRepository.save(booking);
        }
    }

    @Scheduled(fixedDelayString = "${app.message.scheduler-ms:60000}")
    @Transactional
    public void generateMissedCheckIns() {
        Instant now = Instant.now().minusSeconds(300);
        for (BookingEventEntity booking : bookingRepository.findByMissedCheckInSentFalseAndStartTimeBefore(now)) {
            queueService.enqueue(
                    booking.getUsername(),
                    MessageType.MISSED_CHECK_IN,
                    "Missed Check-In",
                    "You missed check-in for route " + booking.getRouteNumber() + ". Please rebook if needed."
            );
            booking.setMissedCheckInSent(true);
            bookingRepository.save(booking);
        }
    }

    private String shortToken(String token) {
        if (token == null || token.length() < 8) {
            return "n/a";
        }
        return token.substring(0, 8);
    }
}
