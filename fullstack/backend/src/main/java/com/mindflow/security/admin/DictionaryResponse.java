package com.mindflow.security.admin;

public record DictionaryResponse(
        Long id,
        String category,
        String code,
        String value,
        boolean enabled
) {
}
