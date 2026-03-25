package com.mindflow.security.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    public RequestTimingFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.nanoTime() - start;
            Timer.builder("app.api.request.duration")
                    .tag("method", request.getMethod())
                    .tag("path", normalizePath(request.getRequestURI()))
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);
        }
    }

    private String normalizePath(String uri) {
        if (uri == null) {
            return "unknown";
        }
        return uri.replaceAll("/\\d+", "/{id}");
    }
}
