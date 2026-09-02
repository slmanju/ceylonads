package com.slmanju.ceylonads.customer.dto;

import com.slmanju.ceylonads.auth.entity.AccountStatus;

public record CustomerResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String phone,
        AccountStatus status) {
}
