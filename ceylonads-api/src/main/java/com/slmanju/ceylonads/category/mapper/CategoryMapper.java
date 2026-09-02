package com.slmanju.ceylonads.category.mapper;

import com.slmanju.ceylonads.category.dto.CategoryResponse;
import com.slmanju.ceylonads.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getDisplayOrder(),
                category.isActive());
    }
}
