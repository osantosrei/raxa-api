package com.raxa.domain.match;

import static com.raxa.security.AuthUtils.currentUserId;

import com.raxa.dto.request.CreateMatchRequest;
import com.raxa.dto.response.MatchResponse;
import com.raxa.dto.response.PlayerResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Matches", description = "Gerenciamento de partidas")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateMatchRequest request
    ) {
        return matchService.createMatch(currentUserId(authentication), request);
    }

    @GetMapping
    public List<MatchResponse> list(Authentication authentication) {
        return matchService.listByUser(currentUserId(authentication));
    }

    @GetMapping("/{id}")
    public MatchResponse get(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return matchService.getMatch(id, currentUserId(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        matchService.cancelMatch(id, currentUserId(authentication));
    }

    @PostMapping("/{id}/join")
    public MatchResponse join(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return matchService.joinMatch(id, currentUserId(authentication));
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        matchService.leaveMatch(id, currentUserId(authentication));
    }

    @GetMapping("/{id}/players")
    public List<PlayerResponse> players(@PathVariable UUID id) {
        return matchService.listPlayers(id);
    }
}
