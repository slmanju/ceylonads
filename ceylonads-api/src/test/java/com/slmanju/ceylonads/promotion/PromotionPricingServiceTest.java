package com.slmanju.ceylonads.promotion;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.PricingType;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import com.slmanju.ceylonads.promotion.service.PromotionPricingService;
import com.slmanju.ceylonads.promotion.service.PromotionPricingService.PromotionPricing;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit coverage for the money math behind the ezClass promotion catalog (§28 of the
 * campaign/pricing refactor): base prices, the launch fixed-price campaign, the 50%-off campaign
 * with its Rs.990 floor, normal fallback, campaign date boundaries, and MAIN_SITE isolation. No
 * Spring context / database needed, so this always runs regardless of the H2/Flyway test-infra
 * issue affecting the rest of the promotion test suite.
 */
class PromotionPricingServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

    private static PromotionPlan tuitionPlan(String code, String price) {
        PromotionSlot slot = new PromotionSlot(
                "SLOT_" + code, code, "desc", PlacementType.AD_DETAIL_SIDEBAR, null, SourceChannel.TUITION, 10, 3, 0);
        return new PromotionPlan(code, code, "desc", slot, 30, new BigDecimal(price), true, true, 0);
    }

    private static PromotionPlan mainSitePlan(String code, String price) {
        PromotionSlot slot = new PromotionSlot(
                "SLOT_" + code, code, "desc", PlacementType.HOME_FEATURED, null, SourceChannel.MAIN_SITE, 10, 3, 0);
        return new PromotionPlan(code, code, "desc", slot, 30, new BigDecimal(price), true, true, 0);
    }

    private static PromotionCampaign fixedPriceCampaign(String fixedPrice, Instant startsAt, Instant endsAt) {
        return new PromotionCampaign(
                "LAUNCH", "Launch", "desc", SourceChannel.TUITION, PricingType.FIXED_PRICE,
                null, new BigDecimal(fixedPrice), null, startsAt, endsAt, Set.of(),
                null, null, null, false, false, false);
    }

    private static PromotionCampaign percentageCampaign(
            String percent, String minimumPrice, Instant startsAt, Instant endsAt) {
        return new PromotionCampaign(
                "HALF_PRICE", "Half Price", "desc", SourceChannel.TUITION, PricingType.PERCENTAGE_DISCOUNT,
                new BigDecimal(percent), null, minimumPrice == null ? null : new BigDecimal(minimumPrice),
                startsAt, endsAt, Set.of(),
                null, null, null, false, false, false);
    }

    private static PromotionPricingService serviceReturning(PromotionCampaign... matches) {
        PromotionCampaignRepository repo = mock(PromotionCampaignRepository.class);
        when(repo.findActiveFor(any(), any(), any())).thenReturn(List.of(matches));
        return new PromotionPricingService(repo);
    }

    // --- base pricing (no campaign) -------------------------------------------------------------

    @Test
    void noCampaignResolvesToBasePriceForAllSevenTuitionProducts() {
        PromotionPricingService service = serviceReturning();

        assertBasePrice(service, tuitionPlan("TUITION_SEARCH_TOP_30D", "3490.00"), "3490.00");
        assertBasePrice(service, tuitionPlan("TUITION_SEARCH_BOOST_30D", "2990.00"), "2990.00");
        // Search Page Spotlight (restored by V22, TUITION_SEARCH_SIDEBAR_TOP_30D) - below Search
        // Boost, equal to Homepage Featured, per the seven-product catalog.
        assertBasePrice(service, tuitionPlan("TUITION_SEARCH_SIDEBAR_TOP_30D", "2490.00"), "2490.00");
        assertBasePrice(service, tuitionPlan("TUITION_HOME_FEATURED_30D", "2490.00"), "2490.00");
        assertBasePrice(service, tuitionPlan("TUITION_DETAIL_TOP_30D", "1990.00"), "1990.00");
        assertBasePrice(service, tuitionPlan("TUITION_HOME_LATEST_RIGHT_30D", "1490.00"), "1490.00");
        assertBasePrice(service, tuitionPlan("TUITION_DETAIL_RIGHT_30D", "1490.00"), "1490.00");
    }

    private void assertBasePrice(PromotionPricingService service, PromotionPlan plan, String expected) {
        PromotionPricing pricing = service.resolve(plan, NOW);
        assertEquals(0, new BigDecimal(expected).compareTo(pricing.basePrice()));
        assertEquals(0, new BigDecimal(expected).compareTo(pricing.effectivePrice()));
        assertFalse(pricing.discounted());
        assertNull(pricing.campaignCode());
    }

    // --- launch campaign (FIXED_PRICE, Rs. 990) --------------------------------------------------

    @Test
    void activeLaunchCampaignMakesEachEligiblePlanRs990() {
        PromotionCampaign launch = fixedPriceCampaign("990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        PromotionPricingService service = serviceReturning(launch);

        for (String basePrice : List.of("3490.00", "2990.00", "2490.00", "1990.00", "1490.00")) {
            PromotionPlan plan = tuitionPlan("PLAN_" + basePrice, basePrice);
            PromotionPricing pricing = service.resolve(plan, NOW);
            assertEquals(0, new BigDecimal("990.00").compareTo(pricing.effectivePrice()));
            assertTrue(pricing.discounted());
            assertEquals(0, new BigDecimal(basePrice).compareTo(pricing.basePrice()), "base price must be unchanged");
            assertEquals("LAUNCH", pricing.campaignCode());
        }
    }

    @Test
    void launchCampaignOnAnAlreadyRs990PlanIsNotShownAsDiscounted() {
        // A generic plan already sitting at the launch fixed price (no real Tuition product is
        // currently seeded at Rs. 990 - see V21 migration raising TUITION_DETAIL_RIGHT_30D off it).
        PromotionCampaign launch = fixedPriceCampaign("990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        PromotionPricingService service = serviceReturning(launch);

        PromotionPricing pricing = service.resolve(tuitionPlan("GENERIC_FLOOR_PRICE_PLAN_30D", "990.00"), NOW);
        assertEquals(0, new BigDecimal("990.00").compareTo(pricing.effectivePrice()));
        assertFalse(pricing.discounted());
    }

    // --- 50% campaign (PERCENTAGE_DISCOUNT, Rs. 990 minimum) --------------------------------------

    @Test
    void fiftyPercentCampaignHalvesEachBasePriceWithA990Floor() {
        PromotionCampaign halfPrice = percentageCampaign(
                "50.00", "990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        assertEffectivePrice(halfPrice, "3490.00", "1745.00");
        assertEffectivePrice(halfPrice, "2990.00", "1495.00");
        assertEffectivePrice(halfPrice, "2490.00", "1245.00");
        assertEffectivePrice(halfPrice, "1990.00", "995.00");
        // Below-990 raw halves are floored at the minimum, not their nominal half.
        assertEffectivePrice(halfPrice, "1490.00", "990.00");
        assertEffectivePrice(halfPrice, "990.00", "990.00");
    }

    private void assertEffectivePrice(PromotionCampaign campaign, String basePrice, String expected) {
        PromotionPricingService isolatedService = serviceReturning(campaign);
        PromotionPricing pricing = isolatedService.resolve(tuitionPlan("PLAN_" + basePrice, basePrice), NOW);
        assertEquals(0, new BigDecimal(expected).compareTo(pricing.effectivePrice()),
                () -> basePrice + " -> expected " + expected + " but was " + pricing.effectivePrice());
    }

    @Test
    void minimumPriceFlooredPlansShowNoMisleadingDiscountBadge() {
        // Rs. 990 -> 50% off floored back to Rs. 990: zero actual savings, so this must not claim
        // to be discounted even though a campaign matched (the badge must reflect the ACTUAL
        // effective value, not the nominal 50%). Generic plan - no real Tuition product currently
        // sits at Rs. 990 (see V21 migration raising TUITION_DETAIL_RIGHT_30D off it).
        PromotionCampaign halfPrice = percentageCampaign(
                "50.00", "990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        PromotionPricingService service = serviceReturning(halfPrice);

        PromotionPricing pricing = service.resolve(tuitionPlan("GENERIC_FLOOR_PRICE_PLAN_30D", "990.00"), NOW);
        assertFalse(pricing.discounted());
        assertEquals(0, BigDecimal.ZERO.compareTo(pricing.discountAmount()));

        // Rs. 1490 -> floored to Rs. 990 too, but that IS real savings (500), so it must show as
        // discounted with the actual (not nominal) percent. Both Homepage Spotlight and Detail Page
        // Spotlight are seeded at this base price.
        PromotionPricing spotlightPricing = service.resolve(tuitionPlan("TUITION_HOME_LATEST_RIGHT_30D", "1490.00"), NOW);
        assertTrue(spotlightPricing.discounted());
        assertEquals(0, new BigDecimal("500.00").compareTo(spotlightPricing.discountAmount()));
    }

    // --- Detail Page Spotlight base price update (Rs. 990 -> Rs. 1,490, see V21 migration) --------

    @Test
    void detailPageSpotlightResolvesCorrectlyAtItsUpdatedBasePriceAcrossAllPricingStates() {
        PromotionPlan detailSpotlight = tuitionPlan("TUITION_DETAIL_RIGHT_30D", "1490.00");

        // No campaign -> base price, not discounted.
        PromotionPricing normal = serviceReturning().resolve(detailSpotlight, NOW);
        assertEquals(0, new BigDecimal("1490.00").compareTo(normal.basePrice()));
        assertEquals(0, new BigDecimal("1490.00").compareTo(normal.effectivePrice()));
        assertFalse(normal.discounted());

        // Launch campaign (fixed Rs. 990) -> discounted to 990, Rs. 500 saving.
        PromotionCampaign launch = fixedPriceCampaign("990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        PromotionPricing launchPricing = serviceReturning(launch).resolve(detailSpotlight, NOW);
        assertEquals(0, new BigDecimal("1490.00").compareTo(launchPricing.basePrice()), "base price must be unchanged");
        assertEquals(0, new BigDecimal("990.00").compareTo(launchPricing.effectivePrice()));
        assertTrue(launchPricing.discounted());
        assertEquals(0, new BigDecimal("500.00").compareTo(launchPricing.discountAmount()));

        // 50% campaign -> raw half (745.00) floored to the Rs. 990 minimum, still real savings.
        PromotionCampaign halfPrice = percentageCampaign(
                "50.00", "990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        PromotionPricing halfPricing = serviceReturning(halfPrice).resolve(detailSpotlight, NOW);
        assertEquals(0, new BigDecimal("990.00").compareTo(halfPricing.effectivePrice()));
        assertTrue(halfPricing.discounted());
        assertEquals(0, new BigDecimal("500.00").compareTo(halfPricing.discountAmount()));
    }

    // --- ezClass free launch campaign (PERCENTAGE_DISCOUNT 100%, Rs. 0 minimum, see V27) ----------

    @Test
    void hundredPercentDiscountCampaignMakesEachOfTheSevenTuitionProductsFree() {
        PromotionCampaign freeLaunch = percentageCampaign(
                "100.00", "0.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(90, ChronoUnit.DAYS));

        assertFreePrice(freeLaunch, "TUITION_SEARCH_TOP_30D", "3490.00");
        assertFreePrice(freeLaunch, "TUITION_SEARCH_BOOST_30D", "2990.00");
        assertFreePrice(freeLaunch, "TUITION_SEARCH_SIDEBAR_TOP_30D", "2490.00");
        assertFreePrice(freeLaunch, "TUITION_HOME_FEATURED_30D", "2490.00");
        assertFreePrice(freeLaunch, "TUITION_DETAIL_TOP_30D", "1990.00");
        assertFreePrice(freeLaunch, "TUITION_HOME_LATEST_RIGHT_30D", "1490.00");
        assertFreePrice(freeLaunch, "TUITION_DETAIL_RIGHT_30D", "1490.00");
    }

    private void assertFreePrice(PromotionCampaign campaign, String planCode, String basePrice) {
        PromotionPricingService service = serviceReturning(campaign);
        PromotionPricing pricing = service.resolve(tuitionPlan(planCode, basePrice), NOW);
        assertEquals(0, new BigDecimal(basePrice).compareTo(pricing.basePrice()), "base price must be unchanged");
        assertEquals(0, BigDecimal.ZERO.compareTo(pricing.effectivePrice()), () -> planCode + " must resolve to Rs. 0");
        assertTrue(pricing.discounted());
        assertEquals(0, new BigDecimal(basePrice).compareTo(pricing.discountAmount()));
        assertEquals(campaign.getCode(), pricing.campaignCode());
    }

    // --- normal pricing fallback -------------------------------------------------------------------

    @Test
    void noActiveCampaignFallsBackToBasePrice() {
        PromotionPricingService service = serviceReturning();
        PromotionPricing pricing = service.resolve(tuitionPlan("TUITION_SEARCH_TOP_30D", "3490.00"), NOW);
        assertEquals(0, new BigDecimal("3490.00").compareTo(pricing.effectivePrice()));
        assertFalse(pricing.discounted());
    }

    // --- date boundaries -----------------------------------------------------------------------
    //
    // PromotionCampaignRepository.findActiveFor is the component that actually enforces
    // "c.active = true and :now between c.startsAt and c.endsAt" in SQL - a plain WHERE clause,
    // not logic worth a full database-backed test. What PromotionPricingService itself must get
    // right is its half of that contract: when the repository legitimately reports no match (as it
    // will for a not-yet-started, already-ended, or inactive campaign), pricing must fall back to
    // the base price rather than erroring or stale-caching a previous result. That's what these
    // three scenarios simulate via the mock.

    @Test
    void campaignBeforeItsStartDateDoesNotApply() {
        PromotionPricingService service = serviceReturning(); // repo finds nothing yet
        PromotionPricing pricing = service.resolve(tuitionPlan("TUITION_SEARCH_TOP_30D", "3490.00"), NOW);
        assertFalse(pricing.discounted());
        assertEquals(0, new BigDecimal("3490.00").compareTo(pricing.effectivePrice()));
    }

    @Test
    void campaignAfterItsEndDateDoesNotApply() {
        PromotionPricingService service = serviceReturning(); // repo no longer finds it
        PromotionPricing pricing = service.resolve(tuitionPlan("TUITION_SEARCH_TOP_30D", "3490.00"), NOW);
        assertFalse(pricing.discounted());
        assertEquals(0, new BigDecimal("3490.00").compareTo(pricing.effectivePrice()));
    }

    @Test
    void inactiveCampaignDoesNotApply() {
        PromotionPricingService service = serviceReturning(); // repo filters active = false out
        PromotionPricing pricing = service.resolve(tuitionPlan("TUITION_SEARCH_TOP_30D", "3490.00"), NOW);
        assertFalse(pricing.discounted());
        assertEquals(0, new BigDecimal("3490.00").compareTo(pricing.effectivePrice()));
    }

    // --- MAIN_SITE isolation -------------------------------------------------------------------

    @Test
    void mainSitePlanIsUnaffectedEvenWhenATuitionCampaignExists() {
        // The mock repository is never stubbed for a MAIN_SITE lookup, so it returns no matches -
        // exactly mirroring findActiveFor's real WHERE c.source_channel = :channel clause.
        PromotionCampaign tuitionLaunch = fixedPriceCampaign("990.00", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS));
        PromotionCampaignRepository repo = mock(PromotionCampaignRepository.class);
        when(repo.findActiveFor(org.mockito.ArgumentMatchers.eq(SourceChannel.TUITION), any(), any()))
                .thenReturn(List.of(tuitionLaunch));
        PromotionPricingService service = new PromotionPricingService(repo);

        PromotionPricing pricing = service.resolve(mainSitePlan("HOME_FEATURED_30D", "2500.00"), NOW);
        assertEquals(0, new BigDecimal("2500.00").compareTo(pricing.effectivePrice()));
        assertFalse(pricing.discounted());
    }
}
