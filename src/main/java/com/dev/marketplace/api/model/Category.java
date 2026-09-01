package com.dev.marketplace.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Entidad MongoDB que representa una categoría (o subcategoría) del catálogo del marketplace.
 * Se persiste en la colección {@code categories}. Las categorías forman un árbol de dos
 * niveles: las categorías raíz tienen {@link #parentId} nulo, y sus subcategorías directas
 * referencian el id de la categoría raíz mediante {@link #parentId}.
 */
@Data
@Document(collection = "categories")
public class Category {
    /** Usamos un slug legible como id, ej: "electronics", "phones". */
    @Id
    private String id;

    private String name;

    /** Id de la categoría padre; {@code null} si esta categoría es raíz. */
    private String parentId;

    private String icon;
    private int displayOrder;
}
