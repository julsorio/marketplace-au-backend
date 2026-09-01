package com.dev.marketplace.api.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dev.marketplace.api.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtro que se ejecuta una vez por petición para autenticar al usuario a partir del JWT
 * enviado en la cabecera {@code Authorization}. Si el token es válido, deja al usuario
 * autenticado en el {@link SecurityContextHolder} para el resto de la cadena de filtros;
 * en caso contrario, deja pasar la petición como anónima (la propia configuración de
 * seguridad decidirá si el endpoint requiere autenticación).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;


    /**
     * Extrae el JWT de la cabecera {@code Authorization} (si existe y usa el esquema
     * {@code Bearer}), lo valida y, si es correcto, autentica al usuario correspondiente en
     * el contexto de seguridad antes de continuar con la cadena de filtros.
     * Si no hay token, el token no es válido, o el usuario del token ya no existe
     * ({@link UsernameNotFoundException}), la petición continúa sin autenticar.
     *
     * @param request petición HTTP entrante
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros de servlet a continuar
     * @throws ServletException si falla el procesamiento del filtro
     * @throws IOException si falla la lectura o escritura de la petición o respuesta
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {
            String userId = jwtService.extractUserId(token);
            
            try {
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                
                SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
            } catch (UsernameNotFoundException  e) {
            
            }
        }

        filterChain.doFilter(request, response);
    }

}
