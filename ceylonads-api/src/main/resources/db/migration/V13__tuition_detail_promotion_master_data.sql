-- Tuition detail-page top carousel: a fixed, page-level promotional placement above the listing
-- on the CeylonAds Tuition UI's class detail page (GET /api/tuition/featured?slot=
-- TUITION_DETAIL_TOP_CAROUSEL). Reuses the existing promotion_slots/promotion_plans model as-is -
-- no new tables, no ALTER TABLE.
--
-- Deliberately its OWN slot rather than reusing TUITION_FEATURED (the homepage/search carousels'
-- shared CATEGORY_FEATURED slot for Education & Tuition, see V4): the detail page's placement must
-- be independently configurable and purchasable from the search page's, and
-- PromotionSlotService.resolveCategoryFeaturedSlot only ever resolves one CATEGORY_FEATURED slot
-- per category (an ancestor walk, not a per-page lookup), so a second CATEGORY_FEATURED slot on
-- the same category would be unreachable. Following V11's precedent, this reuses the
-- AD_DETAIL_SIDEBAR placement_type (resolved here by its exact `code`, never by
-- placement_type+category) rather than adding a 7th value to the
-- promotion_slots_placement_type_check constraint from V1.
--
-- capacity 12 / visible_count 4 mirrors TUITION_FEATURED's desktop-visible-4-of-12 shape, matching
-- the compact carousel's responsive breakpoints (4 desktop / 3 smaller desktop / 2 tablet / 1
-- mobile, computed client-side from rendered card width - see FeaturedTuitionCarousel).
INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_DETAIL_TOP_CAROUSEL', 'Tuition Detail Top Carousel', 'Fixed promotional carousel above the listing on the CeylonAds Tuition class detail page.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 12, 4, 110, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_DETAIL_TOP_CAROUSEL');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_DETAIL_TOP_CAROUSEL_7D', 'Tuition Detail Top Carousel — 7 Days', 'Fixed promotional carousel placement above the listing on the CeylonAds Tuition class detail page for 7 days.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_DETAIL_TOP_CAROUSEL'), 7, 1200.00, true, true, 110, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_DETAIL_TOP_CAROUSEL_7D');
