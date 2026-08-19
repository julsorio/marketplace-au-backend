package com.dev.marketplace.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.ListingNotFoundException;
import com.dev.marketplace.api.exceptions.UnauthorizedListingAccessException;
import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.model.Price;
import com.dev.marketplace.api.repository.LIstingSearchRepository;
import com.dev.marketplace.api.repository.ListingRepository;
import com.dev.marketplace.api.request.dto.CreateListingRequest;
import com.dev.marketplace.api.request.dto.ListingSearchRequest;
import com.dev.marketplace.api.request.dto.UpdateListingRequest;
import com.dev.marketplace.api.request.dto.UpdateListingStatusRequest;
import com.dev.marketplace.api.response.dto.ListingResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;
    private final LIstingSearchRepository listingSearchRepository;

    private static final long LISTING_EXPIRATION_DAYS = 30;

    public ListingResponse create(String sellerId, CreateListingRequest request) {
        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPrice(new Price(request.price(), "AUD", request.negotiable()));
        listing.setCategory(request.category());
        listing.setSubcategory(request.subcategory());
        listing.setCondition(request.condition());
        listing.setAttributes(request.attributes());
        listing.setImages(request.images());
        listing.setLocation(new GeoJsonPoint(request.longitude(), request.latitude()));
        listing.setSuburb(request.suburb());
        listing.setState(request.state());
        listing.setExpiresAt(Instant.now().plus(LISTING_EXPIRATION_DAYS, ChronoUnit.DAYS));

        Listing saved = listingRepository.save(listing);
        return toResponse(saved);
    }

    public ListingResponse getById(String id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        listing.setViews(listing.getViews() + 1);
        listingRepository.save(listing);

        return toResponse(listing);
    }

    public List<ListingResponse> search(ListingSearchRequest request) {
        return listingSearchRepository.search(request).stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(String id, String requesterId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (!listing.getSellerId().equals(requesterId)) {
            throw new UnauthorizedListingAccessException();
        }

        listingRepository.deleteById(id);
    }

    private ListingResponse toResponse(Listing l) {
        return new ListingResponse(
                l.getId(), l.getSellerId(), l.getTitle(), l.getDescription(),
                l.getPrice().amount(), l.getPrice().currency(), l.getPrice().negotiable(),
                l.getCategory(), l.getSubcategory(), l.getCondition(), l.getImages(),
                l.getSuburb(), l.getState(), l.getStatus(), l.getViews(), l.getFavoritesCount(),
                l.getCreatedAt());
    }

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
        listing.setAttributes(request.attributes());
        listing.setImages(request.images());
        listing.setUpdatedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        return toResponse(saved);
    }

    public ListingResponse updateStatus(String id, String requesterId, UpdateListingStatusRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (!listing.getSellerId().equals(requesterId)) {
            throw new UnauthorizedListingAccessException();
        }

        listing.setStatus(request.status());
        listing.setUpdatedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        return toResponse(saved);
    }
}
