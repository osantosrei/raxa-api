package com.raxa.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raxa.dto.request.LoginRequest;
import com.raxa.dto.request.RegisterRequest;
import com.raxa.exception.BusinessException;
import com.raxa.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesUserWithNormalizedEmailAndReturnsToken() {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest("João Silva", "JOAO@TEST.COM", "senha123");

        when(userRepository.existsByEmail("joao@test.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
        when(jwtService.generateToken(userId, "joao@test.com")).thenReturn("jwt-token");
        when(jwtService.expirationSeconds()).thenReturn(86400L);

        var response = userService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400L);
        assertThat(response.user().email()).isEqualTo("joao@test.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerWithDuplicateEmailThrowsConflict() {
        RegisterRequest request = new RegisterRequest("João Silva", "joao@test.com", "senha123");
        when(userRepository.existsByEmail("joao@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo("E-mail já cadastrado");
                });
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        User user = buildUser(UUID.randomUUID(), "joao@test.com", "hashed-password");

        when(userRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest("joao@test.com", "errada")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.getMessage()).isEqualTo("Credenciais inválidas");
                });
    }

    private User buildUser(UUID id, String email, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setName("João Silva");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        return user;
    }
}
