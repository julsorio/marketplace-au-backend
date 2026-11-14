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

@Repository
@RequiredArgsConstructor
public class ListingSearchRepository {
    private final MongoTemplate mongoTemplate;

    private static final double EARTH_RADIUS_KM = 6378.1;

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
