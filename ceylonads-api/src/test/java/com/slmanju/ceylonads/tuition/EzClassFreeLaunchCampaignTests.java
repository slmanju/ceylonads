package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.payment.repository.PaymentRepository;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the EZCLASS_LAUNCH_FREE campaign seeded by V27__ezclass_free_launch_campaign.sql: it is
 * the real, live-by-default TUITION launch offer (100% off, Rs. 0 floor) covering all seven Tuition
 * promotion products for [starts_at, ends_at). Verifies campaign-window resolution via
 * GET /api/tuition/promotions/campaign and GET /api/tuition/promotions/plans, and that a customer
 * request for a plan while it is active skips payment entirely (Rs. 0 charge, no Payment row) but
 * still requires admin approval before activating - FREE only zeroes the price, it never bypasses
 * moderation (see PromotionService#resolveCreationPlan). Also covers the admin approval step
 * itself, and contrasts customer-request lifecycle against the admin "Promote Class" direct-activate
 * path under both free and normal paid pricing.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class EzClassFreeLaunchCampaignTests {

    private static final Set<String> FREE_PLAN_CODES = Set.of(
            "TUITION_SEARCH_TOP_30D", "TUITION_SEARCH_BOOST_30D", "TUITION_SEARCH_SIDEBAR_TOP_30D",
            "TUITION_HOME_FEATURED_30D", "TUITION_DETAIL_TOP_30D", "TUITION_HOME_LATEST_RIGHT_30D",
            "TUITION_DETAIL_RIGHT_30D");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private PromotionCampaignRepository promotionCampaignRepository;

    @Autowired
    private AdRepository ads;

    @Autowired
    private PaymentRepository payments;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void isActiveByDefaultAndReturnedWithFreeLaunchCopy() throws Exception {
        mockMvc.perform(get("/api/tuition/promotions/campaign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EZCLASS_LAUNCH_FREE"))
                .andExpect(jsonPath("$.headline").value("Promote your class for FREE"))
                .andExpect(jsonPath("$.message")
                        .value("All eligible ezClass promotion placements are free during our launch period."))
                .andExpect(jsonPath("$.ctaLabel").value("Promote Your Class"))
                .andExpect(jsonPath("$.showBanner").value(true))
                .andExpect(jsonPath("$.showModal").value(true));
    }

    @Test
    void allSevenTuitionProductsResolveToFreeUnderTheDefaultLaunchCampaign() throws Exception {
        String response = mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Set<String> seenFreeCodes = new java.util.HashSet<>();
        for (JsonNode node : objectMapper.readTree(response)) {
            JsonNode plan = node.get("plan");
            String code = plan.get("code").asText();
            if (!FREE_PLAN_CODES.contains(code)) {
                continue;
            }
            seenFreeCodes.add(code);
            assertEquals(0, BigDecimal.ZERO.compareTo(new BigDecimal(plan.get("currentPrice").asText())),
                    () -> code + " must be free under EZCLASS_LAUNCH_FREE");
            assertTrue(plan.get("discounted").asBoolean(), () -> code + " must be flagged discounted");
            assertTrue(plan.get("price").decimalValue().compareTo(BigDecimal.ZERO) > 0,
                    () -> code + " base price must remain unchanged and positive");
        }
        assertEquals(FREE_PLAN_CODES, seenFreeCodes, "all seven Tuition products must resolve as free");
    }

    @Test
    @Transactional
    void campaignBeforeItsStartWindowIsNotReturned() throws Exception {
        Instant now = Instant.now();
        updateLaunchCampaignWindow(now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void campaignAfterItsEndWindowIsNotReturned() throws Exception {
        Instant now = Instant.now();
        updateLaunchCampaignWindow(now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    void purchasingAPlanDuringTheFreeLaunchStillRequiresAdminApprovalWithZeroChargeAndNoPayment() throws Exception {
        String token = registerAndGetToken();
        long adId = createApprovedTuitionClass(token, "Free Launch Purchase " + UUID.randomUUID());

        long planId = compatiblePlanIdByCode(token, adId, "TUITION_SEARCH_BOOST_30D");
        String createResponse = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);

        // FREE means chargedPrice = 0, not an implicit admin approval - a customer-initiated
        // request still requires moderation exactly like a paid one would (see
        // PromotionService#resolveCreationPlan; this is the production bug this test now guards).
        assertEquals("PENDING_APPROVAL", created.get("status").asText(), "a free promotion still requires admin approval");
        assertEquals(0, BigDecimal.ZERO.compareTo(new BigDecimal(created.get("price").asText())));
        assertEquals("TUITION_SEARCH_BOOST_30D", created.get("promotionPlanCode").asText());
        assertEquals("TUITION_SEARCH_BOOST", created.get("slotCode").asText());
        assertEquals(30, created.get("durationDays").asInt());
        assertFalse(created.get("paymentWaived").asBoolean(), "genuinely free, not an admin-waived payment");
        assertTrue(created.get("startsAt").isNull(), "the paid period must not start before admin approval");
        assertTrue(created.get("endsAt").isNull(), "the paid period must not start before admin approval");

        long promotionId = created.get("id").asLong();
        assertTrue(payments.findAllByOrderByCreatedAtDesc().stream()
                        .noneMatch(p -> p.getPromotion().getId().equals(promotionId)),
                "a free promotion must never create a Payment row");
    }

    @Test
    void adminApprovingAFreeLaunchPromotionActivatesItFromTheApprovalTimeAndExtendsClassExpiry() throws Exception {
        String token = registerAndGetToken();
        long adId = createApprovedTuitionClass(token, "Free Launch Approval " + UUID.randomUUID());
        long planId = compatiblePlanIdByCode(token, adId, "TUITION_SEARCH_BOOST_30D");

        String createResponse = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(createResponse).get("id").asLong();

        String adminToken = loginAndGetToken("admin", "admin123");
        String approveResponse = mockMvc.perform(patch("/api/admin/tuition/promotions/" + promotionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode approved = objectMapper.readTree(approveResponse);
        // The price snapshotted at request time must survive approval unchanged - approval never
        // recharges or re-resolves pricing.
        assertEquals(0, BigDecimal.ZERO.compareTo(new BigDecimal(approved.get("price").asText())));

        Instant endsAt = Instant.parse(approved.get("endsAt").asText());
        Ad ad = ads.findById(adId).orElseThrow();
        assertFalse(ad.getExpiresAt().isBefore(endsAt), "ad expiry must cover the full paid-duration guarantee once approved");
    }

    @Test
    @Transactional
    void customerRequestUnderNormalPaidPricingAlsoRequiresApprovalNotJustPayment() throws Exception {
        Instant now = Instant.now();
        updateLaunchCampaignWindow(now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));

        String token = registerAndGetToken();
        long adId = createApprovedTuitionClass(token, "Paid Pricing Purchase " + UUID.randomUUID());
        long planId = compatiblePlanIdByCode(token, adId, "TUITION_SEARCH_BOOST_30D");

        // Every current Tuition plan has paymentRequired=true AND approvalRequired=true (validated
        // at the plan level), so once the free campaign is out of its window, a real charge is due
        // first - PENDING_PAYMENT, not PENDING_APPROVAL. The approval step still happens, just via
        // the payment-confirmation path rather than a second explicit review (see
        // PromotionService#resolveCreationPlan).
        mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.price").value(2990.0))
                .andExpect(jsonPath("$.startsAt").doesNotExist())
                .andExpect(jsonPath("$.endsAt").doesNotExist());
    }

    @Test
    @Transactional
    void adminCreatedPromotionActivatesImmediatelyEvenUnderNormalPaidPricing() throws Exception {
        Instant now = Instant.now();
        updateLaunchCampaignWindow(now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));

        String token = registerAndGetToken();
        long adId = createApprovedTuitionClass(token, "Admin Promote Paid " + UUID.randomUUID());

        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_SEARCH_BOOST_30D");

        // The admin's own "Promote Class" action is itself the approval (createAdminPromotionForTuitionClass
        // bypasses resolveCreationPlan entirely), so it activates immediately regardless of price -
        // unlike the customer-request path above, which needed PENDING_PAYMENT under the same pricing.
        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.price").value(2990.0));
    }

    private long tuitionPlanIdByCode(String adminToken, String code) throws Exception {
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans")
                        .param("scope", "ALL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("code").asText().equals(code)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException(code + " plan not found");
    }

    // --- helpers --------------------------------------------------------------------------------

    private void updateLaunchCampaignWindow(Instant startsAt, Instant endsAt) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        PromotionCampaign launch = promotionCampaignRepository.findByCode("EZCLASS_LAUNCH_FREE").orElseThrow();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", launch.getName());
        request.put("description", launch.getDescription());
        request.put("discountPercent", launch.getDiscountPercent());
        request.put("minimumPrice", launch.getMinimumPrice());
        request.put("startsAt", startsAt.toString());
        request.put("endsAt", endsAt.toString());
        request.put("active", true);
        request.put("planIds", launch.getPlans().stream().map(p -> p.getId()).toList());
        request.put("headline", launch.getHeadline());
        request.put("message", launch.getMessage());
        request.put("ctaLabel", launch.getCtaLabel());
        request.put("customerVisible", launch.isCustomerVisible());
        request.put("showBanner", launch.isShowBanner());
        request.put("showModal", launch.isShowModal());

        mockMvc.perform(put("/api/admin/promotion-campaigns/" + launch.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private long compatiblePlanIdByCode(String token, long adId, String code) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/my-classes/" + adId + "/promotion-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("plan").get("code").asText().equals(code)) {
                return node.get("plan").get("id").asLong();
            }
        }
        throw new IllegalStateException("Compatible plan not found: " + code);
    }

    private long createApprovedTuitionClass(String token, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "A/L");
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
        body.put("deliveryMode", "PHYSICAL");
        body.put("classFormat", "INDIVIDUAL");
        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        return id;
    }

    private String registerAndGetToken() throws Exception {
        String username = "ezclass_free_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Free Launch Test Tutor");
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
