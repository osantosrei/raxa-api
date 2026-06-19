package com.raxa.domain.match;

import com.raxa.domain.invite.Invite;
import com.raxa.domain.invite.InviteCodeGenerator;
import com.raxa.domain.invite.InviteRepository;
import com.raxa.domain.player.MatchPlayer;
import com.raxa.domain.player.MatchPlayerRepository;
import com.raxa.domain.user.User;
import com.raxa.domain.user.UserRepository;
import com.raxa.dto.request.CreateMatchRequest;
import com.raxa.dto.response.MatchResponse;
import com.raxa.dto.response.PlayerResponse;
import com.raxa.dto.response.UserResponse;
import com.raxa.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final InviteRepository inviteRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final UserRepository userRepository;

    public MatchService(
            MatchRepository matchRepository,
            MatchPlayerRepository matchPlayerRepository,
            InviteRepository inviteRepository,
            InviteCodeGenerator inviteCodeGenerator,
            UserRepository userRepository
    ) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.inviteRepository = inviteRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.userRepository = userRepository;
    }

    public MatchResponse createMatch(UUID creatorId, CreateMatchRequest request) {
        validateScheduledAt(request.scheduledAt());

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        Match match = new Match();
        match.setCreator(creator);
        match.setTitle(request.title().trim());
        match.setLocation(request.location().trim());
        match.setScheduledAt(request.scheduledAt());
        match.setMaxPlayers(request.maxPlayers());
        match.setStatus(MatchStatus.OPEN);

        Match savedMatch = matchRepository.save(match);
        matchPlayerRepository.save(new MatchPlayer(savedMatch, creator));

        String inviteCode = createInvite(savedMatch);

        return toResponse(savedMatch, inviteCode);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listByUser(UUID userId) {
        return matchRepository.findAllByUserId(userId)
                .stream()
                .map(match -> toResponse(match, inviteCodeForCreator(match, userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId, UUID requesterId) {
        Match match = findOrThrow(matchId);
        return toResponse(match, inviteCodeForCreator(match, requesterId));
    }

    public void cancelMatch(UUID matchId, UUID requesterId) {
        Match match = findOrThrow(matchId);

        if (!match.getCreator().getId().equals(requesterId)) {
            throw new BusinessException("Somente o criador pode cancelar a partida", HttpStatus.FORBIDDEN);
        }

        if (match.getEffectiveStatus() == MatchStatus.FINISHED) {
            throw new BusinessException("Não é possível cancelar uma partida já realizada.");
        }

        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new BusinessException("Partida já cancelada");
        }

        match.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(match);
    }

    public MatchResponse joinMatch(UUID matchId, UUID userId) {
        Match match = findForUpdateOrThrow(matchId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        int currentPlayers = matchPlayerRepository.countByMatchIdForUpdate(matchId);

        validateJoin(match, userId, currentPlayers);

        matchPlayerRepository.save(new MatchPlayer(match, user));

        if (currentPlayers + 1 >= match.getMaxPlayers()) {
            match.setStatus(MatchStatus.FULL);
            matchRepository.save(match);
        }

        return toResponse(match, inviteCodeForCreator(match, userId));
    }

    public MatchResponse leaveMatch(UUID matchId, UUID userId) {
        Match match = findForUpdateOrThrow(matchId);
        MatchStatus effectiveStatus = match.getEffectiveStatus();
        boolean wasFull = match.getStatus() == MatchStatus.FULL;

        if (match.getCreator().getId().equals(userId)) {
            throw new BusinessException("O criador não pode sair da partida. Cancele-a em vez disso.");
        }

        if (!matchPlayerRepository.existsByMatchIdAndUserId(matchId, userId)) {
            throw new BusinessException("Você não está nessa partida.", HttpStatus.NOT_FOUND);
        }

        if (effectiveStatus == MatchStatus.FINISHED) {
            throw new BusinessException("Não é possível sair de uma partida já realizada.");
        }

        matchPlayerRepository.deleteByMatchIdAndUserId(matchId, userId);

        if (wasFull) {
            match.setStatus(MatchStatus.OPEN);
            matchRepository.save(match);
        }

        return toResponse(match, inviteCodeForCreator(match, userId));
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> listPlayers(UUID matchId) {
        findOrThrow(matchId);

        return matchPlayerRepository.findByMatchIdOrderByJoinedAtAsc(matchId)
                .stream()
                .map(player -> new PlayerResponse(
                        player.getUser().getId(),
                        player.getUser().getName(),
                        player.getJoinedAt()
                ))
                .toList();
    }

    public Match findOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException("Partida não encontrada", HttpStatus.NOT_FOUND));
    }

    private Match findForUpdateOrThrow(UUID matchId) {
        return matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new BusinessException("Partida não encontrada", HttpStatus.NOT_FOUND));
    }

    private String createInvite(Match match) {
        String code = inviteCodeGenerator.generate();
        inviteRepository.save(new Invite(match, code));
        return code;
    }

    private String inviteCodeForCreator(Match match, UUID requesterId) {
        if (!match.getCreator().getId().equals(requesterId)) {
            return null;
        }

        if (match.getEffectiveStatus() == MatchStatus.FINISHED) {
            return null;
        }

        return inviteRepository.findByMatchId(match.getId())
                .map(Invite::getCode)
                .orElse(null);
    }

    private MatchResponse toResponse(Match match, String inviteCode) {
        return new MatchResponse(
                match.getId(),
                match.getTitle(),
                match.getLocation(),
                match.getScheduledAt(),
                match.getMaxPlayers(),
                matchPlayerRepository.countByMatchId(match.getId()),
                match.getEffectiveStatus(),
                toUserResponse(match.getCreator()),
                inviteCode,
                match.getCreatedAt()
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    private void validateScheduledAt(LocalDateTime scheduledAt) {
        if (scheduledAt.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BusinessException("A partida deve ser agendada com pelo menos 1 hora de antecedência");
        }
    }

    private void validateJoin(Match match, UUID userId, int currentPlayers) {
        MatchStatus effectiveStatus = match.getEffectiveStatus();

        if (effectiveStatus == MatchStatus.CANCELLED) {
            throw new BusinessException("Partida cancelada.");
        }

        if (effectiveStatus == MatchStatus.FINISHED) {
            throw new BusinessException("Não é possível entrar em uma partida que já ocorreu.");
        }

        if (effectiveStatus == MatchStatus.FULL) {
            throw new BusinessException("Partida sem vagas disponíveis.");
        }

        if (matchPlayerRepository.existsByMatchIdAndUserId(match.getId(), userId)) {
            throw new BusinessException("Você já está confirmado nessa partida.");
        }

        if (currentPlayers >= match.getMaxPlayers()) {
            throw new BusinessException("Partida sem vagas disponíveis.");
        }
    }
}
