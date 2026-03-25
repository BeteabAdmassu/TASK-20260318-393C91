package com.mindflow.security.search;

import java.util.List;

public record SearchResponse(
        String query,
        List<String> suggestions,
        List<SearchResult> results
) {
}
