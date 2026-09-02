package com.slmanju.ceylonads.payment.controller;

import com.slmanju.ceylonads.payment.dto.BankTransferDetailsResponse;
import com.slmanju.ceylonads.payment.dto.PaymentResponse;
import com.slmanju.ceylonads.payment.dto.PaymentSummaryResponse;
import com.slmanju.ceylonads.payment.dto.SubmitPaymentRequest;
import com.slmanju.ceylonads.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/bank-transfer-details")
    @Operation(summary = "Bank account details and instructions for completing a manual bank transfer")
    BankTransferDetailsResponse bankTransferDetails() {
        return paymentService.bankTransferDetails();
    }

    @GetMapping("/me")
    @Operation(summary = "List my payments")
    List<PaymentSummaryResponse> mine(Authentication authentication) {
        return paymentService.mine(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "View one of my payments")
    PaymentResponse get(Authentication authentication, @PathVariable Long id) {
        return paymentService.getOwned(id, authentication.getName());
    }

    @PostMapping("/{id}/receipt")
    @Operation(summary = "Upload (or replace) the bank transfer receipt for one of my payments; "
            + "only allowed before the payment is submitted for review")
    PaymentResponse uploadReceipt(
            Authentication authentication,
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) throws IOException {
        return paymentService.uploadReceipt(id, authentication.getName(), file);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit my bank reference and receipt for admin review")
    PaymentResponse submit(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SubmitPaymentRequest request) {
        return paymentService.submit(id, authentication.getName(), request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of my payments before it is submitted; also cancels its pending promotion")
    PaymentResponse cancel(Authentication authentication, @PathVariable Long id) {
        return paymentService.cancelOwned(id, authentication.getName());
    }
}
