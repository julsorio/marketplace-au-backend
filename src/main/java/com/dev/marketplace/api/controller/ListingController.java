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

@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingController {
    private final ListingService listingService;

    @PostMapping
    public ResponseEntity<ListingResponse> create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(principal.getUserId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ListingResponse>> search(
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
        return ResponseEntity.ok(listingService.search(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        listingService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateListingRequest request) {
        return ResponseEntity.ok(listingService.update(id, principal.getUserId(), request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateListingStatusRequest request) {
        return ResponseEntity.ok(listingService.updateStatus(id, principal.getUserId(), request));
    }

}
