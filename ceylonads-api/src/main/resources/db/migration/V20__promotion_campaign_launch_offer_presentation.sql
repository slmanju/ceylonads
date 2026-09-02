-- Populates storefront presentation for the two Tuition campaigns seeded in
-- V18__tuition_promotion_catalog_v2.sql, so the Tuition UI's campaign banner/modal have real
-- backend-configured copy instead of nothing (customer_visible/show_banner/show_modal default to
-- false, so without this update neither campaign would ever surface on the storefront).
--
-- Pricing/dates/active are untouched here - this migration only sets presentation fields.
UPDATE promotion_campaigns
SET headline = 'Promote any Tuition Ad for just Rs. 990',
    message = 'Flat rate for all eligible promotion placements during our launch period.',
    cta_label = 'Promote My Ad',
    customer_visible = true,
    show_banner = true,
    show_modal = true,
    updated_at = now()
WHERE code = 'EZCLASS_LAUNCH_990';

UPDATE promotion_campaigns
SET headline = 'Get 50% Off Tuition Promotions',
    message = 'Promote your class at half the normal price during our extended launch offer.',
    cta_label = 'Promote My Ad',
    customer_visible = true,
    show_banner = true,
    show_modal = true,
    updated_at = now()
WHERE code = 'EZCLASS_HALF_PRICE';
