package com.mindflow.security.monitoring;

import com.mindflow.security.messagecenter.MessageQueueEventRepository;
import com.mindflow.security.messagecenter.QueueStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);
    private static final long MAX_REASONABLE_P95_MS = 300_000;

    private final MeterRegistry meterRegistry;
    private final MessageQueueEventRepository queueEventRepository;
    private final AlertDiagnosticRepository alertRepository;
    private final MonitoringProperties properties;

    public ObservabilityService(MeterRegistry meterRegistry,
                                MessageQueueEventRepository queueEventRepository,
                                AlertDiagnosticRepository alertRepository,
                                MonitoringProperties properties) {
        this.meterRegistry = meterRegistry;
        this.queueEventRepository = queueEventRepository;
        this.alertRepository = alertRepository;
        this.properties = properties;
    }

    public void recordWorkflowLog(String workflow, String detail) {
        String trace = TraceContext.getTraceId();
        log.info("event=workflow activity={} traceId={} detail={}", workflow, trace, detail);
    }

    @Scheduled(fixedDelayString = "${app.monitoring.check-ms:30000}")
    @Transactional
    public void evaluateAlerts() {
        int backlog = queueEventRepository.findTop100ByStatusOrderByCreatedAtAsc(QueueStatus.PENDING).size();
        meterRegistry.gauge("app.queue.backlog", backlog);

        if (backlog > properties.getQueueBacklogThreshold()) {
            createAlert("QUEUE_BACKLOG", "WARN",
                    "Queue backlog is " + backlog + " which exceeds threshold " + properties.getQueueBacklogThreshold());
        }

        Timer timer = meterRegistry.find("app.api.request.duration").timer();
        if (timer != null) {
            long p95Ms = Math.round(readApiP95Ms(timer));
            if (p95Ms > MAX_REASONABLE_P95_MS) {
                log.warn("event=alert-skip type=API_P95 reason=unrealistic_value valueMs={}", p95Ms);
                return;
            }
            if (p95Ms > properties.getApiP95ThresholdMs()) {
                createAlert("API_P95", "WARN",
                        "API p95 latency is " + p95Ms + "ms which exceeds threshold " + properties.getApiP95ThresholdMs() + "ms");
            }
        }
    }

    private double readApiP95Ms(Timer timer) {
        double fromTimer = timer.percentile(0.95, TimeUnit.MILLISECONDS);
        if (Double.isFinite(fromTimer) && fromTimer > 0) {
            return fromTimer;
        }
        return extractPercentileMs(timer, 0.95);
    }

    @Transactional(readOnly = true)
    public List<AlertDiagnosticEntity> recentDiagnostics() {
        return alertRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private void createAlert(String type, String severity, String message) {
        AlertDiagnosticEntity alert = new AlertDiagnosticEntity();
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setMessage(message + " @ " + Instant.now());
        alertRepository.save(alert);
        log.warn("event=alert type={} severity={} message={}", type, severity, message);
    }

    private double extractPercentileMs(Timer timer, double percentile) {
        ValueAtPercentile[] values = timer.takeSnapshot().percentileValues();
        if (values.length == 0) {
            return 0;
        }
        for (ValueAtPercentile candidate : values) {
            if (Math.abs(candidate.percentile() - percentile) < 0.001) {
                return toMillis(candidate.value(), timer.baseTimeUnit());
            }
        }
        return toMillis(values[values.length - 1].value(), timer.baseTimeUnit());
    }

    static double toMillis(double value, TimeUnit baseUnit) {
        return switch (baseUnit) {
            case NANOSECONDS -> value / 1_000_000d;
            case MICROSECONDS -> value / 1_000d;
            case MILLISECONDS -> value;
            case SECONDS -> value * 1_000d;
            case MINUTES -> value * 60_000d;
            case HOURS -> value * 3_600_000d;
            case DAYS -> value * 86_400_000d;
        };
    }
}
