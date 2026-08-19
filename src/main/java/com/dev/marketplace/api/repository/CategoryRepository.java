package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Category;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentIdIsNull();

    List<Category> findByParentId(String parentId);

    boolean existsById(String id);
}
