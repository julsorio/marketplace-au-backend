package com.dev.marketplace.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.response.dto.FavoriteResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.FavoriteService;

import lombok.RequiredArgsConstructor;

/**
 * Expone los endpoints REST para que un usuario gestione sus listings favoritos:
 * añadirlos, quitarlos y consultar su lista de favoritos.
 */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    /**
     * Marca un listing como favorito del usuario autenticado.
     * La operación es idempotente: si el listing ya era favorito, no ocurre nada
     * y la respuesta sigue siendo satisfactoria.
     *
     * @param principal usuario autenticado
     * @param listingId identificador del listing a marcar como favorito
     * @return 204 (NO CONTENT) si la operación se completa correctamente
     */
    @PostMapping("/{listingId}")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String listingId) {
        favoriteService.addFavorite(principal.getUserId(), listingId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Quita un listing de los favoritos del usuario autenticado.
     *
     * @param principal usuario autenticado
     * @param listingId identificador del listing a quitar de favoritos
     * @return 204 (NO CONTENT) si la operación se completa correctamente
     */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String listingId) {
        favoriteService.removeFavorite(principal.getUserId(), listingId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene la lista de favoritos del usuario autenticado, con el detalle completo
     * de cada listing favorito (no solo su identificador).
     *
     * @param principal usuario autenticado
     * @return listado de favoritos del usuario
     */
    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteService.getFavorites(principal.getUserId()));
    }
}
