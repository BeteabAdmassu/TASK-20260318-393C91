package com.mindflow.security.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.security.auth.AuthService;
import com.mindflow.security.auth.LoginRequest;
import com.mindflow.security.user.Role;
import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setup() {
        ensureUser("pref_user", "pass12345", Role.PASSENGER);
        token = authService.login(new LoginRequest("pref_user", "pass12345")).token();
    }

    @Test
    void rejectsMissingDndTimesWhenDndEnabled() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "arrivalReminderEnabled", true,
                "reservationSuccessEnabled", true,
                "reminderLeadMinutes", 10,
                "dndEnabled", true,
                "dndStart", "",
                "dndEnd", ""
        ));

        mockMvc.perform(put("/api/passenger/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsInvalidLeadMinutes() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "arrivalReminderEnabled", true,
                "reservationSuccessEnabled", true,
                "reminderLeadMinutes", 0,
                "dndEnabled", false,
                "dndStart", "22:00",
                "dndEnd", "07:00"
        ));

        mockMvc.perform(put("/api/passenger/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private void ensureUser(String username, String password, Role role) {
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
