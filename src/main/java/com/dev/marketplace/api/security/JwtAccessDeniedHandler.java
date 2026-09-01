package com.dev.marketplace.api.security;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Handler de Spring Security que se invoca cuando un usuario autenticado intenta acceder a
 * un recurso para el que no tiene permisos suficientes. Responde con un cuerpo JSON con
 * estado 403 Forbidden, en lugar de la página de error por defecto de Spring.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Construye y escribe en la respuesta el cuerpo JSON de error 403 Forbidden.
     *
     * @param request petición que provocó el acceso denegado
     * @param response respuesta HTTP sobre la que se escribe el cuerpo de error
     * @param accessDeniedException excepción de Spring Security que originó la denegación de acceso
     * @throws IOException si falla la escritura del cuerpo JSON en la respuesta
     * @throws ServletException si falla el procesamiento del servlet
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "No tienes permisos para acceder a este recurso");

        objectMapper.writeValue(response.getOutputStream(), body);
    }

}
