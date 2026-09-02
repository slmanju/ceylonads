package com.slmanju.ceylonads.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 30) String phone) {
}
