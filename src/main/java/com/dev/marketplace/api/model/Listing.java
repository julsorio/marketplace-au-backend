package com.dev.marketplace.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Entidad MongoDB que representa un anuncio (listing) publicado en el marketplace.
 * Se persiste en la colección {@code listings}.
 */
@Data
@Document(collection = "listings")
public class Listing {
    @Id
    private String id;

    /** Id del usuario propietario/vendedor del listing. */
    private String sellerId;

    private String title;
    private String description;

    private Price price;

    /** Id (slug) de la categoría raíz a la que pertenece el listing. Indexado para filtrar búsquedas por categoría. */
    @Indexed
    private String category;

    /** Id (slug) de la subcategoría, hija directa de {@link #category}. */
    private String subcategory;

    /** Condición del producto. Valores esperados: {@code new}, {@code like_new}, {@code good}, {@code fair}. */
    private String condition;

    /** Método de entrega. Valores esperados: {@code shipping}, {@code in_person}, {@code both}. Por defecto {@code in_person}. */
    private String deliveryMethod = "in_person";

    private Map<String, Object> attributes;

    private List<String> images;

    /**
     * Ubicación geográfica exacta del listing (longitud, latitud) tal y como la introdujo el
     * vendedor. Se indexa con un índice geoespacial 2dsphere para soportar búsquedas por
     * radio en {@link com.dev.marketplace.api.repository.ListingSearchRepository}.
     * No se expone directamente en las respuestas de la API a quien no es el dueño: se
     * difumina mediante {@link com.dev.marketplace.api.util.LocationFuzzer}.
     */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String suburb;

    /** Estado o región australiana del listing (ej. NSW, VIC). Indexado para filtrar búsquedas. */
    @Indexed
    private String state;

    /**
     * Estado del ciclo de vida del listing. Valores esperados: {@code active}, {@code sold},
     * {@code reserved}, {@code expired}, {@code draft}. Por defecto {@code active}. Indexado
     * porque las búsquedas siempre filtran por listings activos.
     */
    @Indexed
    private String status = "active";

    /** Número de veces que se ha consultado el listing (ver {@link com.dev.marketplace.api.service.ListingService#getById}). */
    private int views = 0;
    private int favoritesCount = 0;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    /** Momento en el que el listing expira automáticamente (30 días desde su creación). */
    private Instant expiresAt;
}
