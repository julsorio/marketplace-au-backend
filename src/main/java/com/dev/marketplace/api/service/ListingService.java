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

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;
    private final ListingSearchRepository listingSearchRepository;
    private final CategoryService categoryService;

    private static final long LISTING_EXPIRATION_DAYS = 30;

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
        listing.setAttributes(request.attributes());
        listing.setImages(request.images());
        listing.setLocation(new GeoJsonPoint(request.longitude(), request.latitude()));
        listing.setSuburb(request.suburb());
        listing.setState(request.state());
        listing.setExpiresAt(Instant.now().plus(LISTING_EXPIRATION_DAYS, ChronoUnit.DAYS));

        Listing saved = listingRepository.save(listing);
        return toResponse(saved, sellerId);
    }

    public ListingResponse getById(String id, String viewerId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        listing.setViews(listing.getViews() + 1);
        listingRepository.save(listing);

        return toResponse(listing, viewerId);
    }

    public List<ListingResponse> search(ListingSearchRequest request, String viewerId) {
        return listingSearchRepository.search(request).stream()
                .map(l -> toResponse(l, viewerId))
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

    // viewerId es el id del usuario autenticado que está viendo el listing (null si es
    // anónimo). Solo el dueño ve la ubicación exacta; para cualquier otro se difumina.
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
                l.getCategory(), l.getSubcategory(), l.getCondition(), l.getImages(),
                l.getSuburb(), l.getState(), latitude, longitude, l.getStatus(), l.getViews(),
                l.getFavoritesCount(), l.getCreatedAt());
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
        return toResponse(saved, requesterId);
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
        return toResponse(saved, requesterId);
    }
}
