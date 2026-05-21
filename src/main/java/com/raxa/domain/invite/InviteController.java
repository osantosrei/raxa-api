package com.raxa.domain.invite;

import static com.raxa.security.AuthUtils.currentUserId;

import com.raxa.dto.response.InvitePreviewResponse;
import com.raxa.dto.response.MatchResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
@Tag(name = "Invites", description = "Sistema de convites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/{code}/resolve")
    public InvitePreviewResponse resolve(@PathVariable String code) {
        return inviteService.resolveInvite(code);
    }

    @PostMapping("/{code}/join")
    public MatchResponse join(
            Authentication authentication,
            @PathVariable String code
    ) {
        return inviteService.joinViaInvite(code, currentUserId(authentication));
    }
}
