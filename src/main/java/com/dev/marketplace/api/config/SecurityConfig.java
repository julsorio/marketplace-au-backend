package com.dev.marketplace.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.dev.marketplace.api.security.JwtAccessDeniedHandler;
import com.dev.marketplace.api.security.JwtAuthFilter;
import com.dev.marketplace.api.security.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;


/**
 * Configuración central de Spring Security de la API: define la cadena de filtros de
 * seguridad, las reglas de autorización por endpoint, la política CORS y los beans de
 * cifrado de contraseñas y autenticación usados por el resto de la aplicación.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${cors.allowed-origins}")
    private String serverUrl;

    /**
     * Define la cadena de filtros de seguridad de la aplicación: habilita CORS con la
     * configuración de {@link #corsConfigurationSource()}, desactiva CSRF (API stateless
     * consumida por un cliente que no usa sesiones ni cookies), fija la política de sesión
     * como stateless, registra los handlers de errores de autenticación/autorización
     * ({@link JwtAuthenticationEntryPoint} y {@link JwtAccessDeniedHandler}), declara qué
     * endpoints son públicos y cuáles requieren autenticación, y añade el
     * {@link JwtAuthFilter} antes del filtro estándar de autenticación por usuario/contraseña.
     *
     * @param http builder de configuración de seguridad HTTP proporcionado por Spring Security
     * @return la cadena de filtros de seguridad construida
     * @throws Exception si falla la construcción de la configuración de seguridad
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reviews/**").permitAll()
                        .requestMatchers("/categories/**").permitAll()
                        // /user/me va antes que /user/{id}: la regla más específica debe
                        // evaluarse primero para que "me" no caiga en el permitAll de abajo
                        .requestMatchers(HttpMethod.GET, "/user/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user/{id}").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Expone el encoder de contraseñas usado por la aplicación (BCrypt), tanto para
     * registrar usuarios como para verificar credenciales en el login.
     *
     * @return una instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el {@link AuthenticationManager} por defecto de Spring Security, necesario para
     * autenticar manualmente las credenciales de login en el flujo de autenticación propio.
     *
     * @param config configuración de autenticación gestionada por Spring Security
     * @return el {@link AuthenticationManager} de la configuración de autenticación
     * @throws Exception si Spring Security no puede resolver el {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define la política CORS de la API: solo permite peticiones desde el origen configurado
     * en {@code cors.allowed-origins}, con los métodos HTTP habituales, las cabeceras
     * {@code Authorization} y {@code Content-Type}, y con envío de credenciales habilitado.
     *
     * @return la fuente de configuración CORS aplicada a todas las rutas ({@code /**})
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(serverUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
