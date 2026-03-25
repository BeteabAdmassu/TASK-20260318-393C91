package com.mindflow.security.message;

import com.mindflow.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagePrivacyServiceTest {

    private MessagePrivacyService service;

    @BeforeEach
    void setUp() {
        service = new MessagePrivacyService();
    }

    @Test
    void lowSensitivityReturnsOriginal() {
        String result = service.desensitize("Train on time", SensitivityLevel.LOW, Role.PASSENGER);
        assertEquals("Train on time", result);
    }

    @Test
    void mediumSensitivityMasksForPassenger() {
        String result = service.desensitize("ROUTE-77881", SensitivityLevel.MEDIUM, Role.PASSENGER);
        assertEquals("RO*******81", result);
    }

    @Test
    void highSensitivityRedactsForDispatcher() {
        String result = service.desensitize("securityCode=9291", SensitivityLevel.HIGH, Role.DISPATCHER);
        assertEquals("[REDACTED]", result);
    }

    @Test
    void highSensitivityVisibleToAdmin() {
        String result = service.desensitize("securityCode=9291", SensitivityLevel.HIGH, Role.ADMIN);
        assertEquals("securityCode=9291", result);
    }
}
