package com.dev.marketplace.api.response.dto;

import java.time.Instant;
import java.util.List;

/**
 * Formato estándar de error de la API, devuelto en el cuerpo de las respuestas cuando una petición
 * falla.
 *
 * @param timestamp instante en el que se generó el error
 * @param status código de estado HTTP asociado al error
 * @param error nombre corto del tipo de error (por ejemplo, el reason phrase del status HTTP)
 * @param message mensaje descriptivo del error
 * @param details lista opcional de detalles adicionales (por ejemplo, errores de validación por
 *        campo); {@code null} si no aplica
 */
public record ErrorResponse( Instant timestamp,
    int status,
    String error,
    String message,
    List<String> details) {
    /**
     * Crea una respuesta de error sin detalles adicionales, usando el instante actual como timestamp.
     *
     * @param status código de estado HTTP
     * @param error nombre corto del tipo de error
     * @param message mensaje descriptivo del error
     */
    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, null);
    }

    /**
     * Crea una respuesta de error con una lista de detalles adicionales, usando el instante actual
     * como timestamp.
     *
     * @param status código de estado HTTP
     * @param error nombre corto del tipo de error
     * @param message mensaje descriptivo del error
     * @param details detalles adicionales del error (por ejemplo, errores de validación por campo)
     */
    public ErrorResponse(int status, String error, String message, List<String> details) {
        this(Instant.now(), status, error, message, details);
    }
}
