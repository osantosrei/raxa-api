package com.raxa.domain.match;

import static com.raxa.security.AuthUtils.currentUserId;

import com.raxa.dto.request.CreateMatchRequest;
import com.raxa.dto.response.MatchResponse;
import com.raxa.dto.response.PlayerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @Operation(summary = "Criar partida", description = "Cria uma partida, adiciona o criador como participante e gera convite.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateMatchRequest request
    ) {
        return matchService.createMatch(currentUserId(authentication), request);
    }

    @Operation(summary = "Listar minhas partidas", description = "Lista partidas criadas ou participadas pelo usuario autenticado.")
    @GetMapping
    public List<MatchResponse> list(Authentication authentication) {
        return matchService.listByUser(currentUserId(authentication));
    }

    @Operation(summary = "Detalhar partida", description = "Retorna dados da partida. O inviteCode aparece apenas para o criador.")
    @GetMapping("/{id}")
    public MatchResponse get(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return matchService.getMatch(id, currentUserId(authentication));
    }

    @Operation(summary = "Cancelar partida", description = "Cancela uma partida criada pelo usuario autenticado.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        matchService.cancelMatch(id, currentUserId(authentication));
    }

    @Operation(summary = "Entrar na partida", description = "Confirma presenca respeitando status, horario e limite de vagas.")
    @PostMapping("/{id}/join")
    public MatchResponse join(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return matchService.joinMatch(id, currentUserId(authentication));
    }

    @Operation(summary = "Sair da partida", description = "Remove a confirmacao do usuario autenticado.")
    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        matchService.leaveMatch(id, currentUserId(authentication));
    }

    @Operation(summary = "Listar participantes", description = "Lista jogadores confirmados em ordem de entrada.")
    @GetMapping("/{id}/players")
    public List<PlayerResponse> players(@PathVariable UUID id) {
        return matchService.listPlayers(id);
    }
}
