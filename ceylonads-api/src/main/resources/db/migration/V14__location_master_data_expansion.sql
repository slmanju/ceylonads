-- Expands Sri Lanka city/locality master data with commonly-used localities that were missing
-- from the initial V2 seed, focused on Colombo and Gampaha (highest tuition-search demand).
-- WHERE NOT EXISTS guards make this idempotent and safe to re-run.
--
-- Naming decisions:
-- - Colombo postal-numbered areas (e.g. "Colombo 06") are represented by their commonly used
--   suburb name (e.g. Wellawatte) instead of adding a separate numbered entry, to avoid two
--   locations meaning the same place.
-- - "Kotte" is intentionally NOT added as a separate entry; V2 already seeded the canonical
--   name "Sri Jayawardenepura Kotte" (slug sri-jayawardenepura-kotte) for that locality.

-- Colombo district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Nawala', 'nawala', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'nawala');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kirulapone', 'kirulapone', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kirulapone');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pannipitiya', 'pannipitiya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pannipitiya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Wellawatte', 'wellawatte', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'wellawatte');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Bambalapitiya', 'bambalapitiya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'bambalapitiya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kollupitiya', 'kollupitiya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kollupitiya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Borella', 'borella', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'borella');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Ratmalana', 'ratmalana', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'ratmalana');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Thalawathugoda', 'thalawathugoda', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'thalawathugoda');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kesbewa', 'kesbewa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kesbewa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Avissawella', 'avissawella', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'avissawella');

-- Gampaha district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Katana', 'katana', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'gampaha-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'katana');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Weliweriya', 'weliweriya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'gampaha-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'weliweriya');
