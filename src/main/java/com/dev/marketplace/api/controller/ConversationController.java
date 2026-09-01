package com.dev.marketplace.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.request.dto.SendMessageRequest;
import com.dev.marketplace.api.response.dto.ConversationResponse;
import com.dev.marketplace.api.response.dto.MessageResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.ConversationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Expone los endpoints REST de mensajería entre usuarios (conversaciones y mensajes).
 * El chat funciona mediante peticiones REST convencionales combinadas con polling por
 * parte del cliente (no se usa WebSocket): el cliente envía mensajes con
 * {@code POST /messages} y refresca el estado de la conversación consultando
 * periódicamente {@code GET /conversations} y {@code GET /conversations/{id}/messages}.
 */
@RestController
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    /**
     * Envía un mensaje del usuario autenticado a otro usuario dentro del contexto de un listing.
     * Si no existe todavía una conversación entre ambos usuarios para ese listing, se crea
     * automáticamente antes de guardar el mensaje.
     *
     * @param principal usuario autenticado que envía el mensaje
     * @param request   datos del mensaje: listing de referencia, destinatario y texto
     * @return el mensaje creado, con código de estado 201 (CREATED)
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.sendMessage(principal.getUserId(), request));
    }

    /**
     * Obtiene todas las conversaciones en las que participa el usuario autenticado,
     * ordenadas de la más recientemente actualizada a la más antigua.
     *
     * @param principal usuario autenticado
     * @return listado de conversaciones del usuario
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.getConversationsForUser(principal.getUserId()));
    }

    /**
     * Obtiene el detalle de una conversación concreta, siempre que el usuario autenticado
     * sea uno de sus participantes.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la conversación
     * @return el detalle de la conversación solicitada
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationResponse> getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(conversationService.getConversation(id, principal.getUserId()));
    }

    /**
     * Obtiene todos los mensajes de una conversación, ordenados cronológicamente.
     * Al consultarlos se marcan como leídos los mensajes pendientes dirigidos al usuario
     * autenticado; este es el endpoint que el cliente sondea periódicamente para refrescar el chat.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la conversación
     * @return listado de mensajes de la conversación, del más antiguo al más reciente
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(conversationService.getMessages(id, principal.getUserId()));
    }
}
