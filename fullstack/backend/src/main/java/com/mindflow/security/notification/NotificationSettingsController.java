package com.mindflow.security.notification;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passenger/preferences")
public class NotificationSettingsController {

    private final NotificationPreferenceService preferenceService;

    public NotificationSettingsController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASSENGER', 'DISPATCHER', 'ADMIN')")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(Authentication authentication) {
        return ResponseEntity.ok(preferenceService.getPreferences(authentication.getName()));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PASSENGER', 'DISPATCHER', 'ADMIN')")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(preferenceService.updatePreferences(authentication.getName(), request));
    }
}
