package com.mindflow.security.common;

import java.time.Instant;

public record ApiErrorResponse(
        int code,
        String msg,
        String traceId,
        Instant timestamp
) {
}
