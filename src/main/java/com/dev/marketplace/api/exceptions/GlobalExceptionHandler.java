package com.dev.marketplace.api.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.dev.marketplace.api.response.dto.ErrorResponse;

/**
 * Manejador global de excepciones de la API: centraliza la traducción de las excepciones
 * de negocio (paquete {@code exceptions}) y de Spring a respuestas HTTP consistentes con el
 * formato {@link ErrorResponse}, evitando repetir bloques try/catch en los controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Traduce un intento de registro con un email ya existente a un 409 Conflict.
     *
     * @param ex excepción lanzada al detectar el email duplicado
     * @return respuesta 409 Conflict con el mensaje de la excepción
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Traduce un fallo de autenticación (credenciales inválidas, ya sea nuestra excepción
     * de negocio o la de Spring Security) a un 401 Unauthorized.
     *
     * @param ex excepción de credenciales inválidas
     * @return respuesta 401 Unauthorized con el mensaje de la excepción
     */
    @ExceptionHandler({ InvalidCredentialsException.class, BadCredentialsException.class })
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Traduce los errores de validación de los DTOs anotados con {@code @Valid} a un
     * 400 Bad Request, incluyendo en la respuesta el detalle de cada campo inválido.
     *
     * @param ex excepción lanzada por Spring cuando falla la validación de un argumento
     * @return respuesta 400 Bad Request con la lista de errores por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", "Error de validación en los datos enviados", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Traduce un refresh token inválido o expirado a un 401 Unauthorized.
     *
     * @param ex excepción lanzada al validar el refresh token
     * @return respuesta 401 Unauthorized con el mensaje de la excepción
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Manejador de último recurso para cualquier excepción no controlada explícitamente por
     * los demás handlers de esta clase. Se traduce a un 500 Internal Server Error con un
     * mensaje genérico, sin exponer el detalle interno de la excepción al cliente.
     *
     * @param ex excepción no controlada
     * @param request petición en la que se produjo el error
     * @return respuesta 500 Internal Server Error con un mensaje genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Traduce la búsqueda de un listing inexistente a un 404 Not Found.
     *
     * @param ex excepción lanzada al no encontrar el listing
     * @return respuesta 404 Not Found con el mensaje de la excepción
     */
    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleListingNotFound(ListingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()));
    }

    /**
     * Traduce un acceso no autorizado a un listing ajeno a un 403 Forbidden.
     *
     * @param ex excepción lanzada al detectar que el usuario no es propietario del listing
     * @return respuesta 403 Forbidden con el mensaje de la excepción
     */
    @ExceptionHandler(UnauthorizedListingAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedListing(UnauthorizedListingAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage()));
    }

    /**
     * Traduce una categoría no válida o inexistente a un 400 Bad Request.
     *
     * @param ex excepción lanzada al no reconocer la categoría indicada
     * @return respuesta 400 Bad Request con el mensaje de la excepción
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()));
    }

    /**
     * Traduce la búsqueda de una conversación inexistente a un 404 Not Found.
     *
     * @param ex excepción lanzada al no encontrar la conversación
     * @return respuesta 404 Not Found con el mensaje de la excepción
     */
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()));
    }

    /**
     * Traduce un acceso no autorizado a una conversación ajena a un 403 Forbidden.
     *
     * @param ex excepción lanzada al detectar que el usuario no participa en la conversación
     * @return respuesta 403 Forbidden con el mensaje de la excepción
     */
    @ExceptionHandler(UnauthorizedConversationAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedConversation(UnauthorizedConversationAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage()));
    }

    /**
     * Traduce la búsqueda de una transacción inexistente a un 404 Not Found.
     *
     * @param ex excepción lanzada al no encontrar la transacción
     * @return respuesta 404 Not Found con el mensaje de la excepción
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()));
    }

    /**
     * Traduce una operación no permitida para el estado actual de un listing a un
     * 409 Conflict.
     *
     * @param ex excepción lanzada al detectar un estado de listing incompatible con la operación
     * @return respuesta 409 Conflict con el mensaje de la excepción
     */
    @ExceptionHandler(InvalidListingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidListingState(InvalidListingStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    /**
     * Traduce un acceso no autorizado a una transacción ajena a un 403 Forbidden.
     *
     * @param ex excepción lanzada al detectar que el usuario no participa en la transacción
     * @return respuesta 403 Forbidden con el mensaje de la excepción
     */
    @ExceptionHandler(UnauthorizedTransactionAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedTransaction(UnauthorizedTransactionAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage()));
    }

    /**
     * Traduce el intento de dejar una reseña que incumple las reglas de negocio a un
     * 400 Bad Request.
     *
     * @param ex excepción lanzada al no permitirse la reseña
     * @return respuesta 400 Bad Request con el mensaje de la excepción
     */
    @ExceptionHandler(ReviewNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotAllowed(ReviewNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()));
    }

    /**
     * Traduce el intento de dejar una reseña duplicada sobre la misma transacción a un
     * 409 Conflict.
     *
     * @param ex excepción lanzada al detectar la reseña duplicada
     * @return respuesta 409 Conflict con el mensaje de la excepción
     */
    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReview(DuplicateReviewException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    /**
     * Traduce la búsqueda de un usuario inexistente a un 404 Not Found.
     *
     * @param ex excepción lanzada al no encontrar el usuario
     * @return respuesta 404 Not Found con el mensaje de la excepción
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()));
    }
}
