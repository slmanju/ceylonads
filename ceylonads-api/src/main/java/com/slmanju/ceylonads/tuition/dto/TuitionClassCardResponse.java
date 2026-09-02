package com.slmanju.ceylonads.tuition.dto;

import com.slmanju.ceylonads.location.dto.LocationResponse;

import java.math.BigDecimal;
import java.util.List;

// Lightweight card shape for the Similar Classes rail: no description, no full media list, no
// seller object - just enough to render a card and link to the detail page.
public record TuitionClassCardResponse(
        Long id,
        String slug,
        String title,
        BigDecimal price,
        String primaryImageUrl,
        LocationResponse primaryLocation,
        String subject,
        String level,
        AttributeValueLabel curriculum,
        List<AttributeValueLabel> medium) {
}
