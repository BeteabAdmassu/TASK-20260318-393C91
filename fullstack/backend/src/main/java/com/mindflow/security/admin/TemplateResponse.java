package com.mindflow.security.admin;

public record TemplateResponse(
        Long id,
        String templateKey,
        String subject,
        String body
) {
}
