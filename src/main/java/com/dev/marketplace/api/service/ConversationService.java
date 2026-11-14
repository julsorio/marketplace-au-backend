package com.dev.marketplace.api.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.ConversationNotFoundException;
import com.dev.marketplace.api.exceptions.SelfMessagingNotAllowedException;
import com.dev.marketplace.api.exceptions.UnauthorizedConversationAccessException;
import com.dev.marketplace.api.model.Conversation;
import com.dev.marketplace.api.model.LastMessage;
import com.dev.marketplace.api.model.Message;
import com.dev.marketplace.api.repository.ConversationRepository;
import com.dev.marketplace.api.repository.MessageRepository;
import com.dev.marketplace.api.request.dto.SendMessageRequest;
import com.dev.marketplace.api.response.dto.ConversationResponse;
import com.dev.marketplace.api.response.dto.MessageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;

    public MessageResponse sendMessage(String senderId, SendMessageRequest request) {
        if (senderId.equals(request.recipientId())) {
            throw new SelfMessagingNotAllowedException();
        }

        Conversation conversation = findOrCreateConversation(
                request.listingId(), senderId, request.recipientId());

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setText(request.text());

        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessage(new LastMessage(request.text(), savedMessage.getSentAt(), senderId));
        conversation.setUpdatedAt(Instant.now());

        Map<String, Integer> unread = conversation.getUnreadCount();
        if (unread == null)
            unread = new HashMap<>();
        unread.merge(request.recipientId(), 1, Integer::sum);
        conversation.setUnreadCount(unread);

        conversationRepository.save(conversation);

        return toMessageResponse(savedMessage);
    }

    public List<ConversationResponse> getConversationsForUser(String userId) {
        return conversationRepository.findByParticipantsContainingOrderByUpdatedAtDesc(userId).stream()
                .map(c -> toConversationResponse(c, userId))
                .toList();
    }

    public ConversationResponse getConversation(String id, String requesterId) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException(id));

        if (!conversation.getParticipants().contains(requesterId)) {
            throw new UnauthorizedConversationAccessException();
        }

        return toConversationResponse(conversation, requesterId);
    }

    public List<MessageResponse> getMessages(String conversationId, String requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (!conversation.getParticipants().contains(requesterId)) {
            throw new UnauthorizedConversationAccessException();
        }

        markAsRead(conversation, requesterId);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private Conversation findOrCreateConversation(String listingId, String userA, String userB) {
        Query query = new Query(Criteria.where("listingId").is(listingId)
                .and("participants").all(userA, userB));

        Conversation existing = mongoTemplate.findOne(query, Conversation.class);
        if (existing != null) {
            return existing;
        }

        Conversation conversation = new Conversation();
        conversation.setListingId(listingId);
        conversation.setParticipants(List.of(userA, userB));
        conversation.setUnreadCount(new HashMap<>());

        return conversationRepository.save(conversation);
    }

    private void markAsRead(Conversation conversation, String userId) {
        Map<String, Integer> unread = conversation.getUnreadCount();
        if (unread != null && unread.getOrDefault(userId, 0) > 0) {
            unread.put(userId, 0);
            conversationRepository.save(conversation);
        }

        messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId()).stream()
                .filter(m -> !m.getSenderId().equals(userId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    private ConversationResponse toConversationResponse(Conversation c, String userId) {
        int unread = c.getUnreadCount() != null ? c.getUnreadCount().getOrDefault(userId, 0) : 0;
        return new ConversationResponse(
                c.getId(), c.getListingId(), c.getParticipants(),
                c.getLastMessage() != null ? c.getLastMessage().text() : null,
                c.getLastMessage() != null ? c.getLastMessage().sentAt() : null,
                unread);
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderId(), m.getText(), m.getSentAt(),
                m.isRead());
    }
}
