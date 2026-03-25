package com.mindflow.security.auth;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupUsers() {
        ensureUser("p_user", "pass12345", Role.PASSENGER);
        ensureUser("d_user", "dispatch123", Role.DISPATCHER);
        ensureUser("a_user", "admin12345", Role.ADMIN);
    }

    @Test
    void passengerCannotAccessDispatcherOrAdminRoutes() throws Exception {
        String token = authService.login(new LoginRequest("p_user", "pass12345")).token();
        mockMvc.perform(get("/api/passenger/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dispatcher/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void dispatcherCannotAccessAdminRoute() throws Exception {
        String token = authService.login(new LoginRequest("d_user", "dispatch123")).token();
        mockMvc.perform(get("/api/passenger/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dispatcher/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
