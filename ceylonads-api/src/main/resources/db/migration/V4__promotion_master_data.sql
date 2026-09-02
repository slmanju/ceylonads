-- Promotion slot and plan catalog.
-- Source: the live ceylonads_dev database's current promotion master data (9 slots, 10 plans).
-- CATEGORY_BANNER has no slot/plan data anywhere in the current product - not invented here.

-- Slots
INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'HOME_FEATURED', 'Homepage Featured', 'The homepage Featured Ads section.', 'HOME_FEATURED', NULL, 20, 4, 10, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'HOME_FEATURED');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'HOME_BANNER', 'Homepage Banner', 'A rotating carousel of banners on the homepage.', 'HOME_BANNER', NULL, 6, 1, 20, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'HOME_BANNER');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'VEHICLES_FEATURED', 'Vehicles Featured', 'Top placement on the Vehicles category page.', 'CATEGORY_FEATURED',
       (SELECT id FROM categories WHERE slug = 'vehicles'), 12, 4, 30, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'VEHICLES_FEATURED');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'PROPERTY_FEATURED', 'Property Featured', 'Top placement on the Property category page.', 'CATEGORY_FEATURED',
       (SELECT id FROM categories WHERE slug = 'property'), 12, 4, 40, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'PROPERTY_FEATURED');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'MOBILES_FEATURED', 'Mobiles Featured', 'Top placement on the Mobiles category page.', 'CATEGORY_FEATURED',
       (SELECT id FROM categories WHERE slug = 'mobiles'), 12, 4, 50, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'MOBILES_FEATURED');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'TUITION_FEATURED', 'Education & Tuition Featured', 'Top placement on the Education & Tuition category page.', 'CATEGORY_FEATURED',
       (SELECT id FROM categories WHERE slug = 'education-tuition'), 12, 4, 60, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'TUITION_FEATURED');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'AD_DETAIL_SIDEBAR', 'Ad Detail Sidebar', 'Featured ad displayed in the sidebar of an ad detail page', 'AD_DETAIL_SIDEBAR', NULL, 1, 1, 60, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'AD_DETAIL_SIDEBAR');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'SERVICES_FEATURED', 'Services Featured', 'Top placement on the Services category page.', 'CATEGORY_FEATURED',
       (SELECT id FROM categories WHERE slug = 'services'), 12, 4, 70, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'SERVICES_FEATURED');

INSERT INTO promotion_slots (code, name, description, placement_type, category_id, capacity, visible_count, display_order, active, created_at, updated_at)
SELECT 'SEARCH_TOP', 'Top Search', 'Top placement in general search and browse results.', 'TOP_SEARCH', NULL, 20, 3, 90, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_slots WHERE code = 'SEARCH_TOP');

-- Plans
INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'HOME_FEATURED_7D', 'Homepage Featured — 7 Days', 'Highlight your ad on the CeylonAds homepage Featured Ads section.',
       (SELECT id FROM promotion_slots WHERE code = 'HOME_FEATURED'), 7, 750.00, true, true, 10, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'HOME_FEATURED_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'HOME_FEATURED_30D', 'Homepage Featured — 30 Days', 'Highlight your ad on the CeylonAds homepage Featured Ads section for a full month.',
       (SELECT id FROM promotion_slots WHERE code = 'HOME_FEATURED'), 30, 2500.00, true, true, 11, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'HOME_FEATURED_30D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'HOME_BANNER_7D', 'Homepage Banner — 7 Days', 'A rotating banner placement on the CeylonAds homepage.',
       (SELECT id FROM promotion_slots WHERE code = 'HOME_BANNER'), 7, 5000.00, true, true, 20, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'HOME_BANNER_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'VEHICLES_FEATURED_7D', 'Vehicles Featured — 7 Days', 'Appear above regular ads on the Vehicles category page.',
       (SELECT id FROM promotion_slots WHERE code = 'VEHICLES_FEATURED'), 7, 1000.00, true, true, 30, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'VEHICLES_FEATURED_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'PROPERTY_FEATURED_7D', 'Property Featured — 7 Days', 'Appear above regular ads on the Property category page.',
       (SELECT id FROM promotion_slots WHERE code = 'PROPERTY_FEATURED'), 7, 1000.00, true, true, 40, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'PROPERTY_FEATURED_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'MOBILES_FEATURED_7D', 'Mobiles Featured — 7 Days', 'Appear above regular ads on the Mobiles category page.',
       (SELECT id FROM promotion_slots WHERE code = 'MOBILES_FEATURED'), 7, 1000.00, true, true, 50, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'MOBILES_FEATURED_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_FEATURED_7D', 'Education & Tuition Featured — 7 Days', 'Appear above regular ads on the Education & Tuition category page.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_FEATURED'), 7, 1000.00, true, true, 60, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_FEATURED_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'SERVICES_FEATURED_7D', 'Services Featured — 7 Days', 'Appear above regular ads on the Services category page.',
       (SELECT id FROM promotion_slots WHERE code = 'SERVICES_FEATURED'), 7, 1000.00, true, true, 70, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'SERVICES_FEATURED_7D');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'DETAIL_SIDEBAR_FEATURED', 'Detail Page Featured Ad', 'Feature an ad in the sidebar of relevant ad detail pages for 7 days',
       (SELECT id FROM promotion_slots WHERE code = 'AD_DETAIL_SIDEBAR'), 7, 0.00, true, true, 60, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'DETAIL_SIDEBAR_FEATURED');

INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TOP_SEARCH_7D', 'Top Search — 7 Days', 'Rank higher in general search and browse results.',
       (SELECT id FROM promotion_slots WHERE code = 'SEARCH_TOP'), 7, 400.00, true, true, 90, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TOP_SEARCH_7D');
