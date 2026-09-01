package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Message;

/**
 * Repositorio Spring Data MongoDB para el acceso a la colección de mensajes.
 */
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * Busca todos los mensajes de una conversación, ordenados del más antiguo al más reciente.
     *
     * @param conversationId identificador de la conversación
     * @return los mensajes de la conversación, ordenados por fecha de envío ascendente
     */
    List<Message> findByConversationIdOrderBySentAtAsc(String conversationId);

}
