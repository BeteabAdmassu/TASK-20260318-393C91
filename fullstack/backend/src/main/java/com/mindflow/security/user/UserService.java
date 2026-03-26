package com.mindflow.security.user;

import com.mindflow.security.common.ResourceConflictException;
import com.mindflow.security.common.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String tenantId = TenantContext.getTenantId();
        userRepository.findByUsernameAndTenantId(request.username(), tenantId).ifPresent(existing -> {
            throw new ResourceConflictException("Username already exists");
        });
        if (request.role() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);
        user.setArrivalReminderEnabled(true);
        user.setReservationSuccessEnabled(true);
        user.setReminderLeadMinutes(10);
        user.setTenantId(tenantId);
        UserEntity saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRole(), saved.isEnabled());
    }
}
