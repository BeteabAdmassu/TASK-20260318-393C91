package com.mindflow.security.auth;

import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import com.mindflow.security.common.TenantContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantId = TenantContext.getTenantId();
        UserEntity user = userRepository.findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return new UserPrincipal(user.getUsername(), user.getPasswordHash(), user.getRole(), user.isEnabled(), user.getTenantId());
    }
}
