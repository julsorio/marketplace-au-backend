package com.dev.marketplace.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.User;

public interface UserRepository extends MongoRepository<User, String> {
 Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
