package com.slmanju.ceylonads.tuition.service;

import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.AttributeDefinitionRepository;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.tuition.dto.TuitionFilterMetadataResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionFilterOptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Isolated master-data read for the CeylonAds Tuition UI's filter panel, covering the whole
// tuition vertical (education-tuition and every one of its direct children), not a single leaf
// category. Reads the same attribute_definitions/attribute_options tables the generic category
// domain owns, scoped to the small fixed set of tuition attribute keys - mirrors the read-path
// style TuitionClassService uses for the same tables, kept separate from
// CategoryController/AttributeDefinitionService so the generic /api/categories/{slug}/filters
// contract never has to account for tuition-specific shape.
@Service
public class TuitionFilterMetadataService {

    // Master data root, from V3/V9/V10 migrations.
    private static final String TUITION_ROOT_SLUG = "education-tuition";

    // "level" and "delivery_mode" don't exist as attribute keys yet; grade/classMode are the
    // actual keys used across the tuition vertical (see
    // V10__tuition_filter_master_data.sql).
    private static final String SUBJECT_KEY = "subject";
    private static final String LEVEL_KEY = "grade";
    private static final String CURRICULUM_KEY = "curriculum";
    private static final String MEDIUM_KEY = "medium";
    private static final String DELIVERY_MODE_KEY = "classMode";

    private static final Set<String> FILTER_KEYS =
            Set.of(SUBJECT_KEY, LEVEL_KEY, CURRICULUM_KEY, MEDIUM_KEY, DELIVERY_MODE_KEY);

    private final CategoryRepository categories;
    private final AttributeDefinitionRepository attributeDefinitions;
    private final AttributeOptionRepository attributeOptions;

    public TuitionFilterMetadataService(
            CategoryRepository categories,
            AttributeDefinitionRepository attributeDefinitions,
            AttributeOptionRepository attributeOptions) {
        this.categories = categories;
        this.attributeDefinitions = attributeDefinitions;
        this.attributeOptions = attributeOptions;
    }

    // Query shape: root category lookup (1), its direct children (1), active tuition-key
    // definitions across those children (1), active options for the definitions found (0-1) - no
    // ads, promotions, locations, or unrelated categories are ever touched, and the category tree
    // is walked exactly one level deep rather than recursively.
    //
    // Each child category has its own copy of subject/grade/classMode (where relevant) rather than
    // one shared definition on the root - see V10__tuition_filter_master_data.sql for why: ad
    // posting resolves attribute definitions strictly by the ad's own leaf category, with no
    // ancestor-chain inheritance, so a root-only definition would be invisible to Post Ad
    // validation for every leaf category. This method reassembles those per-category definitions
    // into one merged, deduplicated list per filter key.
    @Transactional(readOnly = true)
    public TuitionFilterMetadataResponse getFilters() {
        Category root = categories.findBySlugAndActiveTrue(TUITION_ROOT_SLUG)
                .orElseThrow(() -> new NotFoundException("Tuition root category not found"));

        List<Category> children = categories.findByParentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(root.getId());
        Map<Long, Integer> categoryDisplayOrder = children.stream()
                .collect(Collectors.toMap(Category::getId, Category::getDisplayOrder));
        List<Long> childIds = children.stream().map(Category::getId).toList();

        List<AttributeDefinition> defs = childIds.isEmpty()
                ? List.of()
                : attributeDefinitions.findByCategoryIdInAndKeyInAndActiveTrue(childIds, FILTER_KEYS);

        Map<Long, List<AttributeOption>> optionsByDefinitionId = loadOptions(defs);

        // Group definitions by key, ordered by their owning category's display_order so the
        // merged list reads in the same School Tuition -> ... -> Other Education & Tuition
        // grouping the categories themselves use, rather than arbitrary id order.
        Map<String, List<AttributeDefinition>> defsByKey = defs.stream()
                .collect(Collectors.groupingBy(AttributeDefinition::getKey));
        defsByKey.values().forEach(list -> list.sort(
                Comparator.comparingInt((AttributeDefinition d) -> categoryDisplayOrder.getOrDefault(d.getCategory().getId(), 0))
                        .thenComparingInt(AttributeDefinition::getDisplayOrder)));

        return new TuitionFilterMetadataResponse(
                optionsFor(defsByKey, optionsByDefinitionId, SUBJECT_KEY),
                optionsFor(defsByKey, optionsByDefinitionId, LEVEL_KEY),
                optionsFor(defsByKey, optionsByDefinitionId, CURRICULUM_KEY),
                optionsFor(defsByKey, optionsByDefinitionId, MEDIUM_KEY),
                optionsFor(defsByKey, optionsByDefinitionId, DELIVERY_MODE_KEY));
    }

    private Map<Long, List<AttributeOption>> loadOptions(List<AttributeDefinition> definitions) {
        List<Long> definitionIds = definitions.stream().map(AttributeDefinition::getId).toList();
        if (definitionIds.isEmpty()) {
            return Map.of();
        }
        return attributeOptions.findByAttributeDefinitionIdInAndActiveTrue(definitionIds).stream()
                .collect(Collectors.groupingBy(o -> o.getAttributeDefinition().getId()));
    }

    // Merges options from every category's own definition for this key into one list, deduping by
    // stable value (first occurrence wins) since the same canonical option (e.g. ACCOUNTING) can
    // legitimately be attached to more than one category's definition.
    private List<TuitionFilterOptionResponse> optionsFor(
            Map<String, List<AttributeDefinition>> defsByKey,
            Map<Long, List<AttributeOption>> optionsByDefinitionId,
            String key) {
        List<AttributeDefinition> defs = defsByKey.getOrDefault(key, List.of());
        Map<String, TuitionFilterOptionResponse> merged = new LinkedHashMap<>();
        for (AttributeDefinition def : defs) {
            optionsByDefinitionId.getOrDefault(def.getId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(AttributeOption::getDisplayOrder).thenComparing(AttributeOption::getId))
                    .forEach(o -> merged.putIfAbsent(o.getValue(), new TuitionFilterOptionResponse(o.getValue(), o.getLabel())));
        }
        return List.copyOf(merged.values());
    }
}
