package com.mindflow.security.monitoring;

import com.mindflow.security.messagecenter.MessageQueueEventEntity;
import com.mindflow.security.messagecenter.MessageQueueEventRepository;
import com.mindflow.security.messagecenter.MessageType;
import com.mindflow.security.messagecenter.QueueStatus;
import com.mindflow.security.message.SensitivityLevel;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ObservabilityAlertTriggerTest {

    @Autowired
    private ObservabilityService observabilityService;

    @Autowired
    private MessageQueueEventRepository queueEventRepository;

    @Autowired
    private AlertDiagnosticRepository alertDiagnosticRepository;

    @Autowired
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Test
    void createsQueueBacklogAlertWhenPendingEventsExceedThreshold() {
        for (int i = 0; i < 5; i++) {
            MessageQueueEventEntity event = new MessageQueueEventEntity();
            event.setUsername("obs_user");
            event.setType(MessageType.ARRIVAL_REMINDER);
            event.setTitle("Reminder");
            event.setContent("payload");
            event.setSensitivityLevel(SensitivityLevel.MEDIUM);
            event.setTraceId("obs-trace-" + i);
            event.setStatus(QueueStatus.PENDING);
            queueEventRepository.save(event);
        }

        observabilityService.evaluateAlerts();

        assertThat(alertDiagnosticRepository.findTop100ByOrderByCreatedAtDesc())
                .anyMatch(alert -> "QUEUE_BACKLOG".equals(alert.getAlertType()));
    }

    @Test
    void createsApiP95AlertWhenLatencyBreachesThreshold() {
        Timer timer = Timer.builder("app.api.request.duration")
                .publishPercentiles(0.95)
                .register(meterRegistry);

        for (int i = 0; i < 30; i++) {
            timer.record(Duration.ofMillis(700));
        }

        observabilityService.evaluateAlerts();

        assertThat(alertDiagnosticRepository.findTop100ByOrderByCreatedAtDesc())
                .anyMatch(alert -> "API_P95".equals(alert.getAlertType()));
    }
}
