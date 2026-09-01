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

/**
 * Contiene la lógica de negocio de los listings favoritos de un usuario:
 * añadirlos, quitarlos y consultarlos con el detalle completo del listing asociado.
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;

    /**
     * Marca un listing como favorito del usuario.
     * La operación es idempotente: si el listing ya era favorito, el método no hace nada
     * y no se lanza ningún error. Además, si dos peticiones concurrentes intentan añadir
     * el mismo favorito por primera vez a la vez, el índice único de la colección puede
     * rechazar la segunda inserción; esa {@link DuplicateKeyException} se captura y se
     * ignora, porque el resultado final (el favorito existe) es el deseado.
     *
     * @param userId    identificador del usuario que añade el favorito
     * @param listingId identificador del listing a marcar como favorito
     * @throws ListingNotFoundException si el listing indicado no existe
     */
    public void addFavorite(String userId, String listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ListingNotFoundException(listingId);
        }

        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            return;
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

    /**
     * Quita un listing de los favoritos del usuario. Si no estaba marcado como favorito,
     * no ocurre nada.
     *
     * @param userId    identificador del usuario
     * @param listingId identificador del listing a quitar de favoritos
     */
    public void removeFavorite(String userId, String listingId) {
        favoriteRepository.deleteByUserIdAndListingId(userId, listingId);
    }

    /**
     * Obtiene la lista de favoritos del usuario con el detalle completo de cada listing
     * (no solo sus identificadores), para que el cliente no tenga que hacer una consulta
     * adicional por cada favorito. Los favoritos cuyo listing ya no existe (fue eliminado)
     * se omiten del resultado, aunque el registro de favorito en sí siga existiendo.
     *
     * @param userId identificador del usuario
     * @return listado de favoritos del usuario, con el detalle de cada listing
     */
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

    /**
     * Convierte un {@link Listing} en su DTO de respuesta, difuminando su ubicación salvo
     * que el que consulta sea el propio vendedor del listing.
     * El parámetro {@code viewerId} es quien está consultando sus favoritos; casi siempre
     * será distinto del vendedor del listing favorito, así que en la práctica esto casi
     * siempre difumina la ubicación. Se comprueba igual por si alguna vez alguien marca
     * como favorito su propio anuncio.
     *
     * @param l        listing a convertir
     * @param viewerId identificador del usuario que consulta sus favoritos
     * @return el DTO de respuesta del listing, con la ubicación difuminada si corresponde
     */
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
                l.getCategory(), l.getSubcategory(), l.getCondition(), l.getDeliveryMethod(), l.getImages(),
                l.getSuburb(), l.getState(), latitude, longitude, l.getStatus(), l.getViews(),
                l.getFavoritesCount(), l.getCreatedAt());
    }
}
