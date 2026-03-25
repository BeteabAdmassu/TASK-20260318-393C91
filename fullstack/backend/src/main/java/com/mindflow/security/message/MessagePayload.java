package com.mindflow.security.message;

import jakarta.validation.constraints.NotBlank;

public record MessagePayload(
        @NotBlank String content,
        SensitivityLevel sensitivityLevel
) {
}
