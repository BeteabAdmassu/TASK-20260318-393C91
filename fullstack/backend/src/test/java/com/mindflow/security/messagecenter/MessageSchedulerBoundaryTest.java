package com.mindflow.security.messagecenter;

import com.mindflow.security.user.Role;
import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MessageSchedulerBoundaryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingEventRepository bookingEventRepository;

    @Autowired
    private MessageSchedulerService scheduler;

    @Autowired
    private MessageQueueService queueService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MutableClock mutableClock;

    @BeforeEach
    void setup() {
        UserEntity user = userRepository.findByUsername("boundary_user").orElseGet(UserEntity::new);
        user.setUsername("boundary_user");
        user.setPasswordHash("noop");
        user.setRole(Role.PASSENGER);
        user.setEnabled(true);
        user.setArrivalReminderEnabled(true);
        user.setReservationSuccessEnabled(true);
        user.setReminderLeadMinutes(10);
        user.setDndStart(LocalTime.of(22, 0));
        user.setDndEnd(LocalTime.of(7, 0));
        userRepository.save(user);
    }

    @Test
    void arrivalReminderTriggersAtExactLeadTimeOutsideDnd() {
        mutableClock.set(Instant.parse("2026-03-25T12:00:00Z"));
        BookingEventEntity event = new BookingEventEntity();
        event.setUsername("boundary_user");
        event.setRouteNumber("R100");
        event.setPassengerPhoneToken("1234567890abcdef");
        event.setPassengerIdCardToken("id");
        event.setStartTime(Instant.parse("2026-03-25T12:10:00Z"));
        event.setArrivalReminderSent(false);
        event.setReservationSuccessSent(true);
        event.setMissedCheckInSent(false);
        bookingEventRepository.save(event);

        scheduler.generateArrivalReminders();
        queueService.processQueue();

        assertThat(messageRepository.findByUsernameOrderByCreatedAtDesc("boundary_user"))
                .anyMatch(m -> m.getType() == MessageType.ARRIVAL_REMINDER);
    }

    @Test
    void arrivalReminderSuppressedInsideDndEvenIfLeadTimeReached() {
        mutableClock.set(Instant.parse("2026-03-25T23:00:00Z"));
        BookingEventEntity event = new BookingEventEntity();
        event.setUsername("boundary_user");
        event.setRouteNumber("R200");
        event.setPassengerPhoneToken("1234567890abcdef");
        event.setPassengerIdCardToken("id");
        event.setStartTime(Instant.parse("2026-03-25T23:10:00Z"));
        event.setArrivalReminderSent(false);
        event.setReservationSuccessSent(true);
        event.setMissedCheckInSent(false);
        bookingEventRepository.save(event);

        scheduler.generateArrivalReminders();
        queueService.processQueue();

        assertThat(messageRepository.findByUsernameOrderByCreatedAtDesc("boundary_user"))
                .noneMatch(m -> m.getType() == MessageType.ARRIVAL_REMINDER && m.getContent().contains("R200"));
    }

    @Test
    void missedCheckInTriggersAtPlusFiveMinutesBoundary() {
        mutableClock.set(Instant.parse("2026-03-25T10:05:00Z"));
        BookingEventEntity event = new BookingEventEntity();
        event.setUsername("boundary_user");
        event.setRouteNumber("R300");
        event.setPassengerPhoneToken("1234567890abcdef");
        event.setPassengerIdCardToken("id");
        event.setStartTime(Instant.parse("2026-03-25T10:00:00Z"));
        event.setArrivalReminderSent(true);
        event.setReservationSuccessSent(true);
        event.setMissedCheckInSent(false);
        bookingEventRepository.save(event);

        scheduler.generateMissedCheckIns();
        queueService.processQueue();

        assertThat(messageRepository.findByUsernameOrderByCreatedAtDesc("boundary_user"))
                .anyMatch(m -> m.getType() == MessageType.MISSED_CHECK_IN);
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-03-25T00:00:00Z"), ZoneId.of("UTC"));
        }

        @Bean
        @Primary
        Clock exposedClock(MutableClock mutableClock) {
            return mutableClock;
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
