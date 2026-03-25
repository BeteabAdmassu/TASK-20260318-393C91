package com.mindflow.security.admin;

public record RuleWeightsResponse(
        int relevanceWeight,
        int frequencyWeight,
        int popularityWeight
) {
}
