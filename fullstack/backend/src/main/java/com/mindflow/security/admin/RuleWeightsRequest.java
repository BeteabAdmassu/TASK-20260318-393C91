package com.mindflow.security.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RuleWeightsRequest(
        @NotNull @Min(1) Integer relevanceWeight,
        @NotNull @Min(1) Integer frequencyWeight,
        @NotNull @Min(1) Integer popularityWeight,
        @Pattern(regexp = "^(BLENDED|STRICT_FREQUENCY_POPULARITY)$") String rankingMode
) {
}
