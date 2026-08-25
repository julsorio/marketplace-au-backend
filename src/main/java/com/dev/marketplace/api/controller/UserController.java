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

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();

        return ResponseEntity.ok(new UserSummary(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl()));
    }

    // Perfil público mínimo (sin email ni otros datos sensibles), para mostrar quién es el
    // otro participante de una conversación o el vendedor de un anuncio. Público a propósito
    // (ver SecurityConfig): igual que se puede ver el anuncio de alguien sin iniciar sesión,
    // se puede ver el nombre de quien lo publicó.
    @GetMapping("/{id}")
    public ResponseEntity<UserPublicResponse> getPublicProfile(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return ResponseEntity.ok(
                new UserPublicResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl(), user.getRating()));
    }
}
