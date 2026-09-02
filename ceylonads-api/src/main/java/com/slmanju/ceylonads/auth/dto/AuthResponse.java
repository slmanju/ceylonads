package com.slmanju.ceylonads.auth.dto;

import com.slmanju.ceylonads.auth.entity.Role;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String username,
        Role role) {
}
