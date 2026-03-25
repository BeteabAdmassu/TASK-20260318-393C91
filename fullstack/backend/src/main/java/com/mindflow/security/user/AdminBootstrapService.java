package com.mindflow.security.user;

import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    public AdminBootstrapService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 AdminBootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @PostConstruct
    @Transactional
    public void ensureAdminExists() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getPassword() == null || properties.getPassword().length() < 8) {
            throw new IllegalStateException("Bootstrap admin password must be at least 8 characters");
        }
        userRepository.findByUsername(properties.getUsername()).ifPresentOrElse(
                existing -> {
                },
                () -> {
                    UserEntity user = new UserEntity();
                    user.setUsername(properties.getUsername());
                    user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
                    user.setRole(Role.ADMIN);
                    user.setEnabled(true);
                    user.setArrivalReminderEnabled(true);
                    user.setReservationSuccessEnabled(true);
                    user.setReminderLeadMinutes(10);
                    userRepository.save(user);
                }
        );
    }
}
