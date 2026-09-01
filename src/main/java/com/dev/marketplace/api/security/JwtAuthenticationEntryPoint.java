package com.dev.marketplace.api.security;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Punto de entrada de Spring Security que se invoca cuando una petición no autenticada
 * intenta acceder a un recurso protegido. Responde con un cuerpo JSON con estado
 * 401 Unauthorized, en lugar de redirigir a una página de login.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Construye y escribe en la respuesta el cuerpo JSON de error 401 Unauthorized.
     *
     * @param request petición que provocó el fallo de autenticación
     * @param response respuesta HTTP sobre la que se escribe el cuerpo de error
     * @param authException excepción de Spring Security que originó el fallo de autenticación
     * @throws IOException si falla la escritura del cuerpo JSON en la respuesta
     * @throws ServletException si falla el procesamiento del servlet
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Token inválido, expirado o no proporcionado");

        objectMapper.writeValue(response.getOutputStream(), body);
    }

}
