package com.mindflow.security.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DictionaryRequest(
        @NotBlank String category,
        @NotBlank String code,
        @NotBlank String value,
        @NotNull Boolean enabled
) {
}
