package com.mindflow.security.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CleaningRuleSetRequest(
        @NotBlank String areaUnit,
        @NotBlank String priceUnit,
        @NotBlank String missingValueMarker,
        @NotNull Boolean trimEnabled
) {
}
