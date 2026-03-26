package com.mindflow.security.messagecenter;

import com.mindflow.security.message.SensitivityLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MessageQueueIdempotencyTest {

    @Autowired
    private MessageQueueService queueService;

    @Autowired
    private MessageQueueEventRepository queueRepository;

    @Test
    void enqueueIsIdempotentForSamePayloadAndTrace() {
        queueService.enqueue("idem_user", MessageType.ARRIVAL_REMINDER, "Arrival Reminder", "Bus arrives in 10 minutes", SensitivityLevel.MEDIUM);
        queueService.enqueue("idem_user", MessageType.ARRIVAL_REMINDER, "Arrival Reminder", "Bus arrives in 10 minutes", SensitivityLevel.MEDIUM);

        long pendingCount = queueRepository.findTop100ByStatusOrderByCreatedAtAsc(QueueStatus.PENDING).stream()
                .filter(e -> "idem_user".equals(e.getUsername())
                        && e.getType() == MessageType.ARRIVAL_REMINDER
                        && "Arrival Reminder".equals(e.getTitle()))
                .count();

        assertThat(pendingCount).isEqualTo(1);
    }
}
