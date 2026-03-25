package com.mindflow.security.monitoring;

import org.slf4j.MDC;

public final class TraceContext {

    private TraceContext() {
    }

    public static String getTraceId() {
        String trace = MDC.get(TraceIdFilter.TRACE_ID);
        return trace == null ? "unknown" : trace;
    }
}
