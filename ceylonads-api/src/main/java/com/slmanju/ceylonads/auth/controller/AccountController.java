package com.slmanju.ceylonads.auth.controller;

import com.slmanju.ceylonads.auth.dto.ChangePasswordRequest;
import com.slmanju.ceylonads.auth.dto.ChangePasswordResponse;
import com.slmanju.ceylonads.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/password")
    @Operation(summary = "Change the authenticated account's own password")
    ChangePasswordResponse changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return new ChangePasswordResponse("Password changed successfully");
    }
}
