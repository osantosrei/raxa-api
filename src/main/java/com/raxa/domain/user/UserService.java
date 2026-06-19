package com.raxa.domain.user;

import com.raxa.dto.request.LoginRequest;
import com.raxa.dto.request.RegisterRequest;
import com.raxa.dto.request.UpdateProfileRequest;
import com.raxa.dto.response.AuthResponse;
import com.raxa.dto.response.UserResponse;
import com.raxa.exception.BusinessException;
import com.raxa.security.JwtService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("E-mail já cadastrado", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());

        return buildAuthResponse(token, savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return buildAuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return toResponse(findById(userId));
    }

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.name() != null) {
            user.setName(request.name().trim());
        }

        return toResponse(userRepository.save(user));
    }

    private User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.expirationSeconds(),
                toResponse(user)
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("Credenciais inválidas", HttpStatus.UNAUTHORIZED);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
