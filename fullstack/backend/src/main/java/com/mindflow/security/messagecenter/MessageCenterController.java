package com.mindflow.security.messagecenter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import com.mindflow.security.user.Role;
import com.mindflow.security.common.TenantContext;

@RestController
@RequestMapping("/api/passenger/messages-center")
@Validated
public class MessageCenterController {

    private final MessageCenterService messageCenterService;
    private final BookingEventRepository bookingRepository;

    public MessageCenterController(MessageCenterService messageCenterService,
                                   BookingEventRepository bookingRepository) {
        this.messageCenterService = messageCenterService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<List<MessageResponse>> list(
            @RequestParam(name = "type", required = false) MessageType type,
            Authentication authentication) {
        Role role = extractRole(authentication);
        return ResponseEntity.ok(messageCenterService.listMessages(authentication.getName(), role, type));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<MessageResponse> markRead(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MessageReadRequest request,
            Authentication authentication) {
        Role role = extractRole(authentication);
        return ResponseEntity.ok(messageCenterService.markRead(authentication.getName(), role, id, request.read()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id,
            Authentication authentication) {
        messageCenterService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/booking-events")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<Map<String, Long>> createBooking(
            @Valid @RequestBody BookingEventRequest request,
            Authentication authentication) {
        BookingEventEntity event = new BookingEventEntity();
        event.setUsername(authentication.getName());
        event.setRouteNumber(request.routeNumber());
        event.setPassengerPhoneToken(hashToken(request.passengerPhone()));
        event.setPassengerIdCardToken(hashToken(request.passengerIdCard()));
        event.setStartTime(request.startTime());
        event.setReservationSuccessSent(false);
        event.setArrivalReminderSent(false);
        event.setMissedCheckInSent(false);
        event.setTenantId(TenantContext.getTenantId());
        BookingEventEntity saved = bookingRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("bookingEventId", saved.getId()));
    }

    private String hashToken(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : out) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private Role extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .map(Role::valueOf)
                .orElse(Role.PASSENGER);
    }
}
