package com.slmanju.ceylonads.category.controller;

import com.slmanju.ceylonads.category.dto.AttributeDefinitionResponse;
import com.slmanju.ceylonads.category.dto.CategoryFiltersResponse;
import com.slmanju.ceylonads.category.dto.CategoryResponse;
import com.slmanju.ceylonads.category.service.AttributeDefinitionService;
import com.slmanju.ceylonads.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AttributeDefinitionService attributeDefinitionService;

    public CategoryController(CategoryService categoryService, AttributeDefinitionService attributeDefinitionService) {
        this.categoryService = categoryService;
        this.attributeDefinitionService = attributeDefinitionService;
    }

    @GetMapping
    @Operation(summary = "List active categories")
    List<CategoryResponse> list() {
        return categoryService.findAllActive();
    }

    @GetMapping("/{slug}/attributes")
    @Operation(summary = "List active attribute definitions for a category",
            description = "Includes each attribute's active SELECT/MULTI_SELECT options. "
                    + "The frontend can build a Post Ad form or a filter panel from this response alone.")
    List<AttributeDefinitionResponse> attributes(@PathVariable String slug) {
        return attributeDefinitionService.findActiveByCategorySlug(slug);
    }

    @GetMapping("/{slug}/filters")
    @Operation(summary = "Get search-filter metadata for a category",
            description = "Active, filterable attribute definitions for this category plus any inherited from its "
                    + "ancestor categories, for building a search filter panel (e.g. GET /api/ads?category=<slug>"
                    + "&attr.<key>=<value>).")
    CategoryFiltersResponse filters(@PathVariable String slug) {
        return attributeDefinitionService.findFiltersByCategorySlug(slug);
    }
}
