package com.mindflow.security.workflow;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowAuthorizationTest {

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

    private String d1Token;
    private String d2Token;

    @BeforeEach
    void setup() {
        ensureUser("dispatcher_a", "dispatch123", Role.DISPATCHER);
        ensureUser("dispatcher_b", "dispatch123", Role.DISPATCHER);
        d1Token = authService.login(new LoginRequest("dispatcher_a", "dispatch123")).token();
        d2Token = authService.login(new LoginRequest("dispatcher_b", "dispatch123")).token();
    }

    @Test
    void dispatcherCannotAccessAnotherDispatchersTask() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "type", "ROUTE_DATA_CHANGE",
                "mode", "CONDITIONAL",
                "title", "Task A",
                "payload", "{\"route\":\"A1\"}",
                "requiredApprovals", 1
        ));

        String created = mockMvc.perform(post("/api/dispatcher/workflows")
                        .header("Authorization", "Bearer " + d1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/dispatcher/workflows/" + id)
                        .header("Authorization", "Bearer " + d2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(put("/api/dispatcher/workflows/" + id + "/approve")
                        .header("Authorization", "Bearer " + d2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void collaboratorCanApproveJointWorkflowTask() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "type", "ROUTE_DATA_CHANGE",
                "mode", "JOINT",
                "title", "Joint Task",
                "payload", "{\"route\":\"A1\"}",
                "requiredApprovals", 2,
                "collaborators", new String[]{"dispatcher_b"}
        ));

        String created = mockMvc.perform(post("/api/dispatcher/workflows")
                        .header("Authorization", "Bearer " + d1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/dispatcher/workflows/" + id + "/approve")
                        .header("Authorization", "Bearer " + d2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"joint approval\"}"))
                .andExpect(status().isOk());
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
