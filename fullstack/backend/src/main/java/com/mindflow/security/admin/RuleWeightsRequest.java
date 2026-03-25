package com.mindflow.security.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RuleWeightsRequest(
        @NotNull @Min(1) Integer relevanceWeight,
        @NotNull @Min(1) Integer frequencyWeight,
        @NotNull @Min(1) Integer popularityWeight
) {
}
