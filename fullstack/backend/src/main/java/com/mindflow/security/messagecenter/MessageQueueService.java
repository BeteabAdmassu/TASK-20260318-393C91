package com.mindflow.security.messagecenter;

import com.mindflow.security.message.SensitivityLevel;
import com.mindflow.security.monitoring.ObservabilityService;
import com.mindflow.security.monitoring.TraceContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageQueueService {

    private final MessageQueueEventRepository queueRepository;
    private final MessageRepository messageRepository;
    private final MessageMaskingService messageMaskingService;
    private final MeterRegistry meterRegistry;
    private final ObservabilityService observabilityService;

    public MessageQueueService(MessageQueueEventRepository queueRepository,
                               MessageRepository messageRepository,
                               MessageMaskingService messageMaskingService,
                               MeterRegistry meterRegistry,
                               ObservabilityService observabilityService) {
        this.queueRepository = queueRepository;
        this.messageRepository = messageRepository;
        this.messageMaskingService = messageMaskingService;
        this.meterRegistry = meterRegistry;
        this.observabilityService = observabilityService;
    }

    @Transactional
    public void enqueue(String username, MessageType type, String title, String content) {
        enqueue(username, type, title, content, SensitivityLevel.MEDIUM);
    }

    @Transactional
    public void enqueue(String username, MessageType type, String title, String content, SensitivityLevel sensitivityLevel) {
        MessageQueueEventEntity event = new MessageQueueEventEntity();
        event.setUsername(username);
        event.setType(type);
        event.setTitle(title);
        event.setContent(content);
        event.setSensitivityLevel(sensitivityLevel == null ? SensitivityLevel.MEDIUM : sensitivityLevel);
        event.setTraceId(TraceContext.getTraceId());
        event.setStatus(QueueStatus.PENDING);
        queueRepository.save(event);
    }

    @Scheduled(fixedDelayString = "${app.message.queue-process-ms:10000}")
    @Transactional
    public void processQueue() {
        Timer.Sample sample = Timer.start(meterRegistry);
        List<MessageQueueEventEntity> events = queueRepository.findTop100ByStatusOrderByCreatedAtAsc(QueueStatus.PENDING);
        for (MessageQueueEventEntity event : events) {
            MessageEntity message = new MessageEntity();
            message.setUsername(event.getUsername());
            message.setType(event.getType());
            message.setTitle(event.getTitle());
            message.setContent(event.getContent());
            message.setSensitivityLevel(event.getSensitivityLevel());
            message.setTraceId(event.getTraceId());
            message.setRead(false);
            message.setMasked(event.getSensitivityLevel() != SensitivityLevel.LOW);
            messageRepository.save(message);

            event.setStatus(QueueStatus.PROCESSED);
            queueRepository.save(event);
        }
        sample.stop(Timer.builder("app.workflow.queue.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        observabilityService.recordWorkflowLog("queue", "processed=" + events.size());
    }
}
