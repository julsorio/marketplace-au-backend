package com.dev.marketplace.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Listing;

/**
 * Repositorio Spring Data MongoDB para el acceso CRUD básico a los listings (colección
 * {@code listings}). Las operaciones de búsqueda con filtros combinados no viven aquí, sino
 * en {@link ListingSearchRepository}.
 */
public interface ListingRepository extends MongoRepository<Listing, String> {

}
