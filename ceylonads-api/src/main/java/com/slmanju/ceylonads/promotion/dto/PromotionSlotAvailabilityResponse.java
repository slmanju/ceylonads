package com.slmanju.ceylonads.promotion.dto;

import java.time.Instant;

public record PromotionSlotAvailabilityResponse(
        Long slotId,
        boolean available,
        int capacity,
        int remainingCapacity,
        Instant requestedStart,
        Instant requestedEnd) {
}
