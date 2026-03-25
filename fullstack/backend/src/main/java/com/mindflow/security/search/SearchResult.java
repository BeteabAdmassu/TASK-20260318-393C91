package com.mindflow.security.search;

public record SearchResult(
        String routeNumber,
        String stopName,
        int frequencyPriority,
        int stopPopularity,
        long rankingScore,
        String matchType
) {
}
