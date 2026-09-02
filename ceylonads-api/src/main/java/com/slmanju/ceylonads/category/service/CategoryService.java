package com.slmanju.ceylonads.category.service;

import com.slmanju.ceylonads.category.dto.CategoryResponse;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.mapper.CategoryMapper;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categories, CategoryMapper categoryMapper) {
        this.categories = categories;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllActive() {
        return categories.findAllByActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream().map(categoryMapper::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(String name, String slug, String parentSlug, int displayOrder) {
        if (categories.findBySlug(slug).isPresent()) {
            throw new BadRequestException("Category slug already exists");
        }
        Category parent = parentSlug == null || parentSlug.isBlank()
                ? null
                : categories.findBySlug(parentSlug)
                    .orElseThrow(() -> new NotFoundException("Parent category not found"));

        Category category = categories.save(new Category(name.trim(), slug.trim().toLowerCase(), parent, displayOrder));
        return categoryMapper.toResponse(category);
    }
}
