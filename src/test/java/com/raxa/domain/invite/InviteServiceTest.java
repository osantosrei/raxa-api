package com.raxa.domain.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.raxa.domain.match.Match;
import com.raxa.domain.match.MatchService;
import com.raxa.domain.match.MatchStatus;
import com.raxa.domain.player.MatchPlayerRepository;
import com.raxa.domain.user.User;
import com.raxa.dto.response.MatchResponse;
import com.raxa.dto.response.UserResponse;
import com.raxa.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchService matchService;

    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(inviteRepository, matchPlayerRepository, matchService);
    }

    @Test
    void resolveInviteReturnsPublicMatchPreview() {
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId);
        Invite invite = new Invite(match, "abc12345");

        when(inviteRepository.findByCodeAndActiveTrue("abc12345")).thenReturn(Optional.of(invite));
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(3);

        var response = inviteService.resolveInvite("abc12345");

        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.title()).isEqualTo("Pelada de quinta");
        assertThat(response.location()).isEqualTo("Arena Centro");
        assertThat(response.maxPlayers()).isEqualTo(10);
        assertThat(response.currentPlayers()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(MatchStatus.OPEN);
        assertThat(response.inviteCode()).isEqualTo("abc12345");
    }

    @Test
    void resolveInviteForFinishedMatchReturnsFinishedPreview() {
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId);
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        Invite invite = new Invite(match, "abc12345");

        when(inviteRepository.findByCodeAndActiveTrue("abc12345")).thenReturn(Optional.of(invite));
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(3);

        var response = inviteService.resolveInvite("abc12345");

        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(response.inviteCode()).isEqualTo("abc12345");
    }

    @Test
    void joinViaInviteDelegatesToMatchJoinRules() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId);
        Invite invite = new Invite(match, "abc12345");
        MatchResponse expected = new MatchResponse(
                matchId,
                match.getTitle(),
                match.getLocation(),
                match.getScheduledAt(),
                match.getMaxPlayers(),
                4,
                MatchStatus.OPEN,
                new UserResponse(match.getCreator().getId(), "Criador", "criador@test.com", null),
                null,
                Instant.now()
        );

        when(inviteRepository.findByCodeAndActiveTrue("abc12345")).thenReturn(Optional.of(invite));
        when(matchService.joinMatch(matchId, userId)).thenReturn(expected);

        var response = inviteService.joinViaInvite("abc12345", userId);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void joinViaInvitePropagatesFinishedMatchJoinBlock() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId);
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        Invite invite = new Invite(match, "abc12345");

        when(inviteRepository.findByCodeAndActiveTrue("abc12345")).thenReturn(Optional.of(invite));
        when(matchService.joinMatch(matchId, userId))
                .thenThrow(new BusinessException("Não é possível entrar em uma partida que já ocorreu."));

        assertThatThrownBy(() -> inviteService.joinViaInvite("abc12345", userId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).contains("já ocorreu")
                );
    }

    @Test
    void resolveInvalidInviteThrowsNotFound() {
        when(inviteRepository.findByCodeAndActiveTrue("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.resolveInvite("invalid"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getMessage()).isEqualTo("Convite inválido ou expirado");
                });
    }

    private Match buildMatch(UUID matchId) {
        User creator = new User();
        creator.setId(UUID.randomUUID());
        creator.setName("Criador");
        creator.setEmail("criador@test.com");
        creator.setPasswordHash("hashed");

        Match match = new Match();
        match.setId(matchId);
        match.setCreator(creator);
        match.setTitle("Pelada de quinta");
        match.setLocation("Arena Centro");
        match.setScheduledAt(LocalDateTime.now().plusHours(2));
        match.setMaxPlayers(10);
        match.setStatus(MatchStatus.OPEN);
        return match;
    }
}
