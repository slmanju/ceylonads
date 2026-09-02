package com.slmanju.ceylonads.payment.controller;

import com.slmanju.ceylonads.payment.dto.PaymentCountResponse;
import com.slmanju.ceylonads.payment.dto.PaymentResponse;
import com.slmanju.ceylonads.payment.dto.PaymentSummaryResponse;
import com.slmanju.ceylonads.payment.dto.RejectPaymentRequest;
import com.slmanju.ceylonads.payment.dto.VerifyPaymentRequest;
import com.slmanju.ceylonads.payment.entity.PaymentStatus;
import com.slmanju.ceylonads.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is restricted to ROLE_ADMIN centrally in SecurityConfig.
@RestController
@RequestMapping("/api/admin/payments")
@SecurityRequirement(name = "bearerAuth")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "List payments, optionally filtered by status")
    List<PaymentSummaryResponse> list(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) PaymentStatus status) {
        return paymentService.adminList(status);
    }

    @GetMapping("/count")
    @Operation(summary = "Count payments in a given status, e.g. for a dashboard 'awaiting review' card")
    PaymentCountResponse count(@RequestParam PaymentStatus status) {
        return new PaymentCountResponse(paymentService.countByStatus(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View one payment, with enough detail to verify the transfer")
    PaymentResponse get(@PathVariable Long id) {
        return paymentService.adminGet(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending or submitted payment; activates the related promotion in the same "
            + "transaction. An optional payment method/note records how a payment with no uploaded proof "
            + "(cash, phone-arranged, etc.) was manually verified.")
    PaymentResponse approve(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) VerifyPaymentRequest request) {
        return paymentService.approve(id, authentication.getName(), request);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a submitted payment with a reason; the promotion stays pending payment so the customer can resubmit")
    PaymentResponse reject(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectPaymentRequest request) {
        return paymentService.reject(id, authentication.getName(), request);
    }
}
