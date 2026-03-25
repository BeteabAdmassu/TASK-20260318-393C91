package com.mindflow.security.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.monitoring")
public class MonitoringProperties {

    private int queueBacklogThreshold = 100;
    private long apiP95ThresholdMs = 500;

    public int getQueueBacklogThreshold() {
        return queueBacklogThreshold;
    }

    public void setQueueBacklogThreshold(int queueBacklogThreshold) {
        this.queueBacklogThreshold = queueBacklogThreshold;
    }

    public long getApiP95ThresholdMs() {
        return apiP95ThresholdMs;
    }

    public void setApiP95ThresholdMs(long apiP95ThresholdMs) {
        this.apiP95ThresholdMs = apiP95ThresholdMs;
    }
}
