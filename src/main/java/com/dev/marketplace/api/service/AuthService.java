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

/**
 * Servicio que concentra toda la lógica de autenticación: alta de usuarios, login,
 * y emisión/renovación de tokens de sesión (access token JWT + refresh token opaco).
 * Los refresh tokens nunca se persisten en texto plano: solo se guarda su hash SHA-256,
 * y cada renovación aplica rotación (el token usado se revoca y se emite uno nuevo).
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final long REFRESH_TOKEN_DAYS = 30;

    /**
     * Registra un nuevo usuario: valida que el email no esté ya en uso, hashea la contraseña
     * antes de persistirla y emite un access token junto con un nuevo refresh token.
     *
     * @param request datos de registro (email, contraseña en texto plano, nombre a mostrar y teléfono)
     * @return access token, refresh token y resumen del usuario recién creado
     * @throws EmailAlreadyExistsException si ya existe un usuario registrado con ese email
     */
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

    /**
     * Autentica a un usuario existente comprobando su email y contraseña, y emite
     * un nuevo access token junto con un nuevo refresh token.
     *
     * @param request credenciales de acceso (email y contraseña en texto plano)
     * @return access token, refresh token y resumen del usuario autenticado
     * @throws InvalidCredentialsException si no existe un usuario con ese email o la contraseña no coincide
     */
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

    /**
     * Canjea un refresh token válido por una nueva pareja de tokens, aplicando rotación:
     * el refresh token recibido se marca como revocado y se genera uno nuevo para el usuario,
     * de forma que un mismo refresh token en texto plano nunca puede reutilizarse dos veces.
     *
     * @param rawRefreshToken refresh token en texto plano recibido del cliente
     * @return nuevo access token, nuevo refresh token y resumen del usuario
     * @throws InvalidRefreshTokenException si el token no existe, ya fue revocado, está expirado
     *         o el usuario asociado ya no existe
     */
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

    /**
     * Genera un nuevo refresh token opaco para el usuario indicado y persiste únicamente
     * su hash (nunca el token en texto plano), con expiración a {@value #REFRESH_TOKEN_DAYS}
     * días desde su creación.
     *
     * @param userId id del usuario propietario del refresh token
     * @return el refresh token en texto plano, que solo se devuelve al cliente una vez
     *         y nunca se guarda en base de datos
     */
    private String createRefreshToken(String userId) {
        String rawToken = generateSecureRandomToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS));

        refreshTokenRepository.save(refreshToken);

        return rawToken; // el token en texto plano solo se devuelve al cliente, nunca se guarda así
    }

    /**
     * Genera 64 bytes aleatorios criptográficamente seguros y los codifica en Base64 URL-safe
     * sin padding, para usarlos como refresh token opaco.
     *
     * @return refresh token en texto plano, listo para enviar al cliente
     */
    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Calcula el hash SHA-256 (codificado en Base64 URL-safe sin padding) de un token en texto
     * plano, para poder buscarlo y almacenarlo sin persistir nunca el valor original.
     *
     * @param rawToken token en texto plano a hashear
     * @return hash del token, en Base64 URL-safe sin padding
     * @throws IllegalStateException si el algoritmo SHA-256 no está disponible en la JVM
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error al hashear el token", e);
        }
    }

    /**
     * Convierte una entidad {@link User} en su resumen público {@link UserSummary}
     * (id, email, nombre a mostrar y avatar), usado en las respuestas de autenticación.
     *
     * @param u usuario a resumir
     * @return resumen del usuario
     */
    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getDisplayName(), u.getAvatarUrl());
    }
}
