package com.slmanju.ceylonads.ad.service;

import com.slmanju.ceylonads.ad.dto.AdAttributeResponse;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import com.slmanju.ceylonads.ad.repository.AdAttributeValueRepository;
import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.AttributeDefinitionRepository;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates category-attribute submissions on ad create/update and maps persisted values back to
 * a response shape. Shared by the write path (AdService) and the read path (AdMapper) so option
 * label resolution / MULTI_SELECT grouping only lives once.
 */
@Service
public class AdAttributeService {

    private final AttributeDefinitionRepository definitions;
    private final AttributeOptionRepository options;
    private final AdAttributeValueRepository values;

    public AdAttributeService(
            AttributeDefinitionRepository definitions,
            AttributeOptionRepository options,
            AdAttributeValueRepository values) {
        this.definitions = definitions;
        this.options = options;
        this.values = values;
    }

    @Transactional
    public void replaceValues(Ad ad, Map<String, String> raw) {
        values.deleteByAdId(ad.getId());
        List<AdAttributeValue> built = buildValues(ad, raw == null ? Map.of() : raw);
        values.saveAll(built);
    }

    // Single ad detail path: one query total (value + definition + options all fetched together).
    @Transactional(readOnly = true)
    public List<AdAttributeResponse> toResponses(Long adId) {
        List<AdAttributeValue> rows = values.findDetailedByAdId(adId);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<AdAttributeResponse> responses = new ArrayList<>();
        for (List<AdAttributeValue> group : groupByDefinition(rows).values()) {
            AttributeDefinition definition = group.get(0).getAttributeDefinition();
            responses.add(toResponse(group, definition.getOptions()));
        }
        return responses;
    }

    // List/card path: two queries total regardless of how many ads are passed in (values+
    // definitions batched by ad id, then options batched by the small set of distinct definition
    // ids actually referenced), instead of the single-ad query's 2N-per-ad cost.
    @Transactional(readOnly = true)
    public Map<Long, List<AdAttributeResponse>> toResponsesForAds(Collection<Long> adIds) {
        if (adIds.isEmpty()) {
            return Map.of();
        }
        List<AdAttributeValue> rows = values.findByAdIdInOrderByDefinitionDisplayOrder(adIds);
        if (rows.isEmpty()) {
            return Map.of();
        }

        List<Long> definitionIds = rows.stream().map(v -> v.getAttributeDefinition().getId()).distinct().toList();
        Map<Long, List<AttributeOption>> optionsByDefinition = options.findByAttributeDefinitionIdIn(definitionIds)
                .stream().collect(Collectors.groupingBy(o -> o.getAttributeDefinition().getId()));

        Map<Long, List<AdAttributeValue>> rowsByAd = new LinkedHashMap<>();
        for (AdAttributeValue row : rows) {
            rowsByAd.computeIfAbsent(row.getAd().getId(), k -> new ArrayList<>()).add(row);
        }

        Map<Long, List<AdAttributeResponse>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<AdAttributeValue>> adEntry : rowsByAd.entrySet()) {
            List<AdAttributeResponse> responses = new ArrayList<>();
            for (List<AdAttributeValue> group : groupByDefinition(adEntry.getValue()).values()) {
                Long definitionId = group.get(0).getAttributeDefinition().getId();
                responses.add(toResponse(group, optionsByDefinition.getOrDefault(definitionId, List.of())));
            }
            result.put(adEntry.getKey(), responses);
        }
        return result;
    }

    private Map<Long, List<AdAttributeValue>> groupByDefinition(List<AdAttributeValue> rows) {
        Map<Long, List<AdAttributeValue>> byDefinition = new LinkedHashMap<>();
        for (AdAttributeValue row : rows) {
            byDefinition.computeIfAbsent(row.getAttributeDefinition().getId(), k -> new ArrayList<>()).add(row);
        }
        return byDefinition;
    }

    private AdAttributeResponse toResponse(List<AdAttributeValue> group, List<AttributeOption> defOptions) {
        AttributeDefinition definition = group.get(0).getAttributeDefinition();
        Map<String, String> labelsByValue = defOptions.stream()
                .collect(Collectors.toMap(AttributeOption::getValue, AttributeOption::getLabel, (a, b) -> a));

        String value;
        String displayValue;
        switch (definition.getDataType()) {
            case NUMBER, DECIMAL -> {
                BigDecimal number = group.get(0).getValueNumber();
                value = number.stripTrailingZeros().toPlainString();
                displayValue = value;
            }
            case BOOLEAN -> {
                Boolean bool = group.get(0).getValueBoolean();
                value = String.valueOf(bool);
                displayValue = Boolean.TRUE.equals(bool) ? "Yes" : "No";
            }
            case SELECT -> {
                String raw = group.get(0).getValueText();
                value = raw;
                displayValue = labelsByValue.getOrDefault(raw, raw);
            }
            case MULTI_SELECT -> {
                value = group.stream().map(AdAttributeValue::getValueText).collect(Collectors.joining(","));
                displayValue = group.stream()
                        .map(v -> labelsByValue.getOrDefault(v.getValueText(), v.getValueText()))
                        .collect(Collectors.joining(", "));
            }
            default -> {
                value = group.get(0).getValueText();
                displayValue = value;
            }
        }

        return new AdAttributeResponse(definition.getKey(), definition.getName(), definition.getDataType(), value, displayValue, definition.getUnit());
    }

    private List<AdAttributeValue> buildValues(Ad ad, Map<String, String> raw) {
        Category category = ad.getCategory();
        List<AttributeDefinition> defs = definitions.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscIdAsc(category.getId());

        Set<String> knownKeys = defs.stream().map(AttributeDefinition::getKey).collect(Collectors.toSet());
        for (String key : raw.keySet()) {
            if (!knownKeys.contains(key)) {
                throw new BadRequestException("Unknown attribute: " + key);
            }
        }

        List<Long> defIds = defs.stream().map(AttributeDefinition::getId).toList();
        Map<Long, List<AttributeOption>> optionsByDefinition = options.findByAttributeDefinitionIdInAndActiveTrue(defIds)
                .stream().collect(Collectors.groupingBy(o -> o.getAttributeDefinition().getId()));

        List<AdAttributeValue> built = new ArrayList<>();
        for (AttributeDefinition def : defs) {
            String rawValue = raw.get(def.getKey());
            if (rawValue != null) {
                rawValue = rawValue.trim();
                if (rawValue.isEmpty()) {
                    rawValue = null;
                }
            }

            if (rawValue == null) {
                if (def.isRequired()) {
                    throw new BadRequestException(def.getName() + " is required");
                }
                continue;
            }

            built.addAll(buildForDefinition(ad, def, rawValue, optionsByDefinition.getOrDefault(def.getId(), List.of())));
        }
        return built;
    }

    private List<AdAttributeValue> buildForDefinition(Ad ad, AttributeDefinition def, String rawValue, List<AttributeOption> defOptions) {
        return switch (def.getDataType()) {
            case TEXT -> {
                if (rawValue.length() > 255) {
                    throw new BadRequestException(def.getName() + " must be 255 characters or fewer");
                }
                yield List.of(new AdAttributeValue(ad, def, rawValue, null, null));
            }
            case NUMBER -> {
                BigDecimal number = parseNumber(def, rawValue);
                if (number.stripTrailingZeros().scale() > 0) {
                    throw new BadRequestException(def.getName() + " must be a whole number");
                }
                yield List.of(new AdAttributeValue(ad, def, null, number, null));
            }
            case DECIMAL -> List.of(new AdAttributeValue(ad, def, null, parseNumber(def, rawValue), null));
            case BOOLEAN -> {
                if (!rawValue.equalsIgnoreCase("true") && !rawValue.equalsIgnoreCase("false")) {
                    throw new BadRequestException(def.getName() + " must be true or false");
                }
                yield List.of(new AdAttributeValue(ad, def, null, null, Boolean.parseBoolean(rawValue)));
            }
            case SELECT -> List.of(new AdAttributeValue(ad, def, matchOption(def, rawValue, defOptions), null, null));
            case MULTI_SELECT -> {
                // LinkedHashSet of the *matched* canonical value (not the raw input) so
                // "English,english" or "English,ENGLISH,English" all collapse to one stored row,
                // while still preserving first-seen order.
                Set<String> matched = new java.util.LinkedHashSet<>();
                for (String v : rawValue.split(",")) {
                    String trimmed = v.trim();
                    if (!trimmed.isEmpty()) {
                        matched.add(matchOption(def, trimmed, defOptions));
                    }
                }
                yield matched.stream().map(v -> new AdAttributeValue(ad, def, v, null, null)).toList();
            }
        };
    }

    private BigDecimal parseNumber(AttributeDefinition def, String rawValue) {
        try {
            return new BigDecimal(rawValue);
        } catch (NumberFormatException e) {
            throw new BadRequestException(def.getName() + " must be a number");
        }
    }

    private String matchOption(AttributeDefinition def, String rawValue, List<AttributeOption> defOptions) {
        return defOptions.stream()
                .filter(o -> o.getValue().equalsIgnoreCase(rawValue))
                .map(AttributeOption::getValue)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(def.getName() + " must be one of "
                        + defOptions.stream().map(AttributeOption::getValue).collect(Collectors.joining(", "))));
    }
}
