package com.mindflow.security.messagecenter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByUsernameOrderByCreatedAtDesc(String username);

    List<MessageEntity> findByUsernameAndTenantIdOrderByCreatedAtDesc(String username, String tenantId);

    List<MessageEntity> findByUsernameAndTypeOrderByCreatedAtDesc(String username, MessageType type);

    List<MessageEntity> findByUsernameAndTypeAndTenantIdOrderByCreatedAtDesc(String username, MessageType type, String tenantId);
}
