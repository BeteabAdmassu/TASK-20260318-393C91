package com.mindflow.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.findByUsername("dispatcher01").ifPresent(userRepository::delete);
        UserEntity user = new UserEntity();
        user.setUsername("dispatcher01");
        user.setPasswordHash(passwordEncoder.encode("dispatch123"));
        user.setRole(Role.DISPATCHER);
        user.setEnabled(true);
        user.setTenantId("default");
        userRepository.save(user);
    }

    @Test
    void loginReturnsJwtWhenCredentialsValid() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dispatcher01",
                "password", "dispatch123"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("DISPATCHER"));
    }

    @Test
    void loginFailsWhenCredentialsInvalid() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dispatcher01",
                "password", "wrongpass"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginFailsValidationWhenPasswordTooShort() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dispatcher01",
                "password", "short"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
