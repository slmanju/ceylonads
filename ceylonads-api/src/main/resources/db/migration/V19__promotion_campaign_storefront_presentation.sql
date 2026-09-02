-- Adds customer-facing storefront presentation to promotion_campaigns, so a campaign's banner/
-- modal copy on the Tuition UI (and eventually other channels) is configured here rather than
-- hardcoded in frontend code. The existing `name` column is reused as the customer-facing campaign
-- name - no duplicate name column. `description` remains the internal/admin-facing summary it
-- already was; it is not shown to customers.
--
-- Plain text only (no HTML) - enforced by convention/service validation, not a DB constraint.
ALTER TABLE promotion_campaigns
    ADD COLUMN headline character varying(180),
    ADD COLUMN message character varying(500),
    ADD COLUMN cta_label character varying(80),
    ADD COLUMN customer_visible boolean NOT NULL DEFAULT false,
    ADD COLUMN show_banner boolean NOT NULL DEFAULT false,
    ADD COLUMN show_modal boolean NOT NULL DEFAULT false;

-- Whether headline/message/cta_label/name are actually populated when customer_visible=true is
-- enforced in PromotionCampaignService (see requireValidStorefrontFields) rather than here - a
-- "non-blank trimmed text" CHECK is awkward in SQL and this codebase's convention (see V17) keeps
-- CHECK constraints to simple structural/enum invariants.
ALTER TABLE promotion_campaigns
    ADD CONSTRAINT ck_promotion_campaigns_storefront_visibility
    CHECK (NOT (show_banner OR show_modal) OR customer_visible);
