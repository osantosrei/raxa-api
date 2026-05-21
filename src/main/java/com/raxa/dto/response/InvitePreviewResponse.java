package com.raxa.dto.response;

import com.raxa.domain.match.MatchStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvitePreviewResponse(
        UUID matchId,
        String title,
        String location,
        LocalDateTime scheduledAt,
        int maxPlayers,
        int currentPlayers,
        MatchStatus status,
        String inviteCode
) {
}
