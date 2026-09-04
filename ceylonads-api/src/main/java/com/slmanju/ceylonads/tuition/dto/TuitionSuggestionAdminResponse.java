package com.slmanju.ceylonads.tuition.dto;

import com.slmanju.ceylonads.tuition.entity.SuggestionStatus;

import java.time.Instant;

public record TuitionSuggestionAdminResponse(
        Long id,
        String name,
        String email,
        String phone,
        String message,
        SuggestionStatus status,
        Instant createdAt,
        Instant reviewedAt,
        Long reviewedByAccountId) {
}
