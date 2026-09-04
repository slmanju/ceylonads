package com.slmanju.ceylonads.promotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers /api/admin/tuition/promotion-plans/** (see AdminTuitionPromotionPlanController):
// ADMIN-only, always scoped to SourceChannel.TUITION via PromotionPlanService's channel-scoped
// overloads.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdminTuitionPromotionPlanControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void adminCanCreateEditCloseAndReactivateATuitionPlan() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long slotId = firstTuitionSlotId(adminToken);
        String code = "TEST_TUITION_PLAN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        String createResponse = mockMvc.perform(post("/api/admin/tuition/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code, "name", "Test Plan", "description", "A test plan.",
                                "slotId", slotId, "durationDays", 30, "price", 1500))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long planId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/admin/tuition/promotion-plans/" + planId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Plan Updated", "description", "Updated.",
                                "price", 1800, "durationDays", 30, "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Plan Updated"))
                .andExpect(jsonPath("$.price").value(1800));

        mockMvc.perform(patch("/api/admin/tuition/promotion-plans/" + planId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Closed plan is absent from the public Tuition catalog...
        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.id == " + planId + ")]").doesNotExist());
        // ...absent from the default (scope=CURRENT) admin list, since it's no longer active...
        mockMvc.perform(get("/api/admin/tuition/promotion-plans").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + planId + ")]").doesNotExist());
        // ...but still visible under scope=HISTORICAL and scope=ALL for audit.
        mockMvc.perform(get("/api/admin/tuition/promotion-plans").param("scope", "HISTORICAL").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + planId + ")]").exists());
        mockMvc.perform(get("/api/admin/tuition/promotion-plans").param("scope", "ALL").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + planId + ")]").exists());

        mockMvc.perform(patch("/api/admin/tuition/promotion-plans/" + planId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Reactivated on a current-catalog slot: back in the default scope=CURRENT list.
        mockMvc.perform(get("/api/admin/tuition/promotion-plans").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + planId + ")]").exists());
    }

    @Test
    void duplicateCodeInvalidPriceAndInvalidDurationAreRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long slotId = firstTuitionSlotId(adminToken);

        mockMvc.perform(post("/api/admin/tuition/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "TUITION_SEARCH_TOP_30D", "name", "Dup", "description", "Dup code.",
                                "slotId", slotId, "durationDays", 30, "price", 1000))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/tuition/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "TEST_BAD_PRICE_" + UUID.randomUUID(), "name", "Bad Price", "description", "x",
                                "slotId", slotId, "durationDays", 30, "price", -100))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/tuition/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "TEST_BAD_DURATION_" + UUID.randomUUID(), "name", "Bad Duration", "description", "x",
                                "slotId", slotId, "durationDays", 0, "price", 1000))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void defaultPlanListIsExactlyTheCurrentTuitionCatalog() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Set<String> codes = new HashSet<>();
        for (var plan : objectMapper.readTree(response)) {
            codes.add(plan.get("code").asText());
        }

        Set<String> expectedCurrent = Set.of(
                "TUITION_SEARCH_TOP_30D", "TUITION_SEARCH_BOOST_30D", "TUITION_SEARCH_SIDEBAR_TOP_30D",
                "TUITION_HOME_FEATURED_30D", "TUITION_HOME_LATEST_RIGHT_30D", "TUITION_DETAIL_TOP_30D",
                "TUITION_DETAIL_RIGHT_30D");
        assertTrue(codes.containsAll(expectedCurrent), "expected all 7 current-catalog codes, got " + codes);
        // Legacy/retired products - some still active=true in the shared tables - must never leak
        // into the default (scope=CURRENT) list.
        assertFalse(codes.contains("TUITION_SEARCH_TOP_BANNER_7D"), "legacy active banner plan leaked into current list");
        assertFalse(codes.contains("TUITION_FEATURED_7D"));
        assertFalse(codes.contains("TUITION_SEARCH_SIDEBAR_TOP_7D"));
        assertFalse(codes.contains("TUITION_SEARCH_SIDEBAR_MIDDLE_7D"));
        assertFalse(codes.contains("TUITION_SEARCH_SIDEBAR_BOTTOM_7D"));
        assertFalse(codes.contains("TUITION_DETAIL_TOP_CAROUSEL_7D"));

        // scope=HISTORICAL is exactly the complement - the retired banner plan shows up there.
        String historicalResponse = mockMvc.perform(get("/api/admin/tuition/promotion-plans")
                        .param("scope", "HISTORICAL").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boolean bannerInHistorical = false;
        for (var plan : objectMapper.readTree(historicalResponse)) {
            if (plan.get("code").asText().equals("TUITION_SEARCH_TOP_BANNER_7D")) {
                bannerInHistorical = true;
            }
        }
        assertTrue(bannerInHistorical, "retired-but-active banner plan should appear under scope=HISTORICAL");
    }

    @Test
    void currentSlotsPickerExcludesRetiredAndTestSlots() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans/slots").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Set<String> slotCodes = new HashSet<>();
        for (var slot : objectMapper.readTree(response)) {
            slotCodes.add(slot.get("code").asText());
        }
        assertTrue(slotCodes.contains("TUITION_SEARCH_TOP"));
        assertTrue(slotCodes.contains("TUITION_SEARCH_BOOST"));
        assertFalse(slotCodes.contains("TUITION_SEARCH_TOP_BANNER"));
        assertFalse(slotCodes.contains("TUITION_SEARCH_SIDEBAR_MIDDLE"));
        assertFalse(slotCodes.contains("TUITION_SEARCH_SIDEBAR_BOTTOM"));
    }

    @Test
    void mainSiteSlotCannotBeUsedToCreateATuitionPlan() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long mainSiteSlotId = mainSiteSlotId(adminToken);

        mockMvc.perform(post("/api/admin/tuition/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "TEST_CROSS_CHANNEL_" + UUID.randomUUID(), "name", "Cross Channel", "description", "x",
                                "slotId", mainSiteSlotId, "durationDays", 30, "price", 1000))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotReachTuitionPlanAdminEndpoints() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        mockMvc.perform(get("/api/admin/tuition/promotion-plans").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
    }

    private long firstTuitionSlotId(String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans/slots").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var slots = objectMapper.readTree(response);
        if (slots.isEmpty()) {
            throw new IllegalStateException("No TUITION promotion slots found");
        }
        return slots.get(0).get("id").asLong();
    }

    private long mainSiteSlotId(String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/admin/promotion-slots").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var slots = objectMapper.readTree(response);
        for (var slot : slots) {
            if (slot.get("sourceChannel").asText().equals("MAIN_SITE")) {
                return slot.get("id").asLong();
            }
        }
        throw new IllegalStateException("No MAIN_SITE promotion slot found");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
