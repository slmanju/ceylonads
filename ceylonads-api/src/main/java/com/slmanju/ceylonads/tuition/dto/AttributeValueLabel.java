package com.slmanju.ceylonads.tuition.dto;

// Stable stored value (e.g. "national") plus its resolved display label (e.g. "Sri Lankan
// National"), for tuition SELECT/MULTI_SELECT attributes (curriculum, medium, classMode, classType).
public record AttributeValueLabel(
        String value,
        String label) {
}
