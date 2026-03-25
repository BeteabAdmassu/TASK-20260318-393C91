package com.mindflow.security.message;

import com.mindflow.security.user.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/passenger/messages")
public class MessageController {

    private final MessagePrivacyService messagePrivacyService;

    public MessageController(MessagePrivacyService messagePrivacyService) {
        this.messagePrivacyService = messagePrivacyService;
    }

    @PostMapping("/desensitize")
    public ResponseEntity<MessageResponse> desensitize(@Valid @RequestBody MessagePayload payload,
                                                       Authentication authentication) {
        String roleAuthority = authentication.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No role found"))
                .getAuthority();

        Role role = Role.valueOf(roleAuthority.replace("ROLE_", ""));
        SensitivityLevel level = payload.sensitivityLevel() == null ? SensitivityLevel.LOW : payload.sensitivityLevel();
        String masked = messagePrivacyService.desensitize(payload.content(), level, role);

        return ResponseEntity.ok(new MessageResponse(payload.content(), masked, level, role.name()));
    }
}
