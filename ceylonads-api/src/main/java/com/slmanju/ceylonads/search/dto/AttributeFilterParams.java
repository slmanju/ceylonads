package com.slmanju.ceylonads.search.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses the "attr.<key>=value" / "attr.<key>.min=" / "attr.<key>.max=" query-param convention out
 * of the raw request parameter map into typed criteria. Malformed numbers are dropped rather than
 * failing the request - an unusable filter value should just not filter, not 500.
 */
public final class AttributeFilterParams {

    private static final String PREFIX = "attr.";

    private AttributeFilterParams() {
    }

    public static List<AttributeFilterCriterion> parse(Map<String, String> rawQueryParams) {
        if (rawQueryParams == null || rawQueryParams.isEmpty()) {
            return List.of();
        }

        Map<String, String> exact = new LinkedHashMap<>();
        Map<String, BigDecimal> mins = new LinkedHashMap<>();
        Map<String, BigDecimal> maxs = new LinkedHashMap<>();
        Set<String> order = new LinkedHashSet<>();

        for (Map.Entry<String, String> entry : rawQueryParams.entrySet()) {
            String param = entry.getKey();
            String value = entry.getValue();
            if (param == null || !param.startsWith(PREFIX) || value == null || value.isBlank()) {
                continue;
            }

            String rest = param.substring(PREFIX.length());
            if (rest.endsWith(".min")) {
                String key = rest.substring(0, rest.length() - 4);
                BigDecimal parsed = parseOrNull(value);
                if (parsed != null) {
                    mins.put(key, parsed);
                    order.add(key);
                }
            } else if (rest.endsWith(".max")) {
                String key = rest.substring(0, rest.length() - 4);
                BigDecimal parsed = parseOrNull(value);
                if (parsed != null) {
                    maxs.put(key, parsed);
                    order.add(key);
                }
            } else if (!rest.isBlank()) {
                exact.put(rest, value.trim());
                order.add(rest);
            }
        }

        List<AttributeFilterCriterion> criteria = new ArrayList<>();
        for (String key : order) {
            BigDecimal min = mins.get(key);
            BigDecimal max = maxs.get(key);
            if (min != null || max != null) {
                criteria.add(new AttributeFilterCriterion(key, null, min, max));
            } else if (exact.containsKey(key)) {
                criteria.add(new AttributeFilterCriterion(key, exact.get(key), null, null));
            }
        }
        return criteria;
    }

    private static BigDecimal parseOrNull(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
