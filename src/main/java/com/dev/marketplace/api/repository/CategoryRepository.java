package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Category;

/**
 * Repositorio Spring Data MongoDB para el acceso a las categorías (colección
 * {@code categories}), incluyendo consultas derivadas para recorrer el árbol de dos niveles
 * (categorías raíz y sus subcategorías directas).
 */
public interface CategoryRepository extends MongoRepository<Category, String> {

    /**
     * Consulta derivada que devuelve las categorías raíz, es decir, aquellas cuyo
     * {@code parentId} es {@code null}.
     *
     * @return la lista de categorías sin categoría padre
     */
    List<Category> findByParentIdIsNull();

    /**
     * Consulta derivada que devuelve las subcategorías directas de una categoría dada.
     *
     * @param parentId id de la categoría padre
     * @return la lista de categorías cuyo {@code parentId} coincide con el indicado
     */
    List<Category> findByParentId(String parentId);

    /**
     * Comprueba si existe una categoría con el id indicado.
     *
     * @param id id (slug) de la categoría a comprobar
     * @return {@code true} si existe una categoría con ese id, {@code false} en caso contrario
     */
    boolean existsById(String id);
}
