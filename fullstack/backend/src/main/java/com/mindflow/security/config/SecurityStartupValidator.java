package com.mindflow.security.config;

import com.mindflow.security.user.AdminBootstrapProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;

@Component
public class SecurityStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);

    private final JwtProperties jwtProperties;
    private final AdminBootstrapProperties adminBootstrapProperties;
    private final Environment environment;

    public SecurityStartupValidator(JwtProperties jwtProperties,
                                    AdminBootstrapProperties adminBootstrapProperties,
                                    Environment environment) {
        this.jwtProperties = jwtProperties;
        this.adminBootstrapProperties = adminBootstrapProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        validateJwtSecret();
        validateBootstrapPassword();
        validateProductionBootstrapGuard();
    }

    private void validateJwtSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required and must not be blank");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length < 32) {
                throw new IllegalStateException("JWT_SECRET must decode to at least 256 bits");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64", ex);
        }
    }

    private void validateBootstrapPassword() {
        if (!adminBootstrapProperties.isEnabled()) {
            return;
        }
        String password = adminBootstrapProperties.getPassword();
        if (password == null || password.length() < 8) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters when bootstrap is enabled");
        }
        if ("admin1234".equals(password) || "changeMeNow123!".equals(password)) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD cannot use insecure sample/default value");
        }
    }

    private void validateProductionBootstrapGuard() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));

        if (production && adminBootstrapProperties.isEnabled()) {
            throw new IllegalStateException("Bootstrap admin creation is not allowed in production profile");
        }

        if (adminBootstrapProperties.isEnabled()) {
            log.warn("Bootstrap admin creation is ENABLED. Use only for controlled local initialization.");
        }
    }
}
