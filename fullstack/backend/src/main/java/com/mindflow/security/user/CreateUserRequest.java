package com.mindflow.security.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        Role role
) {
}
