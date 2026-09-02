package com.slmanju.ceylonads.admin.service;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.admin.dto.AdminAttributeDefinitionRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeDefinitionUpdateRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeOptionRequest;
import com.slmanju.ceylonads.admin.dto.AdminAttributeOptionUpdateRequest;
import com.slmanju.ceylonads.admin.dto.AdminCategoryRequest;
import com.slmanju.ceylonads.admin.dto.AdminLocationRequest;
import com.slmanju.ceylonads.auth.entity.AccountStatus;
import com.slmanju.ceylonads.category.dto.AttributeDefinitionResponse;
import com.slmanju.ceylonads.category.dto.AttributeOptionResponse;
import com.slmanju.ceylonads.category.dto.CategoryResponse;
import com.slmanju.ceylonads.category.service.AttributeDefinitionService;
import com.slmanju.ceylonads.category.service.CategoryService;
import com.slmanju.ceylonads.customer.dto.CustomerResponse;
import com.slmanju.ceylonads.customer.service.CustomerService;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final AdService adService;
    private final CustomerService customerService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final AttributeDefinitionService attributeDefinitionService;

    public AdminService(
            AdService adService,
            CustomerService customerService,
            CategoryService categoryService,
            LocationService locationService,
            AttributeDefinitionService attributeDefinitionService) {
        this.adService = adService;
        this.customerService = customerService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.attributeDefinitionService = attributeDefinitionService;
    }

    @Transactional(readOnly = true)
    public List<AdResponse> pendingAds() {
        return adService.pendingReview();
    }

    @Transactional
    public AdResponse approve(Long id, String reviewerUsername) {
        return adService.approve(id, reviewerUsername);
    }

    @Transactional
    public AdResponse reject(Long id, String reviewerUsername) {
        return adService.reject(id, reviewerUsername);
    }

    @Transactional
    public AdResponse deactivate(Long id) {
        return adService.adminDeactivate(id);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> customers() {
        return customerService.findAll();
    }

    @Transactional(readOnly = true)
    public List<AdResponse> activeAdsForCustomer(Long customerId) {
        return adService.activeByCustomerId(customerId);
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(Long id, AccountStatus status) {
        return customerService.updateStatus(id, status);
    }

    @Transactional
    public CategoryResponse createCategory(AdminCategoryRequest request) {
        return categoryService.create(request.name(), request.slug(), request.parentSlug(), request.displayOrder());
    }

    @Transactional
    public LocationResponse createLocation(AdminLocationRequest request) {
        return locationService.create(request.name(), request.slug(), request.type(), request.parentSlug());
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> categoryAttributes(Long categoryId) {
        return attributeDefinitionService.findAllByCategory(categoryId);
    }

    @Transactional
    public AttributeDefinitionResponse createCategoryAttribute(Long categoryId, AdminAttributeDefinitionRequest request) {
        return attributeDefinitionService.create(categoryId, request);
    }

    @Transactional
    public AttributeDefinitionResponse updateCategoryAttribute(Long categoryId, Long attributeId, AdminAttributeDefinitionUpdateRequest request) {
        return attributeDefinitionService.update(categoryId, attributeId, request);
    }

    @Transactional
    public AttributeDefinitionResponse setCategoryAttributeActive(Long categoryId, Long attributeId, boolean active) {
        return attributeDefinitionService.setActive(categoryId, attributeId, active);
    }

    @Transactional
    public AttributeOptionResponse addCategoryAttributeOption(Long categoryId, Long attributeId, AdminAttributeOptionRequest request) {
        return attributeDefinitionService.addOption(categoryId, attributeId, request);
    }

    @Transactional
    public AttributeOptionResponse updateCategoryAttributeOption(Long categoryId, Long attributeId, Long optionId, AdminAttributeOptionUpdateRequest request) {
        return attributeDefinitionService.updateOption(categoryId, attributeId, optionId, request);
    }

    @Transactional
    public AttributeOptionResponse setCategoryAttributeOptionActive(Long categoryId, Long attributeId, Long optionId, boolean active) {
        return attributeDefinitionService.setOptionActive(categoryId, attributeId, optionId, active);
    }
}
