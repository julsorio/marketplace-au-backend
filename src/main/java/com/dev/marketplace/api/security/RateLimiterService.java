package com.dev.marketplace.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limitador de peticiones en memoria, por ventana fija (fixed window) y clave arbitraria
 * (aquí, IP + endpoint). Pensado para una única instancia del backend: si en el futuro se
 * despliega detrás de un balanceador con varias instancias, cada una llevaría su propio
 * contador y el límite real efectivo sería (límite configurado × nº de instancias) — en ese
 * caso habría que sustituir este componente por uno respaldado en un almacén compartido
 * (p.ej. Redis). Para una única instancia (que es el despliegue actual) es suficiente.
 */
@Component
public class RateLimiterService {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int maxRequests, Duration windowDuration) {
        Instant now = Instant.now();
        Window window = windows.computeIfAbsent(key, k -> new Window());

        synchronized (window) {
            if (window.start == null || Duration.between(window.start, now).compareTo(windowDuration) >= 0) {
                window.start = now;
                window.count = 0;
            }

            if (window.count >= maxRequests) {
                return false;
            }

            window.count++;
            return true;
        }
    }

    // Limpieza periódica: sin esto, el mapa acumularía para siempre una entrada por cada IP
    // que alguna vez haya llamado a un endpoint limitado. Ninguna ventana configurada supera
    // 1 hora, así que cualquier entrada más vieja que eso ya no es relevante.
    @Scheduled(fixedRate = 600_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        windows.entrySet().removeIf(entry -> {
            Window w = entry.getValue();
            synchronized (w) {
                return w.start == null || w.start.isBefore(cutoff);
            }
        });
    }

    private static class Window {
        Instant start;
        int count;
    }
}
