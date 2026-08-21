package com.dev.marketplace.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.DuplicateKeyException;
import com.dev.marketplace.api.exceptions.ListingNotFoundException;
import com.dev.marketplace.api.model.Favorite;
import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.repository.FavoriteRepository;
import com.dev.marketplace.api.repository.ListingRepository;
import com.dev.marketplace.api.response.dto.FavoriteResponse;
import com.dev.marketplace.api.response.dto.ListingResponse;
import com.dev.marketplace.api.util.LocationFuzzer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;

    public void addFavorite(String userId, String listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ListingNotFoundException(listingId);
        }

        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            return; // ya es favorito, operación idempotente — no lanzamos error
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setListingId(listingId);

        try {
            favoriteRepository.save(favorite);
        } catch (DuplicateKeyException e) {
            // condición de carrera: otra petición lo insertó justo antes — no es un error
            // real
        }
    }

    public void removeFavorite(String userId, String listingId) {
        favoriteRepository.deleteByUserIdAndListingId(userId, listingId);
    }

    public List<FavoriteResponse> getFavorites(String userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        return favorites.stream()
                .map(fav -> {
                    Listing listing = listingRepository.findById(fav.getListingId()).orElse(null);
                    if (listing == null) {
                        return null; // el listing fue eliminado, pero el favorito sigue existiendo
                    }
                    return new FavoriteResponse(toListingResponse(listing, userId), fav.getCreatedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // userId es quien está consultando sus favoritos; casi siempre será distinto del
    // sellerId del listing favorito, así que en la práctica esto casi siempre difumina.
    // Se comprueba igual por si alguna vez alguien marca como favorito su propio anuncio.
    private ListingResponse toListingResponse(Listing l, String viewerId) {
        Double latitude = null;
        Double longitude = null;

        if (l.getLocation() != null) {
            boolean isOwner = viewerId != null && viewerId.equals(l.getSellerId());
            if (isOwner) {
                latitude = l.getLocation().getY();
                longitude = l.getLocation().getX();
            } else {
                double[] fuzzed = LocationFuzzer.fuzz(l.getLocation(), l.getId());
                latitude = fuzzed[0];
                longitude = fuzzed[1];
            }
        }

        return new ListingResponse(
                l.getId(), l.getSellerId(), l.getTitle(), l.getDescription(),
                l.getPrice().amount(), l.getPrice().currency(), l.getPrice().negotiable(),
                l.getCategory(), l.getSubcategory(), l.getCondition(), l.getImages(),
                l.getSuburb(), l.getState(), latitude, longitude, l.getStatus(), l.getViews(),
                l.getFavoritesCount(), l.getCreatedAt());
    }
}
