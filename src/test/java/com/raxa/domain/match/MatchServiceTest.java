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
import java.util.List;
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

    @Test
    void cancelMatchWhenScheduledAtIsPastThrowsBusinessException() {
        UUID creatorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, creatorId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).contains("já realizada")
                );
    }

    @Test
    void joinMatchWhenFullThrowsBusinessException() {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(UUID.randomUUID()));
        match.setStatus(MatchStatus.FULL);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId)));
        when(matchPlayerRepository.countByMatchIdForUpdate(matchId)).thenReturn(10);

        assertThatThrownBy(() -> matchService.joinMatch(matchId, userId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).isEqualTo("Partida sem vagas disponíveis.")
                );
    }

    @Test
    void joinMatchWhenScheduledAtIsPastThrowsBusinessException() {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(UUID.randomUUID()));
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId)));
        when(matchPlayerRepository.countByMatchIdForUpdate(matchId)).thenReturn(1);

        assertThatThrownBy(() -> matchService.joinMatch(matchId, userId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).contains("já ocorreu")
                );
    }

    @Test
    void joinMatchWhenLastSlotSetsStatusToFull() {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(UUID.randomUUID()));
        match.setMaxPlayers(2);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId)));
        when(matchPlayerRepository.countByMatchIdForUpdate(matchId)).thenReturn(1);
        when(matchPlayerRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(false);
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(2);

        var response = matchService.joinMatch(matchId, userId);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FULL);
        assertThat(response.status()).isEqualTo(MatchStatus.FULL);
        assertThat(response.currentPlayers()).isEqualTo(2);
        verify(matchPlayerRepository).save(any(MatchPlayer.class));
        verify(matchRepository).save(match);
    }

    @Test
    void getMatchWhenScheduledAtIsPastReturnsFinishedWithoutInviteCode() {
        UUID creatorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(4);

        var response = matchService.getMatch(matchId, creatorId);

        assertThat(response.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(response.inviteCode()).isNull();
        assertThat(response.currentPlayers()).isEqualTo(4);
    }

    @Test
    void listByUserKeepsFinishedMatchesVisible() {
        UUID creatorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));

        when(matchRepository.findAllByUserId(creatorId)).thenReturn(List.of(match));
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(4);

        var response = matchService.listByUser(creatorId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(response.getFirst().inviteCode()).isNull();
    }

    @Test
    void cancelledMatchKeepsCancelledStatusEvenWhenScheduledAtIsPast() {
        UUID creatorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        match.setStatus(MatchStatus.CANCELLED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(4);

        var response = matchService.getMatch(matchId, creatorId);

        assertThat(response.status()).isEqualTo(MatchStatus.CANCELLED);
    }

    @Test
    void leaveMatchWhenCreatorThrowsBusinessException() {
        UUID creatorId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.leaveMatch(matchId, creatorId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).contains("criador")
                );
    }

    @Test
    void leaveMatchWhenScheduledAtIsPastThrowsBusinessException() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));
        match.setScheduledAt(LocalDateTime.now().minusMinutes(1));

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);

        assertThatThrownBy(() -> matchService.leaveMatch(matchId, userId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getMessage()).contains("já realizada")
                );
    }

    @Test
    void leaveMatchWhenFullSetsStatusToOpen() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Match match = buildMatch(matchId, buildUser(creatorId));
        match.setStatus(MatchStatus.FULL);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);

        matchService.leaveMatch(matchId, userId);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.OPEN);
        verify(matchPlayerRepository).deleteByMatchIdAndUserId(matchId, userId);
        verify(matchRepository).save(match);
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
