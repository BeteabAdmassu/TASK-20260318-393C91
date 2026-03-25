package com.mindflow.security.message;

public record MessageResponse(
        String original,
        String masked,
        SensitivityLevel sensitivityLevel,
        String visibleToRole
) {
}
