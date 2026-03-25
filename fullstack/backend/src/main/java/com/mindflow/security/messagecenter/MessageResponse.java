package com.mindflow.security.messagecenter;

import java.time.Instant;

public record MessageResponse(
        Long id,
        MessageType type,
        String title,
        String content,
        boolean read,
        boolean masked,
        Instant createdAt
) {
}
