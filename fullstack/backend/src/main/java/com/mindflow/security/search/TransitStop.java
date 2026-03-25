package com.mindflow.security.search;

import java.util.List;

public record TransitStop(
        String routeNumber,
        String stopName,
        List<String> keywords,
        String pinyin,
        String initials,
        int frequencyPriority,
        int stopPopularity
) {
}
