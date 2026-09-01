package com.dev.marketplace.api.util;

import java.util.Random;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * Difumina la ubicación exacta de un listing para quien no es el dueño, por privacidad
 * del vendedor: en vez de la posición real, devuelve un punto desplazado aleatoriamente
 * dentro de un radio fijo alrededor de ella.
 *
 * El desplazamiento es determinista para un mismo listingId (misma semilla → mismo punto
 * en cada petición), para que el mapa no "salte" de sitio en cada recarga aunque el punto
 * mostrado no sea el real.
 *
 * Importante: esto es solo ofuscación, no privacidad criptográfica — el desplazamiento es
 * reproducible por cualquiera que conozca este algoritmo y el id del listing. Sirve para no
 * exponer la dirección exacta por defecto, no como medida de seguridad fuerte.
 *
 * Esta clase solo debe usarse para construir las respuestas expuestas por la API a quien no
 * es el dueño del anuncio; las búsquedas por radio (ListingSearchRepository) deben seguir
 * usando siempre las coordenadas reales guardadas en el listing.
 */
public final class LocationFuzzer {

    private static final double RADIUS_METERS = 300;
    private static final double EARTH_RADIUS_METERS = 6_378_100.0;

    private LocationFuzzer() {
    }

    /**
     * Calcula un punto desplazado aleatoriamente respecto a {@code exactLocation}, dentro de
     * un radio de {@value #RADIUS_METERS} metros, usando el hash de {@code listingId} como
     * semilla para que el resultado sea siempre el mismo para un mismo listing.
     *
     * @param exactLocation ubicación geográfica real del listing (longitud, latitud)
     * @param listingId identificador del listing, usado como semilla determinista del desplazamiento
     * @return un array {@code [latitud, longitud]} con el punto difuminado
     */
    public static double[] fuzz(GeoJsonPoint exactLocation, String listingId) {
        double exactLat = exactLocation.getY();
        double exactLng = exactLocation.getX();

        Random random = new Random(listingId.hashCode());
        double angle = random.nextDouble() * 2 * Math.PI;
        double distanceMeters = random.nextDouble() * RADIUS_METERS;

        double latOffsetRad = (distanceMeters * Math.cos(angle)) / EARTH_RADIUS_METERS;
        double lngOffsetRad = (distanceMeters * Math.sin(angle))
                / (EARTH_RADIUS_METERS * Math.cos(Math.toRadians(exactLat)));

        double fuzzedLat = exactLat + Math.toDegrees(latOffsetRad);
        double fuzzedLng = exactLng + Math.toDegrees(lngOffsetRad);

        return new double[] { fuzzedLat, fuzzedLng };
    }
}
