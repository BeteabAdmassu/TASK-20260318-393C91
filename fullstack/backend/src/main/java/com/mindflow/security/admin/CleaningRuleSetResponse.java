package com.mindflow.security.admin;

public record CleaningRuleSetResponse(
        String areaUnit,
        String priceUnit,
        String missingValueMarker,
        boolean trimEnabled
) {
}
