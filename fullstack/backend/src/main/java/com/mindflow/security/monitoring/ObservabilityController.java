package com.mindflow.security.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/observability")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class ObservabilityController {

    private final MeterRegistry meterRegistry;
    private final ObservabilityService observabilityService;

    public ObservabilityController(MeterRegistry meterRegistry,
                                   ObservabilityService observabilityService) {
        this.meterRegistry = meterRegistry;
        this.observabilityService = observabilityService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<Map<String, Object>> snapshot() {
        double queueBacklog = meterRegistry.find("app.queue.backlog").gauge() == null
                ? 0
                : meterRegistry.find("app.queue.backlog").gauge().value();

        double apiP95Ms = percentileMs("app.api.request.duration", 0.95);
        double searchP95Ms = percentileMs("app.workflow.search.duration", 0.95);
        double parsingP95Ms = percentileMs("app.workflow.parsing.duration", 0.95);
        double queueP95Ms = percentileMs("app.workflow.queue.duration", 0.95);

        return ResponseEntity.ok(Map.of(
                "queueBacklog", Math.round(queueBacklog),
                "apiP95Ms", Math.round(apiP95Ms),
                "searchP95Ms", Math.round(searchP95Ms),
                "parsingP95Ms", Math.round(parsingP95Ms),
                "queueP95Ms", Math.round(queueP95Ms),
                "healthEndpoint", "/actuator/health",
                "metricsEndpoint", "/actuator/metrics"
        ));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> alerts() {
        List<Map<String, Object>> rows = observabilityService.recentDiagnostics().stream()
                .map(alert -> Map.<String, Object>of(
                        "id", alert.getId(),
                        "alertType", alert.getAlertType(),
                        "severity", alert.getSeverity(),
                        "message", alert.getMessage(),
                        "createdAt", alert.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(rows);
    }

    private double percentileMs(String metric, double percentile) {
        Timer timer = meterRegistry.find(metric).timer();
        if (timer == null || timer.takeSnapshot().percentileValues().length == 0) {
            return 0;
        }
        for (var p : timer.takeSnapshot().percentileValues()) {
            if (Math.abs(p.percentile() - percentile) < 0.001) {
                return ObservabilityService.toMillis(p.value(), timer.baseTimeUnit());
            }
        }
        return 0;
    }
}
