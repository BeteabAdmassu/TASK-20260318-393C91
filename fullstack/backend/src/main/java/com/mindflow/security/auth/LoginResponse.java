package com.mindflow.security.auth;

import com.mindflow.security.user.Role;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String username,
        Role role
) {
}
