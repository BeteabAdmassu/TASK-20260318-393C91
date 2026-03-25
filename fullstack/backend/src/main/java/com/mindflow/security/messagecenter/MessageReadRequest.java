package com.mindflow.security.messagecenter;

import jakarta.validation.constraints.NotNull;

public record MessageReadRequest(
        @NotNull Boolean read
) {
}
