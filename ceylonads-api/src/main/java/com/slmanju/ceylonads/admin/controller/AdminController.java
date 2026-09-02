package com.slmanju.ceylonads.admin.controller;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.admin.dto.AdminAttributeDefinitionRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeDefinitionUpdateRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeOptionRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeOptionUpdateRequest;
import com.slmanju.ceylonads.admin.dto.AdminCategoryRequest;
import com.slmanju.ceylonads.admin.dto.AdminLocationRequest;
import com.slmanju.ceylonads.admin.service.AdminService;
import com.slmanju.ceylonads.auth.entity.AccountStatus;
import com.slmanju.ceylonads.category.dto.AttributeDefinitionResponse;
import com.slmanju.ceylonads.category.dto.AttributeOptionResponse;
import com.slmanju.ceylonads.category.dto.CategoryResponse;
import com.slmanju.ceylonads.customer.dto.CustomerResponse;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Ad moderation is also reachable (for MODERATOR + ADMIN) under /api/moderation/ads/** - see
    // ModerationController. Both delegate to the same AdminService/AdService methods below, so
    // approval logic is never duplicated; this /api/admin/ads/** path stays for existing Admin UI
    // and test coverage.
    @GetMapping("/ads/pending")
    @Operation(summary = "List ads waiting for moderation")
    List<AdResponse> pendingAds() {
        return adminService.pendingAds();
    }

    @PatchMapping("/ads/{id}/approve")
    @Operation(summary = "Approve an ad")
    AdResponse approve(Authentication authentication, @PathVariable Long id) {
        return adminService.approve(id, authentication.getName());
    }

    @PatchMapping("/ads/{id}/reject")
    @Operation(summary = "Reject an ad")
    AdResponse reject(Authentication authentication, @PathVariable Long id) {
        return adminService.reject(id, authentication.getName());
    }

    @PatchMapping("/ads/{id}/deactivate")
    @Operation(summary = "Deactivate an ad")
    AdResponse deactivate(@PathVariable Long id) {
        return adminService.deactivate(id);
    }

    @GetMapping("/customers")
    @Operation(summary = "List all customers")
    List<CustomerResponse> customers() {
        return adminService.customers();
    }

    @PatchMapping("/customers/{id}/status")
    @Operation(summary = "Change customer account status")
    CustomerResponse updateStatus(@PathVariable Long id, @RequestParam AccountStatus status) {
        return adminService.updateCustomerStatus(id, status);
    }

    @GetMapping("/customers/{id}/ads")
    @Operation(summary = "List a customer's active ads",
            description = "Used when creating a promotion on a customer's behalf: only their ACTIVE ads are eligible.")
    List<AdResponse> customerAds(@PathVariable Long id) {
        return adminService.activeAdsForCustomer(id);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    CategoryResponse createCategory(@Valid @RequestBody AdminCategoryRequest request) {
        return adminService.createCategory(request);
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a location")
    LocationResponse createLocation(@Valid @RequestBody AdminLocationRequest request) {
        return adminService.createLocation(request);
    }

    @GetMapping("/categories/{categoryId}/attributes")
    @Operation(summary = "List all attribute definitions for a category, including inactive ones")
    List<AttributeDefinitionResponse> categoryAttributes(@PathVariable Long categoryId) {
        return adminService.categoryAttributes(categoryId);
    }

    @PostMapping("/categories/{categoryId}/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an attribute definition for a category")
    AttributeDefinitionResponse createCategoryAttribute(
            @PathVariable Long categoryId, @Valid @RequestBody AdminAttributeDefinitionRequest request) {
        return adminService.createCategoryAttribute(categoryId, request);
    }

    @PutMapping("/categories/{categoryId}/attributes/{attributeId}")
    @Operation(summary = "Update an attribute definition (key and data type are immutable)")
    AttributeDefinitionResponse updateCategoryAttribute(
            @PathVariable Long categoryId, @PathVariable Long attributeId,
            @Valid @RequestBody AdminAttributeDefinitionUpdateRequest request) {
        return adminService.updateCategoryAttribute(categoryId, attributeId, request);
    }

    @PatchMapping("/categories/{categoryId}/attributes/{attributeId}/active")
    @Operation(summary = "Activate or deactivate an attribute definition")
    AttributeDefinitionResponse setCategoryAttributeActive(
            @PathVariable Long categoryId, @PathVariable Long attributeId, @RequestParam boolean active) {
        return adminService.setCategoryAttributeActive(categoryId, attributeId, active);
    }

    @PostMapping("/categories/{categoryId}/attributes/{attributeId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a SELECT/MULTI_SELECT option to an attribute definition")
    AttributeOptionResponse addCategoryAttributeOption(
            @PathVariable Long categoryId, @PathVariable Long attributeId,
            @Valid @RequestBody AdminAttributeOptionRequest request) {
        return adminService.addCategoryAttributeOption(categoryId, attributeId, request);
    }

    @PutMapping("/categories/{categoryId}/attributes/{attributeId}/options/{optionId}")
    @Operation(summary = "Update an attribute option (value is immutable)")
    AttributeOptionResponse updateCategoryAttributeOption(
            @PathVariable Long categoryId, @PathVariable Long attributeId, @PathVariable Long optionId,
            @Valid @RequestBody AdminAttributeOptionUpdateRequest request) {
        return adminService.updateCategoryAttributeOption(categoryId, attributeId, optionId, request);
    }

    @PatchMapping("/categories/{categoryId}/attributes/{attributeId}/options/{optionId}/active")
    @Operation(summary = "Activate or deactivate an attribute option")
    AttributeOptionResponse setCategoryAttributeOptionActive(
            @PathVariable Long categoryId, @PathVariable Long attributeId, @PathVariable Long optionId,
            @RequestParam boolean active) {
        return adminService.setCategoryAttributeOptionActive(categoryId, attributeId, optionId, active);
    }
}
