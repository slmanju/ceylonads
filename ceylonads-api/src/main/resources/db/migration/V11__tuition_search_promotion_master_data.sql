-- Tuition search-page promotion placements: a top banner + 3 sidebar positions on the CeylonAds
-- Tuition UI search/classes page (GET /api/tuition/promotions). Reuses the existing
-- promotion_slots/promotion_plans model as-is - no new tables, no ALTER TABLE.
--
-- placement_type is constrained by promotion_slots_placement_type_check (see V1) to the existing
-- 6 values, so these new slots reuse two of them rather than adding a 7th:
--   - TUITION_SEARCH_TOP_BANNER uses CATEGORY_BANNER, which V4's own comment notes has no slot/
--     plan data anywhere in the current product - safe to introduce here without affecting any
--     existing feature.
--   - The 3 sidebar slots reuse AD_DETAIL_SIDEBAR. That placement type already has one slot/plan
--     (AD_DETAIL_SIDEBAR / DETAIL_SIDEBAR_FEATURED, see V4) but grep confirms zero Java code
--     resolves promotions by placement_type for it - only by exact slot `code` - so adding more
--     distinct-coded AD_DETAIL_SIDEBAR slots here cannot affect that existing slot or any future
--     consumer of it.
-- All four are bound to the Education & Tuition category so a promoted ad must belong to that
-- category (or one of its descendants) per PromotionService.categoryCompatible's ancestor walk -
-- no separate tuition-scoping logic is needed at read time for this reason alone.

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_TOP_BANNER', 'Tuition Search Top Banner', 'Banner placement above the CeylonAds Tuition search results.', 'CATEGORY_BANNER',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 3, 1, 100, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_SEARCH_TOP_BANNER');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_TOP', 'Tuition Search Sidebar Top', 'Top position in the CeylonAds Tuition search results sidebar.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 1, 1, 101, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_MIDDLE', 'Tuition Search Sidebar Middle', 'Middle position in the CeylonAds Tuition search results sidebar.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 1, 1, 102, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_MIDDLE');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_BOTTOM', 'Tuition Search Sidebar Bottom', 'Bottom position in the CeylonAds Tuition search results sidebar.', 'AD_DETAIL_SIDEBAR',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 1, 1, 103, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_BOTTOM');

-- Plans: minimal, single 7-day plan per slot, following the V4 convention (payment + approval
-- required, price a placeholder consistent with existing tuition/category plan pricing).
INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_TOP_BANNER_7D', 'Tuition Search Top Banner — 7 Days', 'Banner placement above the CeylonAds Tuition search results for 7 days.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_TOP_BANNER'), 7, 1500.00, true, true, 100, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_TOP_BANNER_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_TOP_7D', 'Tuition Search Sidebar — 7 Days', 'Top sidebar placement on the CeylonAds Tuition search results for 7 days.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP'), 7, 800.00, true, true, 101, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_MIDDLE_7D', 'Tuition Search Sidebar — 7 Days', 'Middle sidebar placement on the CeylonAds Tuition search results for 7 days.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_MIDDLE'), 7, 800.00, true, true, 102, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_SIDEBAR_MIDDLE_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_BOTTOM_7D', 'Tuition Search Sidebar — 7 Days', 'Bottom sidebar placement on the CeylonAds Tuition search results for 7 days.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_BOTTOM'), 7, 800.00, true, true, 103, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_SIDEBAR_BOTTOM_7D');
