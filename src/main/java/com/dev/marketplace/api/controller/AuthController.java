package com.dev.marketplace.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.request.dto.AuthResponse;
import com.dev.marketplace.api.request.dto.LoginRequest;
import com.dev.marketplace.api.request.dto.RefreshRequest;
import com.dev.marketplace.api.request.dto.RegisterRequest;
import com.dev.marketplace.api.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST que expone los endpoints públicos de autenticación del marketplace:
 * alta de usuario, login y renovación de tokens mediante refresh token.
 * Todas las respuestas exitosas incluyen un {@link AuthResponse} con el access token,
 * el refresh token y un resumen del usuario autenticado.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
private final AuthService authService;

    /**
     * Da de alta un nuevo usuario en el sistema.
     * Delega en {@link AuthService#register(RegisterRequest)} la validación de email
     * duplicado, el hashing de la contraseña y la emisión de los tokens de sesión.
     *
     * @param request datos de registro (email, contraseña, nombre a mostrar y teléfono opcional)
     * @return HTTP 201 (Created) con el access token, el refresh token y el resumen del usuario creado
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Autentica a un usuario ya existente mediante email y contraseña.
     *
     * @param request credenciales de acceso (email y contraseña)
     * @return HTTP 200 (OK) con un nuevo access token, un nuevo refresh token y el resumen del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Renueva la sesión a partir de un refresh token válido, aplicando rotación:
     * el refresh token recibido queda revocado y se emite uno nuevo junto con un nuevo access token.
     *
     * @param request cuerpo con el refresh token en texto plano a canjear
     * @return HTTP 200 (OK) con el nuevo access token, el nuevo refresh token y el resumen del usuario
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }
}
