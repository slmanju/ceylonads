package com.slmanju.ceylonads.search.dto;

import java.math.BigDecimal;

public record AttributeFilterCriterion(String key, String value, BigDecimal min, BigDecimal max) {

    public boolean isRange() {
        return min != null || max != null;
    }
}
