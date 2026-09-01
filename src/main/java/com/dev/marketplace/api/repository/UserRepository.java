package com.dev.marketplace.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.User;

/**
 * Repositorio Spring Data MongoDB para la colección {@code users}, con las
 * consultas por email necesarias para el login y el alta de usuarios.
 */
public interface UserRepository extends MongoRepository<User, String> {
    /**
     * Busca un usuario por su email exacto.
     *
     * @param email email del usuario a buscar
     * @return el usuario con ese email, o vacío si no existe ninguno
     */
    Optional<User> findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario registrado con ese email.
     *
     * @param email email a comprobar
     * @return {@code true} si ya hay un usuario con ese email; {@code false} en caso contrario
     */
    boolean existsByEmail(String email);
}
