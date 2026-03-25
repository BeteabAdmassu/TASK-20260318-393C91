package com.mindflow.security.user;

public record UserResponse(
        Long id,
        String username,
        Role role,
        boolean enabled
) {
}
