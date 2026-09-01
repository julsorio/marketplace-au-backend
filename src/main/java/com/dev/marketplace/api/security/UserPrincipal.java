package com.dev.marketplace.api.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dev.marketplace.api.model.User;

/**
 * Adaptador de {@link User} al contrato {@link UserDetails} que requiere Spring Security,
 * de forma que el modelo de dominio de usuario pueda usarse directamente como principal de
 * autenticación sin duplicar sus datos.
 */
public class UserPrincipal implements UserDetails {
    private final User user;

    /**
     * Crea el principal envolviendo al usuario de dominio.
     *
     * @param user usuario autenticado a exponer como {@link UserDetails}
     */
    public UserPrincipal(User user) {
        this.user = user;
    }

    /**
     * Devuelve los roles del usuario como authorities de Spring Security.
     *
     * @return las authorities del usuario, o una lista vacía si el usuario no tiene roles asignados
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<String> roles = user.getRoles();
        if (Objects.isNull(roles)) {
            return List.of();
        }
        
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    /**
     * Devuelve el hash de la contraseña del usuario, usado por Spring Security para comparar
     * credenciales durante la autenticación.
     *
     * @return el hash de la contraseña del usuario
     */
    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Devuelve el email del usuario, que actúa como nombre de usuario ("username") a efectos
     * de Spring Security.
     *
     * @return el email del usuario
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Indica si la cuenta no ha expirado. Esta aplicación no implementa expiración de cuentas,
     * por lo que siempre devuelve {@code true}.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta no está bloqueada, en función de si el estado del usuario es
     * "suspended".
     *
     * @return {@code true} si el usuario no está suspendido, {@code false} en caso contrario
     */
    @Override
    public boolean isAccountNonLocked() {
        return !"suspended".equals(user.getStatus());
    }

    /**
     * Indica si las credenciales no han expirado. Esta aplicación no implementa expiración de
     * credenciales, por lo que siempre devuelve {@code true}.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Devuelve el usuario de dominio envuelto por este principal, para cuando el código
     * de negocio necesita más datos que los expuestos por {@link UserDetails}.
     *
     * @return el usuario de dominio autenticado
     */
    public User getUser() {
        return user;
    }

    /**
     * Devuelve el id del usuario autenticado.
     *
     * @return el identificador del usuario
     */
    public String getUserId() {
        return user.getId();
    }
}
