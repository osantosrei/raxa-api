package com.raxa.domain.user;

import com.raxa.dto.request.LoginRequest;
import com.raxa.dto.request.RegisterRequest;
import com.raxa.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Cadastro e login")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cadastrar usuario", description = "Cria um usuario e retorna JWT para uso imediato.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @Operation(summary = "Autenticar usuario", description = "Valida credenciais e retorna um JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado"),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
