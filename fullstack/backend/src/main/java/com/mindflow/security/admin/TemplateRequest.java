package com.mindflow.security.admin;

import jakarta.validation.constraints.NotBlank;

public record TemplateRequest(
        @NotBlank String templateKey,
        @NotBlank String subject,
        @NotBlank String body
) {
}
