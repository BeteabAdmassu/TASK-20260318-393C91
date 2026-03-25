package com.mindflow.security.messagecenter;

import com.mindflow.security.admin.AdminControlService;
import com.mindflow.security.admin.TemplateRequest;
import com.mindflow.security.message.SensitivityLevel;
import com.mindflow.security.user.Role;
import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MessageTemplateRuntimeTest {

    @Autowired
    private AdminControlService adminControlService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingEventRepository bookingEventRepository;

    @Autowired
    private MessageSchedulerService messageSchedulerService;

    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setup() {
        UserEntity user = userRepository.findByUsername("templ_user").orElseGet(UserEntity::new);
        user.setUsername("templ_user");
        user.setPasswordHash("noop");
        user.setRole(Role.PASSENGER);
        user.setEnabled(true);
        user.setArrivalReminderEnabled(true);
        user.setReservationSuccessEnabled(true);
        user.setReminderLeadMinutes(10);
        userRepository.save(user);

        TemplateRequest template = new TemplateRequest(
                "reservation.success",
                "Custom Reservation Subject",
                "Your route {route} booking is done. Ref={phoneToken}"
        );
        adminControlService.listTemplates().stream()
                .filter(t -> "reservation.success".equals(t.templateKey()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> adminControlService.updateTemplate(existing.id(), template),
                        () -> adminControlService.createTemplate(template)
                );
    }

    @Test
    void reservationMessagesUseAdminManagedTemplateAtRuntime() {
        BookingEventEntity event = new BookingEventEntity();
        event.setUsername("templ_user");
        event.setRouteNumber("T88");
        event.setPassengerPhoneToken("1234567890abcdef");
        event.setPassengerIdCardToken("id-token");
        event.setStartTime(Instant.now().plusSeconds(1800));
        event.setReservationSuccessSent(false);
        event.setArrivalReminderSent(false);
        event.setMissedCheckInSent(false);
        bookingEventRepository.save(event);

        messageSchedulerService.generateReservationSuccessMessages();
        messageQueueService.processQueue();

        MessageEntity created = messageRepository.findByUsernameOrderByCreatedAtDesc("templ_user")
                .stream()
                .filter(row -> row.getType() == MessageType.RESERVATION_SUCCESS)
                .findFirst()
                .orElseThrow();

        assertThat(created.getTitle()).isEqualTo("Custom Reservation Subject");
        assertThat(created.getContent()).contains("T88");
        assertThat(created.getContent()).contains("12345678");
        assertThat(created.getSensitivityLevel()).isEqualTo(SensitivityLevel.HIGH);
    }
}
