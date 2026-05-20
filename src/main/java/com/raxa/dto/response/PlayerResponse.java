package com.raxa.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PlayerResponse(
        UUID userId,
        String name,
        Instant joinedAt
) {
}
