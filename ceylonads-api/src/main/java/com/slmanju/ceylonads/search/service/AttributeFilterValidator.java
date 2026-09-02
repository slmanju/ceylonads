package com.slmanju.ceylonads.search.service;

import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.repository.AttributeDefinitionRepository;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.search.dto.AttributeFilterCriterion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rejects attr.* search filters that don't correspond to a real, filterable attribute definition
 * anywhere in the category tree, or whose value doesn't fit that definition's dataType - instead
 * of silently matching nothing (an unknown key) or coercing a bad value (e.g. a non-option SELECT
 * value). Looked up by key only (not scoped to the search's category filter, if any): the same key
 * can be filterable on multiple categories (e.g. "year" on both Cars and Motorcycles), and a
 * category/attribute combination that's simply a mismatch - not an unknown key - is expected to
 * fall through to zero matching rows rather than fail the request.
 */
@Service
public class AttributeFilterValidator {

    private final AttributeDefinitionRepository definitions;
    private final AttributeOptionRepository options;

    public AttributeFilterValidator(AttributeDefinitionRepository definitions, AttributeOptionRepository options) {
        this.definitions = definitions;
        this.options = options;
    }

    @Transactional(readOnly = true)
    public void validate(List<AttributeFilterCriterion> criteria) {
        if (criteria.isEmpty()) {
            return;
        }

        List<AttributeDefinition> candidates = definitions.findByKeyInAndActiveTrueAndFilterableTrue(
                criteria.stream().map(AttributeFilterCriterion::key).collect(Collectors.toSet()));

        // A key can be defined once per category (e.g. "subject" has its own row for each tuition
        // category), so all definitions sharing a key - not just the first one found - must
        // contribute their options; otherwise a value that's only valid under one category's
        // definition (e.g. CHESS under Other Education & Tuition) is wrongly rejected because an
        // arbitrarily-chosen sibling definition (e.g. School Tuition's subject list) was checked
        // instead. Mirrors the merge TuitionFilterMetadataService.optionsFor() does for filter
        // metadata, so search validation accepts exactly what that metadata advertises.
        Map<String, List<AttributeDefinition>> defsByKey = candidates.stream()
                .collect(Collectors.groupingBy(AttributeDefinition::getKey, LinkedHashMap::new, Collectors.toList()));

        List<Long> definitionIds = candidates.stream().map(AttributeDefinition::getId).toList();
        Map<Long, List<AttributeOption>> optionsByDefinition = options.findByAttributeDefinitionIdInAndActiveTrue(definitionIds)
                .stream().collect(Collectors.groupingBy(o -> o.getAttributeDefinition().getId()));

        for (AttributeFilterCriterion criterion : criteria) {
            List<AttributeDefinition> defs = defsByKey.get(criterion.key());
            if (defs == null || defs.isEmpty()) {
                throw new BadRequestException("Unsupported attribute filter: " + criterion.key());
            }
            List<AttributeOption> mergedOptions = defs.stream()
                    .flatMap(d -> optionsByDefinition.getOrDefault(d.getId(), List.of()).stream())
                    .toList();
            validateCriterion(defs.get(0), criterion, mergedOptions);
        }
    }

    private void validateCriterion(AttributeDefinition definition, AttributeFilterCriterion criterion, List<AttributeOption> defOptions) {
        AttributeDataType dataType = definition.getDataType();
        boolean numeric = dataType == AttributeDataType.NUMBER || dataType == AttributeDataType.DECIMAL;

        if (criterion.isRange()) {
            if (!numeric) {
                throw new BadRequestException(definition.getName() + " does not support a min/max range");
            }
            return;
        }

        String value = criterion.value();
        switch (dataType) {
            case NUMBER, DECIMAL -> {
                if (!isNumber(value)) {
                    throw new BadRequestException(definition.getName() + " must be a number");
                }
            }
            case BOOLEAN -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new BadRequestException(definition.getName() + " must be true or false");
                }
            }
            case SELECT, MULTI_SELECT -> {
                boolean matches = defOptions.stream().anyMatch(o -> o.getValue().equalsIgnoreCase(value));
                if (!matches) {
                    // Deliberately doesn't enumerate the full valid-options list: that list is
                    // master data (can be large and changes over time), and callers can already
                    // discover it via the corresponding filter-metadata endpoint.
                    throw new BadRequestException("Invalid " + definition.getKey() + " value: " + value);
                }
            }
            case TEXT -> {
                // Free text: any non-blank value (already guaranteed by AttributeFilterParams) is valid.
            }
        }
    }

    private boolean isNumber(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
