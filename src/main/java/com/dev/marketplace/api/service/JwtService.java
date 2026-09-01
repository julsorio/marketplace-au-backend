package com.dev.marketplace.api.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio encargado de generar y validar los access tokens JWT (firmados con HS256)
 * que identifican a un usuario autenticado en las peticiones a la API.
 */
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Genera un access token JWT para el usuario indicado, con el id del usuario como subject,
     * el email y los roles como claims adicionales, y expiración a {@code jwtExpirationMs}
     * milisegundos desde el momento de la emisión.
     *
     * @param user usuario para el que se genera el token
     * @return access token JWT firmado, listo para enviar al cliente
     */
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Extrae el id de usuario (subject) contenido en un access token JWT.
     *
     * @param token access token JWT ya validado
     * @return id del usuario propietario del token
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Comprueba si un access token JWT es válido: firma correcta, formato bien construido
     * y no expirado.
     *
     * @param token access token JWT a validar
     * @return {@code true} si el token es válido; {@code false} si la firma no coincide,
     *         el token está expirado o está malformado
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parsea y valida la firma de un JWT, devolviendo el conjunto de claims contenidas en él.
     *
     * @param token access token JWT a parsear
     * @return claims del token
     * @throws JwtException si la firma no es válida, el token está expirado o está malformado
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Construye la clave de firma HMAC-SHA a partir del secreto configurado en
     * {@code jwt.secret}, usada tanto para firmar como para validar los tokens.
     *
     * @return clave de firma derivada del secreto configurado
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
