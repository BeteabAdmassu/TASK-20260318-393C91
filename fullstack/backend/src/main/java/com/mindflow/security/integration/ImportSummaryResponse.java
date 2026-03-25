package com.mindflow.security.integration;

import java.util.List;

public record ImportSummaryResponse(
        ImportJobResponse job,
        List<String> notes
) {
}
