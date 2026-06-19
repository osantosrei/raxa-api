package com.raxa.domain.user;

import static com.raxa.security.AuthUtils.currentUserId;

import com.raxa.dto.request.UpdateProfileRequest;
import com.raxa.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Perfil do usuario")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Consultar perfil", description = "Retorna os dados do usuario autenticado.")
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getProfile(currentUserId(authentication));
    }

    @Operation(summary = "Atualizar perfil", description = "Atualiza nome do usuario autenticado.")
    @PutMapping("/me")
    public UserResponse update(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(currentUserId(authentication), request);
    }
}
