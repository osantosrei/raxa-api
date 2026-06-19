package com.raxa.domain.invite;

import com.raxa.domain.match.Match;
import com.raxa.domain.match.MatchService;
import com.raxa.domain.player.MatchPlayerRepository;
import com.raxa.dto.response.InvitePreviewResponse;
import com.raxa.dto.response.MatchResponse;
import com.raxa.exception.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InviteService {

    private final InviteRepository inviteRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchService matchService;

    public InviteService(
            InviteRepository inviteRepository,
            MatchPlayerRepository matchPlayerRepository,
            MatchService matchService
    ) {
        this.inviteRepository = inviteRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchService = matchService;
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse resolveInvite(String code) {
        Invite invite = findActiveInvite(code);
        Match match = invite.getMatch();

        return new InvitePreviewResponse(
                match.getId(),
                match.getTitle(),
                match.getLocation(),
                match.getScheduledAt(),
                match.getMaxPlayers(),
                matchPlayerRepository.countByMatchId(match.getId()),
                match.getEffectiveStatus(),
                invite.getCode()
        );
    }

    public MatchResponse joinViaInvite(String code, UUID userId) {
        Invite invite = findActiveInvite(code);
        return matchService.joinMatch(invite.getMatch().getId(), userId);
    }

    public void deactivateInvite(UUID matchId, UUID requesterId) {
        Match match = matchService.findOrThrow(matchId);

        if (!match.getCreator().getId().equals(requesterId)) {
            throw new BusinessException("Somente o criador pode desativar o convite", HttpStatus.FORBIDDEN);
        }

        inviteRepository.findByMatchId(matchId).ifPresent(Invite::deactivate);
    }

    private Invite findActiveInvite(String code) {
        Invite invite = inviteRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new BusinessException("Convite inválido ou expirado", HttpStatus.NOT_FOUND));

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Convite inválido ou expirado", HttpStatus.NOT_FOUND);
        }

        return invite;
    }
}
