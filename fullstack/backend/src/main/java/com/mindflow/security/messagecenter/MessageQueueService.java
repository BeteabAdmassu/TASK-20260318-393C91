package com.mindflow.security.messagecenter;

import com.mindflow.security.message.SensitivityLevel;
import com.mindflow.security.common.TenantContext;
import com.mindflow.security.monitoring.ObservabilityService;
import com.mindflow.security.monitoring.TraceContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        String tenantId = TenantContext.getTenantId();
        String idempotencyKey = buildIdempotencyKey(username, type, title, content, TraceContext.getTraceId());
        if (queueRepository.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId).isPresent()) {
            return;
        }

        MessageQueueEventEntity event = new MessageQueueEventEntity();
        event.setUsername(username);
        event.setType(type);
        event.setTitle(title);
        event.setContent(content);
        event.setIdempotencyKey(idempotencyKey);
        event.setSensitivityLevel(sensitivityLevel == null ? SensitivityLevel.MEDIUM : sensitivityLevel);
        event.setTraceId(TraceContext.getTraceId());
        event.setStatus(QueueStatus.PENDING);
        event.setTenantId(tenantId);
        try {
            queueRepository.save(event);
        } catch (DataIntegrityViolationException ignored) {
            // Deduplicated by unique idempotency key under race.
        }
    }

    @Scheduled(fixedDelayString = "${app.message.queue-process-ms:10000}")
    @Transactional
    public void processQueue() {
        String tenantId = TenantContext.getTenantId();
        Timer.Sample sample = Timer.start(meterRegistry);
        List<MessageQueueEventEntity> events = queueRepository.findTop100ByStatusAndTenantIdOrderByCreatedAtAsc(QueueStatus.PENDING, tenantId);
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
            message.setTenantId(event.getTenantId());
            messageRepository.save(message);

            event.setStatus(QueueStatus.PROCESSED);
            queueRepository.save(event);
        }
        sample.stop(Timer.builder("app.workflow.queue.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        observabilityService.recordWorkflowLog("queue", "processed=" + events.size());
    }

    private String buildIdempotencyKey(String username, MessageType type, String title, String content, String traceId) {
        String payload = String.join("|",
                username == null ? "" : username,
                type == null ? "" : type.name(),
                title == null ? "" : title,
                content == null ? "" : content,
                traceId == null ? "" : traceId);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
