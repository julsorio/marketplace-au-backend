package com.dev.marketplace.api.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.model.Category;
import com.dev.marketplace.api.repository.CategoryRepository;
import com.dev.marketplace.api.response.dto.CategoryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIdIsNull();

        return roots.stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(this::toResponseWithChildren)
                .toList();
    }

    private CategoryResponse toResponseWithChildren(Category category) {
        List<CategoryResponse> children = categoryRepository.findByParentId(category.getId()).stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(sub -> new CategoryResponse(sub.getId(), sub.getName(), sub.getIcon(), List.of()))
                .toList();

        return new CategoryResponse(category.getId(), category.getName(), category.getIcon(), children);
    }

    public boolean categoryExists(String categoryId) {
        return categoryRepository.existsById(categoryId);
    }
}
