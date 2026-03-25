package com.mindflow.security.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataIntegrationControllerTest {

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

    private String adminToken;

    @BeforeEach
    void setup() {
        ensureUser("import_admin", "pass12345", Role.ADMIN);
        adminToken = authService.login(new LoginRequest("import_admin", "pass12345")).token();
    }

    @Test
    void htmlImportExposesAuditAndVersionEndpoints() throws Exception {
        String html = "<table><tr><th>stop</th><th>address</th><th>apartment</th><th>area</th><th>price</th></tr>"
                + "<tr><td>River Park</td><td>Main Rd 9</td><td>Studio</td><td>60m2</td><td>1800 rmb</td></tr>"
                + "</table>";

        String payload = objectMapper.writeValueAsString(Map.of(
                "format", "HTML",
                "sourceName", "html-seed",
                "payload", html
        ));

        String body = mockMvc.perform(post("/api/admin/integration/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.jobId").exists())
                .andReturn().getResponse().getContentAsString();

        long jobId = objectMapper.readTree(body).path("job").path("jobId").asLong();

        mockMvc.perform(get("/api/admin/integration/audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("jobId", String.valueOf(jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fieldName").exists());

        mockMvc.perform(get("/api/admin/integration/versions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("jobId", String.valueOf(jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stopName").exists());
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
