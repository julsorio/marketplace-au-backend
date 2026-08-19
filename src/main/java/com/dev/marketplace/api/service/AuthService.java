package com.dev.marketplace.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.EmailAlreadyExistsException;
import com.dev.marketplace.api.exceptions.InvalidCredentialsException;
import com.dev.marketplace.api.exceptions.InvalidRefreshTokenException;
import com.dev.marketplace.api.model.RefreshToken;
import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.RefreshTokenRepository;
import com.dev.marketplace.api.repository.UserRepository;
import com.dev.marketplace.api.request.dto.AuthResponse;
import com.dev.marketplace.api.request.dto.LoginRequest;
import com.dev.marketplace.api.request.dto.RegisterRequest;
import com.dev.marketplace.api.request.dto.UserSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final long REFRESH_TOKEN_DAYS = 30;

    public AuthResponse register(RegisterRequest request) throws EmailAlreadyExistsException {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setPhone(request.phone());

        User saved = userRepository.save(user);
        String accessToken = jwtService.generateToken(saved);
        String refreshToken = createRefreshToken(saved.getId());

        return new AuthResponse(accessToken, refreshToken, toSummary(saved));
    }

    public AuthResponse login(LoginRequest request) throws InvalidCredentialsException {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, toSummary(user));
    }

    public AuthResponse refresh(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(stored.getUserId())
            .orElseThrow(InvalidRefreshTokenException::new);

        // rotación: revocamos el token usado y generamos uno nuevo
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user.getId());

        return new AuthResponse(newAccessToken, newRefreshToken, toSummary(user));
    }

    private String createRefreshToken(String userId) {
        String rawToken = generateSecureRandomToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS));

        refreshTokenRepository.save(refreshToken);

        return rawToken; // el token en texto plano solo se devuelve al cliente, nunca se guarda así
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error al hashear el token", e);
        }
    }

    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getDisplayName(), u.getAvatarUrl());
    }
}
