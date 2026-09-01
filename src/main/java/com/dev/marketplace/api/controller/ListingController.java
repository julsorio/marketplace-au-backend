package com.dev.marketplace.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.request.dto.CreateListingRequest;
import com.dev.marketplace.api.request.dto.ListingSearchRequest;
import com.dev.marketplace.api.request.dto.UpdateListingRequest;
import com.dev.marketplace.api.request.dto.UpdateListingStatusRequest;
import com.dev.marketplace.api.response.dto.ListingResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.ListingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoint REST para el ciclo de vida de los listings (anuncios) del marketplace: creación,
 * consulta individual, búsqueda con filtros, actualización, cambio de estado y borrado.
 * Delega toda la lógica de negocio en {@link ListingService}; esta clase se limita a mapear
 * las peticiones HTTP y a extraer al usuario autenticado (si lo hay) del {@link UserPrincipal}.
 */
@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingController {
    private final ListingService listingService;

    /**
     * Crea un nuevo listing perteneciente al usuario autenticado.
     *
     * @param principal usuario autenticado que actuará como vendedor (sellerId) del listing
     * @param request   datos del listing a crear, ya validados por Bean Validation
     * @return el listing creado, con código HTTP 201 (Created)
     */
    @PostMapping
    public ResponseEntity<ListingResponse> create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(principal.getUserId(), request));
    }

    /**
     * Recupera un listing por su id.
     * El acceso es público: si no hay usuario autenticado se trata como visitante anónimo,
     * lo que afecta a si la ubicación devuelta es la exacta o una versión difuminada (ver
     * {@link ListingService#getById(String, String)}).
     *
     * @param principal usuario autenticado, o {@code null} si la petición es anónima
     * @param id        id del listing a consultar
     * @return el listing solicitado
     * @throws com.dev.marketplace.api.exceptions.ListingNotFoundException si no existe un listing con ese id
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        String viewerId = principal != null ? principal.getUserId() : null;
        return ResponseEntity.ok(listingService.getById(id, viewerId));
    }

    /**
     * Busca listings activos aplicando de forma combinada los filtros recibidos (categoría,
     * condición, rango de precio, estado/región, búsqueda geoespacial por radio y texto libre),
     * con paginación.
     *
     * @param principal usuario autenticado, o {@code null} si la petición es anónima
     * @param category  filtro opcional por categoría
     * @param condition filtro opcional por condición del producto
     * @param minPrice  precio mínimo opcional
     * @param maxPrice  precio máximo opcional
     * @param state     filtro opcional por estado/región australiana
     * @param latitude  latitud opcional del punto de referencia para búsqueda por radio
     * @param longitude longitud opcional del punto de referencia para búsqueda por radio
     * @param radiusKm  radio opcional en kilómetros para la búsqueda geoespacial
     * @param query     texto libre opcional a buscar en título/descripción
     * @param page      número de página (0-indexado), 0 por defecto
     * @param size      tamaño de página, 20 por defecto
     * @return la lista de listings que cumplen los filtros indicados
     */
    @GetMapping
    public ResponseEntity<List<ListingResponse>> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ListingSearchRequest request = new ListingSearchRequest(
                category, condition, minPrice, maxPrice, state,
                latitude, longitude, radiusKm, query, page, size);
        String viewerId = principal != null ? principal.getUserId() : null;
        return ResponseEntity.ok(listingService.search(request, viewerId));
    }

    /**
     * Elimina un listing. Solo puede hacerlo el vendedor propietario del anuncio.
     *
     * @param principal usuario autenticado que solicita el borrado
     * @param id        id del listing a eliminar
     * @return sin contenido (HTTP 204) si el borrado se realiza correctamente
     * @throws com.dev.marketplace.api.exceptions.ListingNotFoundException          si no existe un listing con ese id
     * @throws com.dev.marketplace.api.exceptions.UnauthorizedListingAccessException si el usuario autenticado no es el vendedor del listing
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        listingService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Actualiza los datos de un listing existente. Solo puede hacerlo el vendedor propietario.
     *
     * @param principal usuario autenticado que solicita la actualización
     * @param id        id del listing a actualizar
     * @param request   nuevos datos del listing, ya validados por Bean Validation
     * @return el listing actualizado
     * @throws com.dev.marketplace.api.exceptions.ListingNotFoundException          si no existe un listing con ese id
     * @throws com.dev.marketplace.api.exceptions.UnauthorizedListingAccessException si el usuario autenticado no es el vendedor del listing
     */
    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateListingRequest request) {
        return ResponseEntity.ok(listingService.update(id, principal.getUserId(), request));
    }

    /**
     * Cambia el estado de un listing (por ejemplo, a "sold" o "reserved"). Solo puede
     * hacerlo el vendedor propietario.
     *
     * @param principal usuario autenticado que solicita el cambio de estado
     * @param id        id del listing a modificar
     * @param request   nuevo estado, ya validado por Bean Validation
     * @return el listing con el estado actualizado
     * @throws com.dev.marketplace.api.exceptions.ListingNotFoundException          si no existe un listing con ese id
     * @throws com.dev.marketplace.api.exceptions.UnauthorizedListingAccessException si el usuario autenticado no es el vendedor del listing
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateListingStatusRequest request) {
        return ResponseEntity.ok(listingService.updateStatus(id, principal.getUserId(), request));
    }

}
