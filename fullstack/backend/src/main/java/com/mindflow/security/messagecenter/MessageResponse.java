package com.mindflow.security.messagecenter;

import com.mindflow.security.message.SensitivityLevel;

import java.time.Instant;

public record MessageResponse(
        Long id,
        MessageType type,
        String title,
        String content,
        SensitivityLevel sensitivityLevel,
        boolean read,
        boolean masked,
        Instant createdAt
) {
}
