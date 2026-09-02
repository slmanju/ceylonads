package com.slmanju.ceylonads.customer.controller;

import com.slmanju.ceylonads.customer.dto.CustomerResponse;
import com.slmanju.ceylonads.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current customer profile")
    CustomerResponse me(Authentication authentication) {
        return customerService.me(authentication.getName());
    }
}
