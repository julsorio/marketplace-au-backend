package com.dev.marketplace.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Conversation;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByParticipantsContainingOrderByUpdatedAtDesc(String userId);

    Optional<Conversation> findByListingIdAndParticipantsContaining(String listingId, String userId);

}
