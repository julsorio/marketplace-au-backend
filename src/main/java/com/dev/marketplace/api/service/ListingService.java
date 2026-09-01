package com.dev.marketplace.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.CategoryNotFoundException;
import com.dev.marketplace.api.exceptions.ListingNotFoundException;
import com.dev.marketplace.api.exceptions.UnauthorizedListingAccessException;
import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.model.Price;
import com.dev.marketplace.api.repository.ListingSearchRepository;
import com.dev.marketplace.api.repository.ListingRepository;
import com.dev.marketplace.api.request.dto.CreateListingRequest;
import com.dev.marketplace.api.request.dto.ListingSearchRequest;
import com.dev.marketplace.api.request.dto.UpdateListingRequest;
import com.dev.marketplace.api.request.dto.UpdateListingStatusRequest;
import com.dev.marketplace.api.response.dto.ListingResponse;
import com.dev.marketplace.api.util.LocationFuzzer;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de negocio para los listings (anuncios) del marketplace: creación, consulta,
 * búsqueda, actualización, cambio de estado y borrado.
 *
 * Dos aspectos transversales a tener en cuenta al leer esta clase:
 * <ul>
 *   <li><b>Difuminado de ubicación:</b> la ubicación exacta de un listing solo se expone en
 *   las respuestas al propio vendedor. Para cualquier otro visitante (incluido el anónimo)
 *   se sustituye por un punto desplazado aleatoriamente mediante {@link LocationFuzzer}, ver
 *   {@link #toResponse(Listing, String)}.</li>
 *   <li><b>Autorización por sellerId:</b> las operaciones de escritura sobre un listing ya
 *   existente ({@link #delete}, {@link #update}, {@link #updateStatus}) comprueban que el
 *   {@code requesterId} coincida con el {@code sellerId} guardado en el listing, lanzando
 *   {@link UnauthorizedListingAccessException} en caso contrario.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;
    private final ListingSearchRepository listingSearchRepository;
    private final CategoryService categoryService;

    private static final long LISTING_EXPIRATION_DAYS = 30;

    /**
     * Crea un nuevo listing para el vendedor indicado.
     * Antes de crear el listing valida que la categoría exista; si no, no se persiste nada.
     * La fecha de expiración se calcula automáticamente a {@value #LISTING_EXPIRATION_DAYS}
     * días desde el momento de la creación.
     *
     * @param sellerId id del usuario que publica el listing
     * @param request  datos del nuevo listing
     * @return el listing creado, ya convertido a {@link ListingResponse} desde el punto de vista del propio vendedor
     * @throws CategoryNotFoundException si la categoría indicada en la petición no existe
     */
    public ListingResponse create(String sellerId, CreateListingRequest request) {

        if(!categoryService.categoryExists(request.category())) {
            throw new CategoryNotFoundException(request.category());
        }

        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPrice(new Price(request.price(), "AUD", request.negotiable()));
        listing.setCategory(request.category());
        listing.setSubcategory(request.subcategory());
        listing.setCondition(request.condition());
        listing.setDeliveryMethod(request.deliveryMethod());
        listing.setAttributes(request.attributes());
        listing.setImages(request.images());
        listing.setLocation(new GeoJsonPoint(request.longitude(), request.latitude()));
        listing.setSuburb(request.suburb());
        listing.setState(request.state());
        listing.setExpiresAt(Instant.now().plus(LISTING_EXPIRATION_DAYS, ChronoUnit.DAYS));

        Listing saved = listingRepository.save(listing);
        return toResponse(saved, sellerId);
    }

    /**
     * Recupera un listing por su id y registra una visita.
     * Cada llamada a este método incrementa el contador de visitas del listing en 1 y
     * persiste el cambio, con independencia de quién sea el visitante (incluido el propio
     * dueño viendo su propio anuncio); no hay deduplicación por usuario ni por sesión.
     *
     * @param id       id del listing a consultar
     * @param viewerId id del usuario autenticado que realiza la consulta, o {@code null} si es anónimo
     * @return el listing solicitado, con la ubicación exacta si {@code viewerId} es el dueño, o difuminada en caso contrario
     * @throws ListingNotFoundException si no existe un listing con ese id
     */
    public ListingResponse getById(String id, String viewerId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        listing.setViews(listing.getViews() + 1);
        listingRepository.save(listing);

        return toResponse(listing, viewerId);
    }

    /**
     * Busca listings activos combinando los filtros de {@code request} (categoría, condición,
     * rango de precio, estado/región, radio geoespacial y texto libre), delegando la
     * construcción de la consulta a {@link ListingSearchRepository#search(ListingSearchRequest)}.
     *
     * @param request  criterios de búsqueda y paginación
     * @param viewerId id del usuario autenticado que realiza la búsqueda, o {@code null} si es anónimo
     * @return los listings que cumplen los filtros, con la ubicación difuminada salvo para los que pertenezcan al propio {@code viewerId}
     */
    public List<ListingResponse> search(ListingSearchRequest request, String viewerId) {
        return listingSearchRepository.search(request).stream()
                .map(l -> toResponse(l, viewerId))
                .toList();
    }

    /**
     * Elimina un listing. Solo el vendedor propietario puede borrarlo.
     *
     * @param id          id del listing a eliminar
     * @param requesterId id del usuario que solicita el borrado
     * @throws ListingNotFoundException           si no existe un listing con ese id
     * @throws UnauthorizedListingAccessException si {@code requesterId} no coincide con el sellerId del listing
     */
    public void delete(String id, String requesterId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (!listing.getSellerId().equals(requesterId)) {
            throw new UnauthorizedListingAccessException();
        }

        listingRepository.deleteById(id);
    }

    /**
     * Convierte una entidad {@link Listing} en su {@link ListingResponse}, aplicando la
     * política de privacidad de ubicación: {@code viewerId} es el id del usuario autenticado
     * que está viendo el listing (o {@code null} si es anónimo). Solo el dueño ve la ubicación
     * exacta guardada en el listing; para cualquier otro visitante la ubicación se difumina
     * mediante {@link LocationFuzzer#fuzz(GeoJsonPoint, String)}, que aplica un desplazamiento
     * aleatorio pero determinista (mismo listing → mismo punto difuminado en cada petición).
     *
     * @param l        listing a convertir
     * @param viewerId id del usuario que verá la respuesta, o {@code null} si es anónimo
     * @return el DTO de respuesta con la ubicación exacta o difuminada según corresponda
     */
    private ListingResponse toResponse(Listing l, String viewerId) {
        Double latitude = null;
        Double longitude = null;

        if (l.getLocation() != null) {
            boolean isOwner = viewerId != null && viewerId.equals(l.getSellerId());
            if (isOwner) {
                // GeoJsonPoint guarda las coordenadas como (x, y) = (longitude, latitude)
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

    /**
     * Actualiza los datos editables de un listing existente (título, descripción, precio,
     * categoría, condición, método de entrega, atributos e imágenes). No modifica la
     * ubicación ni el estado del listing. Solo el vendedor propietario puede actualizarlo.
     *
     * @param id          id del listing a actualizar
     * @param requesterId id del usuario que solicita la actualización
     * @param request     nuevos datos del listing
     * @return el listing actualizado
     * @throws ListingNotFoundException           si no existe un listing con ese id
     * @throws UnauthorizedListingAccessException si {@code requesterId} no coincide con el sellerId del listing
     */
    public ListingResponse update(String id, String requesterId, UpdateListingRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (!listing.getSellerId().equals(requesterId)) {
            throw new UnauthorizedListingAccessException();
        }

        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPrice(new Price(request.price(), listing.getPrice().currency(), request.negotiable()));
        listing.setCategory(request.category());
        listing.setSubcategory(request.subcategory());
        listing.setCondition(request.condition());
        listing.setDeliveryMethod(request.deliveryMethod());
        listing.setAttributes(request.attributes());
        listing.setImages(request.images());
        listing.setUpdatedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        return toResponse(saved, requesterId);
    }

    /**
     * Cambia el estado de un listing (por ejemplo a "sold", "reserved", etc., según los
     * valores validados en {@link UpdateListingStatusRequest}). Solo el vendedor propietario
     * puede cambiarlo.
     *
     * @param id          id del listing a modificar
     * @param requesterId id del usuario que solicita el cambio de estado
     * @param request     nuevo estado del listing
     * @return el listing con el estado actualizado
     * @throws ListingNotFoundException           si no existe un listing con ese id
     * @throws UnauthorizedListingAccessException si {@code requesterId} no coincide con el sellerId del listing
     */
    public ListingResponse updateStatus(String id, String requesterId, UpdateListingStatusRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (!listing.getSellerId().equals(requesterId)) {
            throw new UnauthorizedListingAccessException();
        }

        listing.setStatus(request.status());
        listing.setUpdatedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        return toResponse(saved, requesterId);
    }
}
