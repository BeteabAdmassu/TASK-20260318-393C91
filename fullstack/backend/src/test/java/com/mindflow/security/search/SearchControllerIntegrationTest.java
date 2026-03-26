package com.mindflow.security.search;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String passengerToken;

    @BeforeEach
    void setup() {
        ensureUser("search_user", "pass12345", Role.PASSENGER);
        passengerToken = authService.login(new LoginRequest("search_user", "pass12345")).token();
    }

    @Test
    void passengerCanSearchAndGetStructuredResponse() throws Exception {
        mockMvc.perform(get("/api/passenger/search")
                        .header("Authorization", "Bearer " + passengerToken)
                        .param("q", "beijing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("beijing"))
                .andExpect(jsonPath("$.results").isArray());
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
