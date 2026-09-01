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

/**
 * Contiene la lógica de negocio de la mensajería entre usuarios (conversaciones y mensajes).
 * El modelo de chat es REST + polling: no hay WebSocket ni push en tiempo real, así que el
 * cliente envía mensajes con peticiones normales y refresca el estado consultando
 * periódicamente los endpoints de conversaciones y mensajes.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Envía un mensaje de {@code senderId} al destinatario indicado en {@code request},
     * dentro del contexto del listing referenciado.
     * Si es el primer mensaje entre ambos usuarios para ese listing, la conversación se
     * crea automáticamente (ver {@link #findOrCreateConversation}); si ya existe, el mensaje
     * se añade a ella. Tras guardar el mensaje se actualiza el resumen de "último mensaje"
     * de la conversación y se incrementa el contador de mensajes no leídos del destinatario.
     *
     * @param senderId identificador del usuario que envía el mensaje
     * @param request  datos del mensaje: listing de referencia, destinatario y texto
     * @return el mensaje recién creado
     */
    public MessageResponse sendMessage(String senderId, SendMessageRequest request) {
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

    /**
     * Obtiene todas las conversaciones en las que participa el usuario, ordenadas de la
     * más recientemente actualizada a la más antigua.
     *
     * @param userId identificador del usuario
     * @return listado de conversaciones del usuario
     */
    public List<ConversationResponse> getConversationsForUser(String userId) {
        return conversationRepository.findByParticipantsContainingOrderByUpdatedAtDesc(userId).stream()
                .map(c -> toConversationResponse(c, userId))
                .toList();
    }

    /**
     * Obtiene el detalle de una conversación, verificando que el solicitante sea uno
     * de sus participantes.
     *
     * @param id          identificador de la conversación
     * @param requesterId identificador del usuario que hace la petición
     * @return el detalle de la conversación
     * @throws ConversationNotFoundException           si no existe ninguna conversación con ese id
     * @throws UnauthorizedConversationAccessException si el solicitante no participa en la conversación
     */
    public ConversationResponse getConversation(String id, String requesterId) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException(id));

        if (!conversation.getParticipants().contains(requesterId)) {
            throw new UnauthorizedConversationAccessException();
        }

        return toConversationResponse(conversation, requesterId);
    }

    /**
     * Obtiene todos los mensajes de una conversación, ordenados del más antiguo al más
     * reciente, y de paso marca como leídos los mensajes pendientes del solicitante.
     * Este es el endpoint que el cliente sondea periódicamente para refrescar el chat,
     * ya que no existe notificación en tiempo real vía WebSocket.
     *
     * @param conversationId identificador de la conversación
     * @param requesterId    identificador del usuario que hace la petición
     * @return listado de mensajes de la conversación
     * @throws ConversationNotFoundException           si no existe ninguna conversación con ese id
     * @throws UnauthorizedConversationAccessException si el solicitante no participa en la conversación
     */
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

    /**
     * Busca una conversación existente entre {@code userA} y {@code userB} para el listing
     * indicado y, si no existe ninguna, la crea. Esta es la creación automática de conversación
     * que se dispara al enviar el primer mensaje entre dos usuarios sobre un mismo listing:
     * el cliente nunca crea conversaciones explícitamente, solo envía mensajes.
     *
     * @param listingId identificador del listing sobre el que trata la conversación
     * @param userA     uno de los participantes (normalmente quien envía el mensaje)
     * @param userB     el otro participante (normalmente el destinatario)
     * @return la conversación existente entre ambos usuarios para ese listing, o una nueva recién creada
     */
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

    /**
     * Marca como leídos, para {@code userId}, tanto el contador de no leídos de la
     * conversación como los mensajes individuales pendientes que no fueron enviados por él.
     * Si no había nada pendiente no se realiza ninguna escritura de más.
     *
     * @param conversation conversación a actualizar
     * @param userId       usuario para el que se marca como leído
     */
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

    /**
     * Convierte una {@link Conversation} en su DTO de respuesta, calculando el contador
     * de no leídos correspondiente al usuario que consulta.
     *
     * @param c      conversación a convertir
     * @param userId usuario para el que se calcula el contador de no leídos
     * @return el DTO de respuesta de la conversación
     */
    private ConversationResponse toConversationResponse(Conversation c, String userId) {
        int unread = c.getUnreadCount() != null ? c.getUnreadCount().getOrDefault(userId, 0) : 0;
        return new ConversationResponse(
                c.getId(), c.getListingId(), c.getParticipants(),
                c.getLastMessage() != null ? c.getLastMessage().text() : null,
                c.getLastMessage() != null ? c.getLastMessage().sentAt() : null,
                unread);
    }

    /**
     * Convierte un {@link Message} en su DTO de respuesta.
     *
     * @param m mensaje a convertir
     * @return el DTO de respuesta del mensaje
     */
    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderId(), m.getText(), m.getSentAt(),
                m.isRead());
    }
}
