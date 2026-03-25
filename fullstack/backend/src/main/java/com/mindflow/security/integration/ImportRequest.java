package com.mindflow.security.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportRequest(
        @NotNull ImportFormat format,
        @NotBlank String sourceName,
        @NotBlank String payload
) {
}
