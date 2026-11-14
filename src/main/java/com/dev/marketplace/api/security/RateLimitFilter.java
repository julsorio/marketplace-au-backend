package com.dev.marketplace.api.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dev.marketplace.api.response.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Rate limiting por IP sobre los endpoints públicos de /auth, objetivo típico de ataques
 * automatizados (fuerza bruta de credenciales, registro masivo, abuso del endpoint de
 * refresco). Límite distinto por endpoint según su superficie de abuso:
 * - login: el más restrictivo, es la superficie de fuerza bruta de credenciales.
 * - register: ventana larga pero pocos intentos, para frenar altas masivas automatizadas
 *   sin bloquear a alguien que simplemente se equivocó una vez al registrarse.
 * - refresh: el más permisivo, porque un usuario legítimo con varias pestañas/dispositivos
 *   puede refrescar su sesión con cierta frecuencia sin que sea un ataque.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    private record LimitRule(int maxRequests, Duration window) {}

    private static final Map<String, LimitRule> LIMITS = Map.of(
            "/auth/login", new LimitRule(5, Duration.ofMinutes(1)),
            "/auth/register", new LimitRule(5, Duration.ofHours(1)),
            "/auth/refresh", new LimitRule(20, Duration.ofMinutes(1)));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LimitRule rule = LIMITS.get(request.getRequestURI());

        if (rule == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ":" + request.getRequestURI();

        if (!rateLimiterService.tryConsume(key, rule.maxRequests(), rule.window())) {
            respondTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Detrás de un proxy/load balancer, la IP real del cliente llega en X-Forwarded-For (el
    // primer valor de la lista); sin proxy por delante, getRemoteAddr() ya es la IP real.
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Demasiados intentos. Inténtalo de nuevo en unos minutos.");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
