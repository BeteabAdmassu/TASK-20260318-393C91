package com.mindflow.security.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccessProbeController {

    @GetMapping("/passenger/ping")
    public ResponseEntity<Map<String, String>> passengerPing() {
        return ResponseEntity.ok(Map.of("message", "Passenger scope granted"));
    }

    @GetMapping("/dispatcher/ping")
    public ResponseEntity<Map<String, String>> dispatcherPing() {
        return ResponseEntity.ok(Map.of("message", "Dispatcher scope granted"));
    }

    @GetMapping("/admin/ping")
    public ResponseEntity<Map<String, String>> adminPing() {
        return ResponseEntity.ok(Map.of("message", "Admin scope granted"));
    }
}
