package com.raxa.dto.response;

import com.raxa.domain.match.MatchStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        String title,
        String location,
        LocalDateTime scheduledAt,
        int maxPlayers,
        int currentPlayers,
        MatchStatus status,
        UserResponse creator,
        String inviteCode,
        Instant createdAt
) {
}
