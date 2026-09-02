-- Cleans up the Tuition promotion-purchase catalog down to exactly six products (Homepage
-- Featured, Homepage Spotlight, Search Page Featured, Search Boost, Detail Page Featured, Detail
-- Page Spotlight), each a 30-day plan with a clean tutor-facing name, and introduces the two
-- launch/50%-off campaigns (inactive scaffolding - see below) that PromotionPricingService
-- resolves against them. Combined with V16's source_channel column, this also fixes the leakage
-- where a handful of channel-agnostic MAIN_SITE plans (Homepage Featured, Top Search, Detail Page
-- Featured Ad) were showing up in the Tuition purchase catalog: every slot below is tagged
-- source_channel = 'TUITION'.
--
-- Two existing slots are stable, already-in-use identifiers (TuitionFeaturedService resolves them
-- by exact code for the ezClass homepage/detail-page carousels) so their *code* is preserved and
-- only the display name/description is cleaned up: TUITION_FEATURED -> "Homepage Featured",
-- TUITION_DETAIL_TOP_CAROUSEL -> "Detail Page Featured". The four other products are brand new
-- slots. TUITION_SEARCH_TOP_BANNER (a standalone banner-ad product, not a per-ad boost - see
-- PlacementType.isBanner()) is untouched and out of scope.
--
-- placement_type reuse (no ALTER to the V1 CHECK constraint): TUITION_HOME_LATEST_RIGHT/
-- TUITION_SEARCH_BOOST/TUITION_DETAIL_RIGHT reuse AD_DETAIL_SIDEBAR (the V11/V13 precedent for an
-- extra Tuition slot on the same category beyond the CATEGORY_FEATURED singleton -
-- PromotionSlotService.resolveCategoryFeaturedSlot only ever resolves one CATEGORY_FEATURED slot
-- per category, so a second one on education-tuition would be unreachable from that path).
-- TUITION_SEARCH_TOP reuses TOP_SEARCH with a non-null category, which is safe: the main site's
-- singleton SEARCH_TOP resolution (findByPlacementTypeAndCategoryIsNull) only ever matches
-- category-less slots, so this can never collide with it. All Tuition slots are resolved by exact
-- code in the Tuition-specific services, never by placement_type+category.

-- 1) Rename the two preserved slots' display name/description only (code/category/placement_type
--    untouched).
UPDATE promotion_slots
SET name = 'Homepage Featured',
    description = 'Get featured prominently on the ezClass homepage.',
    updated_at = now()
WHERE code = 'TUITION_FEATURED';

UPDATE promotion_slots
SET name = 'Detail Page Featured',
    description = 'Appear in the promoted section across Tuition class detail pages.',
    updated_at = now()
WHERE code = 'TUITION_DETAIL_TOP_CAROUSEL';

-- 2) Every existing Tuition-specific slot (the two preserved above, the untouched banner slot,
--    and the three sidebar slots being retired below) predates source_channel and is backfilled
--    TUITION here; every other slot was already backfilled MAIN_SITE by V16.
UPDATE promotion_slots
SET source_channel = 'TUITION', updated_at = now()
WHERE code IN (
    'TUITION_FEATURED', 'TUITION_SEARCH_TOP_BANNER', 'TUITION_DETAIL_TOP_CAROUSEL',
    'TUITION_SEARCH_SIDEBAR_TOP', 'TUITION_SEARCH_SIDEBAR_MIDDLE', 'TUITION_SEARCH_SIDEBAR_BOTTOM'
);

-- 3) The four brand-new slots, all bound to education-tuition and source_channel = TUITION.
INSERT INTO promotion_slots (code, name, description, placement_type, category_id, source_channel, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_HOME_LATEST_RIGHT', 'Homepage Spotlight', 'Appear beside the latest classes on the ezClass homepage.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 'TUITION', 8, 3, 51, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_HOME_LATEST_RIGHT');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, source_channel, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_TOP', 'Search Page Featured', 'Get premium visibility above the Tuition search experience.', 'TOP_SEARCH',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 'TUITION', 12, 4, 10, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_SEARCH_TOP');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, source_channel, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_BOOST', 'Search Boost', 'Rank prominently when students browse and search for classes.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 'TUITION', 20, 6, 20, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_SEARCH_BOOST');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, source_channel, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_DETAIL_RIGHT', 'Detail Page Spotlight', 'Get visible placement beside class details.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 'TUITION', 8, 3, 41, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_DETAIL_RIGHT');

-- 4) The six final 30-day plans, one per product above. display_order matches the commercial
--    priority order (search first, since search/discovery is ezClass's primary surface): Search
--    Page Featured, Search Boost, Homepage Featured, Detail Page Featured, Homepage Spotlight,
--    Detail Page Spotlight.
INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_TOP_30D', 'Search Page Featured', 'Get premium visibility above the Tuition search experience.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_TOP'), 30, 3490.00, true, true, 10, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_TOP_30D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_BOOST_30D', 'Search Boost', 'Rank prominently when students browse and search for classes.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_BOOST'), 30, 2990.00, true, true, 20, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_BOOST_30D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_HOME_FEATURED_30D', 'Homepage Featured', 'Get featured prominently on the ezClass homepage.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_FEATURED'), 30, 2490.00, true, true, 30, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_HOME_FEATURED_30D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_DETAIL_TOP_30D', 'Detail Page Featured', 'Appear in the promoted section across Tuition class detail pages.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_DETAIL_TOP_CAROUSEL'), 30, 1990.00, true, true, 40, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_DETAIL_TOP_30D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_HOME_LATEST_RIGHT_30D', 'Homepage Spotlight', 'Appear beside the latest classes on the ezClass homepage.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_HOME_LATEST_RIGHT'), 30, 1490.00, true, true, 50, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_HOME_LATEST_RIGHT_30D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_DETAIL_RIGHT_30D', 'Detail Page Spotlight', 'Get visible placement beside class details.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_DETAIL_RIGHT'), 30, 990.00, true, true, 60, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_DETAIL_RIGHT_30D');

-- 5) Retire the plans/slots superseded by the six above. Deactivating (never deleting) preserves
--    referential integrity for any already-sold promotions: PromotionService.compatiblePlansForAd
--    filters on plan.active/slot.active, so this only stops *new* purchases - already-active
--    promotions keep displaying via TuitionPromotionService until they expire.
UPDATE promotion_plans SET active = false, updated_at = now()
WHERE code IN (
    'TUITION_FEATURED_7D', 'TUITION_DETAIL_TOP_CAROUSEL_7D',
    'TUITION_SEARCH_SIDEBAR_TOP_7D', 'TUITION_SEARCH_SIDEBAR_MIDDLE_7D', 'TUITION_SEARCH_SIDEBAR_BOTTOM_7D'
);

UPDATE promotion_slots SET active = false, updated_at = now()
WHERE code IN ('TUITION_SEARCH_SIDEBAR_TOP', 'TUITION_SEARCH_SIDEBAR_MIDDLE', 'TUITION_SEARCH_SIDEBAR_BOTTOM');

-- 6) Seed the two campaigns known to be needed at launch, both inactive with placeholder dates:
--    per product requirements, the real launch date is not yet known and must not be invented
--    here, so an admin activates each campaign (and can adjust its dates) via
--    AdminPromotionCampaignController with no deploy required. EZCLASS_LAUNCH_990 is a flat
--    Rs. 990 for all six products; EZCLASS_HALF_PRICE is 50% off each plan's base price with a
--    Rs. 990 floor (see PromotionPricingService) so no plan ever undercuts the launch price.
INSERT INTO promotion_campaigns (code, name, description, source_channel, pricing_type, discount_percent, fixed_price, minimum_price, active, starts_at, ends_at, created_at, updated_at)
SELECT 'EZCLASS_LAUNCH_990', 'ezClass Launch Offer', 'Promote any Tuition ad for Rs. 990 for 30 days.',
       'TUITION', 'FIXED_PRICE', NULL, 990.00, NULL, false, now(), now() + interval '3 months', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_campaigns WHERE code = 'EZCLASS_LAUNCH_990');

INSERT INTO promotion_campaigns (code, name, description, source_channel, pricing_type, discount_percent, fixed_price, minimum_price, active, starts_at, ends_at, created_at, updated_at)
SELECT 'EZCLASS_HALF_PRICE', 'ezClass 50% Offer', '50% off the normal price, down to a Rs. 990 minimum.',
       'TUITION', 'PERCENTAGE_DISCOUNT', 50.00, NULL, 990.00, false, now() + interval '3 months', now() + interval '6 months', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_campaigns WHERE code = 'EZCLASS_HALF_PRICE');

-- 7) Map both campaigns to all six Tuition products.
INSERT INTO promotion_campaign_plans (campaign_id, promotion_plan_id)
SELECT c.id, p.id
FROM promotion_campaigns c
CROSS JOIN promotion_plans p
WHERE c.code IN ('EZCLASS_LAUNCH_990', 'EZCLASS_HALF_PRICE')
  AND p.code IN (
      'TUITION_SEARCH_TOP_30D', 'TUITION_SEARCH_BOOST_30D', 'TUITION_HOME_FEATURED_30D',
      'TUITION_DETAIL_TOP_30D', 'TUITION_HOME_LATEST_RIGHT_30D', 'TUITION_DETAIL_RIGHT_30D'
  )
  AND NOT EXISTS (
      SELECT 1 FROM promotion_campaign_plans cp WHERE cp.campaign_id = c.id AND cp.promotion_plan_id = p.id
  );
