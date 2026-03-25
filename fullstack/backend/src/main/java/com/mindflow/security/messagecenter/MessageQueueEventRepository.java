package com.mindflow.security.messagecenter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageQueueEventRepository extends JpaRepository<MessageQueueEventEntity, Long> {
    List<MessageQueueEventEntity> findTop100ByStatusOrderByCreatedAtAsc(QueueStatus status);
}
