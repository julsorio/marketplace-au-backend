package com.dev.marketplace.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Listing;

public interface ListingRepository extends MongoRepository<Listing, String> {

}
