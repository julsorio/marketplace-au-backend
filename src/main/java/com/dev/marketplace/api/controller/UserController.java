package com.dev.marketplace.api.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.exceptions.UserNotFoundException;
import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.UserRepository;
import com.dev.marketplace.api.request.dto.UserSummary;
import com.dev.marketplace.api.response.dto.UserPublicResponse;
import com.dev.marketplace.api.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Controller REST con los endpoints relativos al usuario: el perfil propio del usuario
 * autenticado y el perfil público de cualquier usuario dado su id.
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    /**
     * Devuelve el resumen del usuario actualmente autenticado, incluyendo su email.
     * Los datos se toman directamente del {@link UserPrincipal} inyectado por Spring Security,
     * sin necesidad de volver a consultar la base de datos.
     *
     * @param principal usuario autenticado, resuelto a partir del token de la petición
     * @return HTTP 200 (OK) con id, email, nombre a mostrar y avatar del usuario
     */
    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();

        return ResponseEntity.ok(new UserSummary(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl()));
    }

    /**
     * Devuelve el perfil público mínimo (sin email ni otros datos sensibles) de un usuario,
     * para mostrar quién es el otro participante de una conversación o el vendedor de un anuncio.
     * Es un endpoint público a propósito (ver SecurityConfig): igual que se puede ver el anuncio
     * de alguien sin iniciar sesión, se puede ver el nombre de quien lo publicó.
     *
     * @param id id del usuario cuyo perfil público se quiere consultar
     * @return HTTP 200 (OK) con id, nombre a mostrar, avatar y rating agregado del usuario
     * @throws UserNotFoundException si no existe ningún usuario con ese id
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserPublicResponse> getPublicProfile(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return ResponseEntity.ok(
                new UserPublicResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl(), user.getRating()));
    }
}
