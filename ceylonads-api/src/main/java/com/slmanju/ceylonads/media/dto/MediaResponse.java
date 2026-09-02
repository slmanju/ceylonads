package com.slmanju.ceylonads.media.dto;

public record MediaResponse(
        Long id,
        String url,
        String contentType,
        int displayOrder) {
}
