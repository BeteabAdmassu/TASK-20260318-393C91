package com.mindflow.security.messagecenter;

import com.mindflow.security.auth.AuthService;
import com.mindflow.security.auth.LoginRequest;
import com.mindflow.security.message.SensitivityLevel;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageCenterAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MessageRepository messageRepository;

    private String ownerToken;
    private String otherUserToken;
    private Long messageId;

    @BeforeEach
    void setup() {
        ensureUser("msg_owner", "pass12345", Role.PASSENGER);
        ensureUser("msg_other", "pass12345", Role.PASSENGER);

        ownerToken = authService.login(new LoginRequest("msg_owner", "pass12345")).token();
        otherUserToken = authService.login(new LoginRequest("msg_other", "pass12345")).token();

        MessageEntity message = new MessageEntity();
        message.setUsername("msg_owner");
        message.setType(MessageType.ARRIVAL_REMINDER);
        message.setTitle("Arrival Reminder");
        message.setContent("Bus arrives in 10 minutes.");
        message.setSensitivityLevel(SensitivityLevel.MEDIUM);
        message.setTraceId("trace-test-1");
        message.setRead(false);
        message.setMasked(false);
        messageId = messageRepository.save(message).getId();
    }

    @Test
    void nonOwnerCannotMarkAnotherUsersMessageAsRead() throws Exception {
        mockMvc.perform(put("/api/passenger/messages-center/" + messageId + "/read")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void nonOwnerCannotDeleteAnotherUsersMessage() throws Exception {
        mockMvc.perform(delete("/api/passenger/messages-center/" + messageId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void ownerCanMarkAndDeleteOwnMessage() throws Exception {
        mockMvc.perform(put("/api/passenger/messages-center/" + messageId + "/read")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(delete("/api/passenger/messages-center/" + messageId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
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
