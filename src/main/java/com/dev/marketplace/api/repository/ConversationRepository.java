package com.dev.marketplace.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Conversation;

/**
 * Repositorio Spring Data MongoDB para el acceso a la colección de conversaciones.
 */
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /**
     * Busca todas las conversaciones en las que participa el usuario indicado, ordenadas
     * de la más recientemente actualizada a la más antigua.
     *
     * @param userId identificador del usuario que debe estar en la lista de participantes
     * @return las conversaciones del usuario, ordenadas por fecha de actualización descendente
     */
    List<Conversation> findByParticipantsContainingOrderByUpdatedAtDesc(String userId);

    /**
     * Busca la conversación de un listing concreto en la que participa el usuario indicado.
     *
     * @param listingId identificador del listing
     * @param userId    identificador de uno de los participantes de la conversación
     * @return la conversación encontrada, o vacío si no existe ninguna que cumpla ambas condiciones
     */
    Optional<Conversation> findByListingIdAndParticipantsContaining(String listingId, String userId);

}
