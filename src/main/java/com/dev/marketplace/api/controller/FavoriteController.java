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

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{listingId}")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String listingId) {
        favoriteService.addFavorite(principal.getUserId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String listingId) {
        favoriteService.removeFavorite(principal.getUserId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteService.getFavorites(principal.getUserId()));
    }
}
