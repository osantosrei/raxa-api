package com.raxa.domain.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raxa.domain.invite.Invite;
import com.raxa.domain.invite.InviteCodeGenerator;
import com.raxa.domain.invite.InviteRepository;
import com.raxa.domain.player.MatchPlayer;
import com.raxa.domain.player.MatchPlayerRepository;
import com.raxa.domain.user.User;
import com.raxa.domain.user.UserRepository;
import com.raxa.dto.request.CreateMatchRequest;
import com.raxa.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private InviteCodeGenerator inviteCodeGenerator;

    @Mock
    private UserRepository userRepository;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                matchPlayerRepository,
                inviteRepository,
                inviteCodeGenerator,
                userRepository
        );
    }

    @Test
    void createMatchAddsCreatorAsPlayerAndReturnsInviteCode() {
        UUID creatorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        User creator = buildUser(creatorId);
        CreateMatchRequest request = new CreateMatchRequest(
                "Pelada de quinta",
                "Arena Centro",
                LocalDateTime.now().plusHours(2),
                10
        );

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(matchId);
            return match;
        });
        when(inviteCodeGenerator.generate()).thenReturn("abc12345");
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(1);

        var response = matchService.createMatch(creatorId, request);

        assertThat(response.id()).isEqualTo(matchId);
        assertThat(response.title()).isEqualTo("Pelada de quinta");
        assertThat(response.currentPlayers()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(MatchStatus.OPEN);
        assertThat(response.inviteCode()).isEqualTo("abc12345");
        assertThat(response.creator().id()).isEqualTo(creatorId);

        verify(matchPlayerRepository).save(any(MatchPlayer.class));

        ArgumentCaptor<Invite> inviteCaptor = ArgumentCaptor.forClass(Invite.class);
        verify(inviteRepository).save(inviteCaptor.capture());
        assertThat(inviteCaptor.getValue().getCode()).isEqualTo("abc12345");
    }

    @Test
    void createMatchLessThanOneHourAheadThrowsBusinessException() {
        UUID creatorId = UUID.randomUUID();
        CreateMatchRequest request = new CreateMatchRequest(
                "Pelada agora",
                "Arena Centro",
                LocalDateTime.now().plusMinutes(30),
                10
        );

        assertThatThrownBy(() -> matchService.createMatch(creatorId, request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).contains("pelo menos 1 hora")
                );
    }

    @Test
    void cancelMatchByNonCreatorThrowsForbidden() {
        UUID creatorId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, requesterId))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo("Somente o criador pode cancelar a partida");
                });
    }

    private Match buildMatch(UUID matchId, User creator) {
        Match match = new Match();
        match.setId(matchId);
        match.setCreator(creator);
        match.setTitle("Pelada");
        match.setLocation("Arena");
        match.setScheduledAt(LocalDateTime.now().plusHours(2));
        match.setMaxPlayers(10);
        match.setStatus(MatchStatus.OPEN);
        return match;
    }

    private User buildUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setName("João Silva");
        user.setEmail("joao@test.com");
        user.setPasswordHash("hashed");
        return user;
    }
}
