package com.mindflow.security.message;

import com.mindflow.security.user.Role;
import org.springframework.stereotype.Service;

@Service
public class MessagePrivacyService {

    public String desensitize(String message, SensitivityLevel sensitivityLevel, Role role) {
        if (message == null || message.isBlank()) {
            return message;
        }
        if (sensitivityLevel == null) {
            sensitivityLevel = SensitivityLevel.LOW;
        }

        return switch (sensitivityLevel) {
            case LOW -> message;
            case MEDIUM -> role == Role.PASSENGER ? partiallyMask(message) : message;
            case HIGH -> {
                if (role == Role.ADMIN) {
                    yield message;
                }
                yield "[REDACTED]";
            }
        };
    }

    private String partiallyMask(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        int keep = Math.max(2, value.length() / 4);
        int maskCount = value.length() - (keep * 2);
        if (maskCount < 1) {
            maskCount = 1;
        }

        String prefix = value.substring(0, keep);
        String suffix = value.substring(value.length() - keep);
        return prefix + "*".repeat(maskCount) + suffix;
    }
}
