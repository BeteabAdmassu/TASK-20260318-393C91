package com.mindflow.security.messagecenter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageQueueEventRepository extends JpaRepository<MessageQueueEventEntity, Long> {
    List<MessageQueueEventEntity> findTop100ByStatusOrderByCreatedAtAsc(QueueStatus status);

    List<MessageQueueEventEntity> findTop100ByStatusAndTenantIdOrderByCreatedAtAsc(QueueStatus status, String tenantId);

    Optional<MessageQueueEventEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<MessageQueueEventEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, String tenantId);
}
