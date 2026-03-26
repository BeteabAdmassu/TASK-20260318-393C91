package com.mindflow.security.messagecenter;

import com.mindflow.security.admin.TemplateResponse;
import com.mindflow.security.admin.AdminControlService;
import com.mindflow.security.common.TenantContext;
import com.mindflow.security.message.SensitivityLevel;
import com.mindflow.security.notification.NotificationRuleService;
import com.mindflow.security.notification.NotificationType;
import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final AdminControlService adminControlService;
    private final Clock clock;

    public MessageSchedulerService(BookingEventRepository bookingRepository,
                                   UserRepository userRepository,
                                   MessageQueueService queueService,
                                   NotificationRuleService notificationRuleService,
                                   AdminControlService adminControlService,
                                   Clock clock) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.queueService = queueService;
        this.notificationRuleService = notificationRuleService;
        this.adminControlService = adminControlService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.message.scheduler-ms:60000}")
    @Transactional
    public void generateReservationSuccessMessages() {
        String tenantId = TenantContext.getTenantId();
        for (BookingEventEntity booking : bookingRepository.findByReservationSuccessSentFalseAndTenantId(tenantId)) {
            TemplateResponse template = adminControlService.resolveTemplate(
                    "reservation.success",
                    "Reservation Confirmed",
                    "Reservation for route {route} confirmed. Reference token {phoneToken}"
            );
            queueService.enqueue(
                    booking.getUsername(),
                    MessageType.RESERVATION_SUCCESS,
                    template.subject(),
                    applyTemplate(template.body(), booking),
                    SensitivityLevel.HIGH
            );
            booking.setReservationSuccessSent(true);
            bookingRepository.save(booking);
        }
    }

    @Scheduled(fixedDelayString = "${app.message.scheduler-ms:60000}")
    @Transactional
    public void generateArrivalReminders() {
        String tenantId = TenantContext.getTenantId();
        Instant now = Instant.now(clock);
        for (BookingEventEntity booking : bookingRepository.findByArrivalReminderSentFalseAndStartTimeBeforeAndTenantId(now.plusSeconds(3600), tenantId)) {
            Optional<UserEntity> userOpt = userRepository.findByUsernameAndTenantId(booking.getUsername(), tenantId);
            if (userOpt.isEmpty()) {
                continue;
            }
            UserEntity user = userOpt.get();
            Instant trigger = booking.getStartTime().minusSeconds((long) user.getReminderLeadMinutes() * 60L);
            if (now.isBefore(trigger)) {
                continue;
            }
            LocalTime currentLocal = LocalTime.ofInstant(now, clock.getZone());
            boolean allowed = notificationRuleService.isNotificationAllowed(user, NotificationType.ARRIVAL_REMINDER, currentLocal);
            if (allowed) {
                TemplateResponse template = adminControlService.resolveTemplate(
                        "arrival.reminder",
                        "Arrival Reminder",
                        "Your bus on route {route} arrives in {leadMinutes} minutes. Ref {phoneToken}"
                );
                queueService.enqueue(
                        booking.getUsername(),
                        MessageType.ARRIVAL_REMINDER,
                        template.subject(),
                        applyTemplate(template.body(), booking, user),
                        SensitivityLevel.MEDIUM
                );
            }
            booking.setArrivalReminderSent(true);
            bookingRepository.save(booking);
        }
    }

    @Scheduled(fixedDelayString = "${app.message.scheduler-ms:60000}")
    @Transactional
    public void generateMissedCheckIns() {
        String tenantId = TenantContext.getTenantId();
        Instant now = Instant.now(clock).minusSeconds(300);
        for (BookingEventEntity booking : bookingRepository.findByMissedCheckInSentFalseAndStartTimeLessThanEqualAndTenantId(now, tenantId)) {
            TemplateResponse template = adminControlService.resolveTemplate(
                    "missed.checkin",
                    "Missed Check-In",
                    "You missed check-in for route {route}. Please rebook if needed."
            );
            queueService.enqueue(
                    booking.getUsername(),
                    MessageType.MISSED_CHECK_IN,
                    template.subject(),
                    applyTemplate(template.body(), booking),
                    SensitivityLevel.LOW
            );
            booking.setMissedCheckInSent(true);
            bookingRepository.save(booking);
        }
    }

    private String applyTemplate(String body, BookingEventEntity booking) {
        return body
                .replace("{route}", booking.getRouteNumber())
                .replace("{phoneToken}", shortToken(booking.getPassengerPhoneToken()));
    }

    private String applyTemplate(String body, BookingEventEntity booking, UserEntity user) {
        return applyTemplate(body, booking)
                .replace("{leadMinutes}", String.valueOf(user.getReminderLeadMinutes()));
    }

    private String shortToken(String token) {
        if (token == null || token.length() < 8) {
            return "n/a";
        }
        return token.substring(0, 8);
    }
}
