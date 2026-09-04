-- Adds expires_at to ads, backing the Tuition vertical's free-listing lifetime, renewal, and
-- paid-promotion duration protection. Nullable and left NULL for every existing row: only the
-- Tuition application layer ever sets it (on first approval), so MAIN_SITE/BOARDING ads keep
-- expires_at = NULL forever and are therefore never affected by the expiry-aware public-visibility
-- queries (NULL is treated as "never expires" everywhere expires_at is checked).
ALTER TABLE ads ADD COLUMN expires_at timestamp(6) with time zone;

-- Backs both the Tuition public-visibility queries (source_channel = 'TUITION' AND status = 'ACTIVE'
-- AND (expires_at IS NULL OR expires_at > now)) and the scheduled expiry bulk update (source_channel
-- = 'TUITION' AND status = 'ACTIVE' AND expires_at <= now). MAIN_SITE/BOARDING rows never populate
-- expires_at, so this index stays small and selective for the one vertical that actually uses it.
CREATE INDEX idx_ads_source_channel_status_expires ON ads (source_channel, status, expires_at);
