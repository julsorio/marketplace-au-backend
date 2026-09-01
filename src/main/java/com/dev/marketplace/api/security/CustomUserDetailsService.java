package com.dev.marketplace.api.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementación de {@link UserDetailsService} que carga los usuarios de la aplicación desde
 * MongoDB y los expone a Spring Security envueltos en un {@link UserPrincipal}. Se usa tanto
 * en la autenticación por email/contraseña como en la resolución del usuario a partir del
 * id contenido en el JWT.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Carga un usuario por su email, tal como requiere el contrato de {@link UserDetailsService}.
     *
     * @param email email del usuario a autenticar
     * @return los detalles del usuario envueltos en un {@link UserPrincipal}
     * @throws UsernameNotFoundException si no existe ningún usuario con ese email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return new UserPrincipal(user);
    }

    /**
     * Carga un usuario por su id. Se usa desde {@link JwtAuthFilter} para resolver al usuario
     * autenticado a partir del id extraído del JWT, en lugar de por email.
     *
     * @param userId identificador del usuario
     * @return los detalles del usuario envueltos en un {@link UserPrincipal}
     * @throws UsernameNotFoundException si no existe ningún usuario con ese id
     */
    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con id: " + userId));

        return new UserPrincipal(user);
    }
}
