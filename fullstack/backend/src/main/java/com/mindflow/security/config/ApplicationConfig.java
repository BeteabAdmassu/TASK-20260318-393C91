package com.mindflow.security.config;

import com.mindflow.security.monitoring.MonitoringProperties;
import com.mindflow.security.user.AdminBootstrapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, AdminBootstrapProperties.class, MonitoringProperties.class})
public class ApplicationConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
