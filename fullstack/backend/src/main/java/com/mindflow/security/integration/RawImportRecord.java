package com.mindflow.security.integration;

import java.util.Map;

public record RawImportRecord(
        String sourceRef,
        Map<String, String> fields,
        String rawSnapshot
) {
}
