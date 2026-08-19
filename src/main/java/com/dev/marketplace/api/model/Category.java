package com.dev.marketplace.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "categories")
public class Category {
    @Id
    private String id; // usamos un slug legible como id, ej: "electronics", "phones"

    private String name;
    private String parentId; // null si es categoría raíz
    private String icon;
    private int displayOrder;
}
