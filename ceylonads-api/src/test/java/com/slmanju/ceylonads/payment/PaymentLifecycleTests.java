package com.slmanju.ceylonads.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class PaymentLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void promotingAnAdAutomaticallyCreatesAPendingPaymentWithTheSnapshottedAmount() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Payment Amount Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");

        long promotionId = createPromotion(kamalToken, adId, planId);

        String response = mockMvc.perform(get("/api/payments/me").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode payment = findByPromotionId(response, promotionId);

        assertPaymentReference(payment.get("paymentReference").asText());
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", payment.get("status").asText());
        org.junit.jupiter.api.Assertions.assertEquals(750.0, payment.get("amount").asDouble());
        org.junit.jupiter.api.Assertions.assertEquals("BANK_TRANSFER", payment.get("paymentMethod").asText());
    }

    @Test
    void customerCanOnlySeeTheirOwnPayment() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String nimalToken = loginAndGetToken("nimal", "customer123");
        long adId = createApprovedAd(kamalToken, "Ownership Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        mockMvc.perform(get("/api/payments/" + paymentId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments/" + paymentId).header("Authorization", "Bearer " + nimalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotSubmitAnotherCustomersPayment() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String nimalToken = loginAndGetToken("nimal", "customer123");
        long adId = createApprovedAd(kamalToken, "Cross Submit Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + nimalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", "FT999"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitRequiresAReceiptEvenWithABankReference() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "No Receipt Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", "FT123456789"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentMovesFromPendingToSubmittedOnceReceiptAndReferenceArePresent() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Submit Flow Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        uploadReceipt(kamalToken, paymentId);

        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", "FT123456789", "customerNote", "Paid via Commercial Bank"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.bankReference").value("FT123456789"))
                .andExpect(jsonPath("$.receiptUrl").isNotEmpty())
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());
    }

    @Test
    void adminApprovalActivatesThePromotionAndPopulatesItsDates() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Approval Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        uploadReceipt(kamalToken, paymentId);
        submitPayment(kamalToken, paymentId, "FT-APPROVE-1");

        // Promotion stays PENDING_PAYMENT while the payment is only SUBMITTED.
        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        // A customer cannot approve their own payment.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty());
    }

    @Test
    void pendingPaymentsCanBeApprovedDirectlyButApprovedPaymentsCannotBeEditedOrResubmitted() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Reapprove Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        // PENDING (no proof, never submitted) can still be approved directly - this is the
        // manual/offline settlement path (cash, phone-arranged, etc). Proof is supporting
        // evidence, not a precondition for verification.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Already-approved payments are final: no re-approval, no receipt replacement, no resubmit.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", "FT-AFTER-APPROVAL"))))
                .andExpect(status().isBadRequest());

        MockMultipartFile file = new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/api/payments/" + paymentId + "/receipt")
                        .file(file)
                        .header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanManuallyVerifyAPendingPaymentWithNoProofRecordingMethodAndNote() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Manual Settlement Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        // No receipt uploaded - payment stays PENDING with no proof of its own.
        mockMvc.perform(get("/api/payments/" + paymentId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.receiptUrl").doesNotExist());

        // A customer cannot verify their own payment.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());

        // Admin manually verifies the cash payment even though no proof was ever uploaded,
        // recording how and why it was settled.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("paymentMethod", "CASH", "adminNote", "Cash received at office"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.adminNote").value("Cash received at office"));

        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty());
    }

    @Test
    void uploadingAReceiptDoesNotByItselfVerifyThePayment() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Proof Not Verification Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        uploadReceipt(kamalToken, paymentId);

        // Proof was uploaded but the customer never submitted it for review, so it is still
        // PENDING - the admin has not checked it, so it is not verified.
        mockMvc.perform(get("/api/payments/" + paymentId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.receiptUrl").isNotEmpty());
    }

    @Test
    void approvalRollsBackWhenThePromotionCanNoLongerBeActivated() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Rollback Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        uploadReceipt(kamalToken, paymentId);
        submitPayment(kamalToken, paymentId, "FT-ROLLBACK-1");

        // The ad is deactivated after the customer submitted proof but before admin review.
        mockMvc.perform(patch("/api/admin/ads/" + adId + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Neither side of the transaction should have moved.
        mockMvc.perform(get("/api/admin/payments/" + paymentId).header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void rejectionRequiresAReasonAndTheRejectedPaymentCanBeResubmitted() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Reject Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        uploadReceipt(kamalToken, paymentId);
        submitPayment(kamalToken, paymentId, "FT-REJECT-1");

        // A customer cannot reject a payment.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/reject")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("reason", "n/a"))))
                .andExpect(status().isForbidden());

        // A reason is required.
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("reason", ""))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Receipt is unclear."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.adminNote").value("Receipt is unclear."));

        // Promotion is untouched by a rejection.
        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        // Rejected -> resubmit with a fresh receipt succeeds.
        uploadReceipt(kamalToken, paymentId);
        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", "FT-REJECT-RESUBMIT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void cancellingAPendingPaymentAlsoCancelsItsPromotion() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Cancel Ad " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        long promotionId = createPromotion(kamalToken, adId, planId);
        long paymentId = paymentIdForPromotion(kamalToken, promotionId);

        mockMvc.perform(post("/api/payments/" + paymentId + "/cancel").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void bankTransferDetailsAreAvailableToAnyAuthenticatedCustomer() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(get("/api/payments/bank-transfer-details").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").isNotEmpty())
                .andExpect(jsonPath("$.accountNumber").isNotEmpty());

        mockMvc.perform(get("/api/payments/bank-transfer-details"))
                .andExpect(status().isUnauthorized());
    }

    private void assertPaymentReference(String reference) {
        org.junit.jupiter.api.Assertions.assertTrue(reference.matches("CA-PAY-\\d{4}-\\d{6}"), "Unexpected reference format: " + reference);
    }

    private void uploadReceipt(String token, long paymentId) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/api/payments/" + paymentId + "/receipt")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void submitPayment(String token, long paymentId, String bankReference) throws Exception {
        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", bankReference))))
                .andExpect(status().isOk());
    }

    private long paymentIdForPromotion(String token, long promotionId) throws Exception {
        String response = mockMvc.perform(get("/api/payments/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return findByPromotionId(response, promotionId).get("id").asLong();
    }

    private JsonNode findByPromotionId(String listJson, long promotionId) throws Exception {
        for (JsonNode node : objectMapper.readTree(listJson)) {
            if (node.get("promotionId").asLong() == promotionId) {
                return node;
            }
        }
        throw new IllegalStateException("No payment found for promotion " + promotionId);
    }

    private long createPromotion(String token, long adId, long planId) throws Exception {
        String response = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long planIdByCode(String token, String code) throws Exception {
        String response = mockMvc.perform(get("/api/promotion-plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("code").asText().equals(code)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seed plan not found: " + code);
    }

    private long createApprovedAd(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "vehicles",
                "locationSlug", "colombo"));
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        return id;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
