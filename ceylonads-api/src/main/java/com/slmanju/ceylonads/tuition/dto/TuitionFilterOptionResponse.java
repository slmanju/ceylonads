package com.slmanju.ceylonads.tuition.dto;

// Stable stored value (e.g. "national") plus its resolved display label (e.g. "Sri Lankan
// National") for a single tuition filter option.
public record TuitionFilterOptionResponse(
        String value,
        String label) {
}
