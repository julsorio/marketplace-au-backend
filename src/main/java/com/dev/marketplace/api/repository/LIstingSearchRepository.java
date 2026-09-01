package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.request.dto.ListingSearchRequest;

import lombok.RequiredArgsConstructor;

/**
 * Repositorio de búsqueda de listings construido directamente sobre {@link MongoTemplate},
 * en lugar de una consulta derivada de Spring Data, porque combina de forma dinámica un
 * número variable de filtros (categoría, condición, precio, estado/región, texto libre y
 * búsqueda geoespacial por radio) según cuáles vengan informados en la petición.
 */
@Repository
@RequiredArgsConstructor
public class ListingSearchRepository {
    private final MongoTemplate mongoTemplate;

    private static final double EARTH_RADIUS_KM = 6378.1;

    /**
     * Ejecuta una búsqueda de listings activos combinando (con AND) todos los filtros
     * presentes en {@code request}. Cada filtro solo se añade a la consulta si su valor
     * correspondiente viene informado; los filtros ausentes simplemente no restringen el
     * resultado. Filtros soportados:
     * <ul>
     *   <li>{@code status = "active"}: siempre se aplica, sin importar el resto de filtros.</li>
     *   <li>{@code category}: coincidencia exacta con la categoría del listing.</li>
     *   <li>{@code condition}: coincidencia exacta con la condición del listing.</li>
     *   <li>{@code state}: coincidencia exacta con el estado/región del listing.</li>
     *   <li>{@code minPrice}/{@code maxPrice}: rango sobre {@code price.amount} (uno de los
     *   dos, o ambos, pueden venir informados).</li>
     *   <li>{@code query}: búsqueda de texto libre e insensible a mayúsculas, por regex OR
     *   sobre {@code title} y {@code description}.</li>
     *   <li>{@code latitude}/{@code longitude}/{@code radiusKm}: búsqueda geoespacial dentro
     *   de un radio (en kilómetros, convertido a radianes) alrededor del punto indicado,
     *   sobre el campo {@code location} (índice 2dsphere). Solo se aplica si los tres valores
     *   vienen informados a la vez. Este filtro se combina (AND) con el resto, incluido el
     *   filtro de {@code state} ya aplicado arriba si también viene informado.</li>
     * </ul>
     * El resultado se pagina según {@code page}/{@code size} de la petición (página 0 y
     * tamaño 20 si no vienen informados o son inválidos) y se ordena por fecha de creación
     * descendente (más recientes primero).
     *
     * @param request criterios de búsqueda y paginación
     * @return la página de listings activos que cumplen todos los filtros indicados
     */
    public List<Listing> search(ListingSearchRequest request) {
        Query query = new Query();
        List<Criteria> criteriaList = new java.util.ArrayList<>();

        criteriaList.add(Criteria.where("status").is("active"));

        if (request.category() != null && !request.category().isBlank()) {
            criteriaList.add(Criteria.where("category").is(request.category()));
        }

        if (request.condition() != null && !request.condition().isBlank()) {
            criteriaList.add(Criteria.where("condition").is(request.condition()));
        }

        if (request.state() != null && !request.state().isBlank()) {
            criteriaList.add(Criteria.where("state").is(request.state()));
        }

        if (request.minPrice() != null || request.maxPrice() != null) {
            Criteria priceCriteria = Criteria.where("price.amount");
            if (request.minPrice() != null)
                priceCriteria = priceCriteria.gte(request.minPrice());
            if (request.maxPrice() != null)
                priceCriteria = priceCriteria.lte(request.maxPrice());
            criteriaList.add(priceCriteria);
        }

        if (request.query() != null && !request.query().isBlank()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(request.query(), "i"),
                    Criteria.where("description").regex(request.query(), "i")));
        }

        // radio de distancia, combinado con el filtro de state ya aplicado arriba
        if (request.latitude() != null && request.longitude() != null && request.radiusKm() != null) {
            double radiusInRadians = request.radiusKm() / EARTH_RADIUS_KM;
            criteriaList.add(Criteria.where("location").withinSphere(
                    new org.springframework.data.geo.Circle(
                            new org.springframework.data.geo.Point(request.longitude(), request.latitude()),
                            radiusInRadians)));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));

        int page = Math.max(request.page(), 0);
        int size = request.size() > 0 ? request.size() : 20;
        query.with(PageRequest.of(page, size));
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        return mongoTemplate.find(query, Listing.class);
    }
}
