package com.slmanju.ceylonads.category.service;

import com.slmanju.ceylonads.admin.dto.AdminAttributeDefinitionRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeDefinitionUpdateRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeOptionRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeOptionUpdateRequest;
import com.slmanju.ceylonads.category.dto.AttributeDefinitionResponse;
import com.slmanju.ceylonads.category.dto.AttributeOptionResponse;
import com.slmanju.ceylonads.category.dto.CategoryFiltersResponse;
import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.mapper.AttributeDefinitionMapper;
import com.slmanju.ceylonads.category.mapper.CategoryMapper;
import com.slmanju.ceylonads.category.repository.AttributeDefinitionRepository;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttributeDefinitionService {

    private static final Set<AttributeDataType> OPTION_BACKED_TYPES = Set.of(AttributeDataType.SELECT, AttributeDataType.MULTI_SELECT);

    private final CategoryRepository categories;
    private final AttributeDefinitionRepository definitions;
    private final AttributeOptionRepository options;
    private final AttributeDefinitionMapper mapper;
    private final CategoryHierarchyService categoryHierarchy;
    private final CategoryMapper categoryMapper;

    public AttributeDefinitionService(
            CategoryRepository categories,
            AttributeDefinitionRepository definitions,
            AttributeOptionRepository options,
            AttributeDefinitionMapper mapper,
            CategoryHierarchyService categoryHierarchy,
            CategoryMapper categoryMapper) {
        this.categories = categories;
        this.definitions = definitions;
        this.options = options;
        this.mapper = mapper;
        this.categoryHierarchy = categoryHierarchy;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> findActiveByCategorySlug(String slug) {
        Category category = categories.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return definitions.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscIdAsc(category.getId()).stream()
                .map(def -> mapper.toResponse(def, options.findByAttributeDefinitionIdAndActiveTrueOrderByDisplayOrderAscIdAsc(def.getId())))
                .toList();
    }

    // Search-filter metadata for the given category: its own filterable attributes plus any
    // inherited from ancestor categories (root-first order), so a parent category can define
    // shared filters its descendants pick up automatically.
    @Transactional(readOnly = true)
    public CategoryFiltersResponse findFiltersByCategorySlug(String slug) {
        Category category = categories.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        List<Long> chainIds = categoryHierarchy.ancestorChainInclusive(category).stream()
                .map(Category::getId)
                .toList();
        List<AttributeDefinition> defs = definitions
                .findByCategoryIdInAndActiveTrueAndFilterableTrueOrderByDisplayOrderAscIdAsc(chainIds);

        List<Long> defIds = defs.stream().map(AttributeDefinition::getId).toList();
        Map<Long, List<AttributeOption>> optionsByDefinition = options.findByAttributeDefinitionIdInAndActiveTrue(defIds)
                .stream().collect(Collectors.groupingBy(o -> o.getAttributeDefinition().getId()));

        List<AttributeDefinitionResponse> filters = defs.stream()
                .map(def -> mapper.toResponse(def, optionsByDefinition.getOrDefault(def.getId(), List.of())))
                .toList();

        return new CategoryFiltersResponse(categoryMapper.toResponse(category), filters);
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> findAllByCategory(Long categoryId) {
        requireCategory(categoryId);
        return definitions.findByCategoryIdOrderByDisplayOrderAscIdAsc(categoryId).stream()
                .map(def -> mapper.toResponse(def, options.findByAttributeDefinitionIdOrderByDisplayOrderAscIdAsc(def.getId())))
                .toList();
    }

    @Transactional
    public AttributeDefinitionResponse create(Long categoryId, AdminAttributeDefinitionRequest request) {
        Category category = requireCategory(categoryId);

        if (definitions.existsByCategoryIdAndKey(categoryId, request.key())) {
            throw new BadRequestException("An attribute with key '" + request.key() + "' already exists for this category");
        }
        if (OPTION_BACKED_TYPES.contains(request.dataType()) && (request.options() == null || request.options().isEmpty())) {
            throw new BadRequestException("SELECT and MULTI_SELECT attributes require at least one option");
        }

        AttributeDefinition definition = definitions.save(new AttributeDefinition(
                category, request.key().trim(), request.name().trim(), request.dataType(),
                request.required(), request.filterable(), request.searchable(), blankToNull(request.unit()), request.displayOrder()));

        List<AttributeOption> savedOptions = List.of();
        if (OPTION_BACKED_TYPES.contains(request.dataType())) {
            savedOptions = request.options().stream()
                    .map(o -> options.save(new AttributeOption(definition, o.value().trim(), o.label().trim(), o.displayOrder())))
                    .toList();
        }

        return mapper.toResponse(definition, savedOptions);
    }

    @Transactional
    public AttributeDefinitionResponse update(Long categoryId, Long attributeId, AdminAttributeDefinitionUpdateRequest request) {
        AttributeDefinition definition = requireDefinition(categoryId, attributeId);
        definition.update(request.name().trim(), request.required(), request.filterable(), request.searchable(),
                blankToNull(request.unit()), request.displayOrder(), request.active());
        return mapper.toResponse(definition, options.findByAttributeDefinitionIdOrderByDisplayOrderAscIdAsc(definition.getId()));
    }

    @Transactional
    public AttributeDefinitionResponse setActive(Long categoryId, Long attributeId, boolean active) {
        AttributeDefinition definition = requireDefinition(categoryId, attributeId);
        definition.setActive(active);
        return mapper.toResponse(definition, options.findByAttributeDefinitionIdOrderByDisplayOrderAscIdAsc(definition.getId()));
    }

    @Transactional
    public AttributeOptionResponse addOption(Long categoryId, Long attributeId, AdminAttributeOptionRequest request) {
        AttributeDefinition definition = requireDefinition(categoryId, attributeId);
        if (options.existsByAttributeDefinitionIdAndValue(definition.getId(), request.value())) {
            throw new BadRequestException("An option with value '" + request.value() + "' already exists for this attribute");
        }
        AttributeOption option = options.save(new AttributeOption(definition, request.value().trim(), request.label().trim(), request.displayOrder()));
        return mapper.toResponse(option);
    }

    @Transactional
    public AttributeOptionResponse updateOption(Long categoryId, Long attributeId, Long optionId, AdminAttributeOptionUpdateRequest request) {
        requireDefinition(categoryId, attributeId);
        AttributeOption option = options.findByIdAndAttributeDefinitionId(optionId, attributeId)
                .orElseThrow(() -> new NotFoundException("Option not found"));
        option.update(request.label().trim(), request.displayOrder(), request.active());
        return mapper.toResponse(option);
    }

    @Transactional
    public AttributeOptionResponse setOptionActive(Long categoryId, Long attributeId, Long optionId, boolean active) {
        requireDefinition(categoryId, attributeId);
        AttributeOption option = options.findByIdAndAttributeDefinitionId(optionId, attributeId)
                .orElseThrow(() -> new NotFoundException("Option not found"));
        option.setActive(active);
        return mapper.toResponse(option);
    }

    private Category requireCategory(Long categoryId) {
        return categories.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private AttributeDefinition requireDefinition(Long categoryId, Long attributeId) {
        requireCategory(categoryId);
        return definitions.findByIdAndCategoryId(attributeId, categoryId)
                .orElseThrow(() -> new NotFoundException("Attribute definition not found"));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
