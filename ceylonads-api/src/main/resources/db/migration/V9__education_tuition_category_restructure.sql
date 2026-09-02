-- Correct the Education & Tuition category hierarchy: add the missing performing/creative-arts
-- and technology categories as direct children of education-tuition, and retire Online Courses as
-- a category (delivery mode - online/physical/both - is already represented by the classMode
-- attribute on individual tuition categories, not by a separate category).
--
-- This migration is master-data only: no schema changes, no edits to already-applied migrations,
-- no table/column changes. It preserves the existing two-level category structure - every category
-- inserted below is a direct child of education-tuition, never a grandchild.
--
-- Online Courses (slug: online-courses) verification performed before writing this migration:
--   - grep across ceylonads-api (src/main, src/test) and both frontends for "online-courses" /
--     "Online Courses" found no ad, no test, and no Flyway migration that inserts an ad or other
--     row referencing this category - only two frontend icon lookup maps
--     (ceylonads-tuition-ui HomePage.tsx / PostAd/CategoryStep.tsx) key an icon off its slug, both
--     of which fall back to a default icon and read categories dynamically from the API rather
--     than hardcoding the category list, so they need no code change.
--   - No Flyway migration (V1-V8) or seed data ever inserts ad rows at all; ads only exist from
--     real usage or the disabled/manual-only LocalDataSeeder, which was not run against this
--     category in this codebase.
--   - Given this, there are no known ads to migrate away from Online Courses in this codebase's
--     data. This migration therefore deactivates the category (active = false) rather than
--     deleting it or reassigning ads: deactivation is non-destructive and reversible, matches the
--     existing soft-disable convention already used for attribute_definitions/attribute_options in
--     this schema, and - unlike a hard delete - can never violate the NOT NULL/REFERENCES
--     constraint on ads.category_id even if an environment other than this one turns out to have
--     ads under this category. If such ads exist elsewhere, they keep their current category
--     reference (now inactive) rather than being silently reassigned, since no single replacement
--     category (School Tuition vs. Higher Education vs. Language Classes vs. Professional Courses)
--     can be determined as universally correct without inspecting each ad's actual subject matter.

-- New direct children of Education & Tuition. Music/Dancing/Drama & Theatre/Art & Creative
-- Classes/Technology & Coding replace what would otherwise become deeper category levels (e.g.
-- Music -> Piano) - specific styles/subjects stay as `subject` attribute values on these
-- categories, never as additional category levels.
INSERT INTO categories (name, slug, parent_id, display_order, active) SELECT 'Music', 'music', p.id, 50, 't' FROM categories p WHERE p.slug = 'education-tuition' AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'music');
INSERT INTO categories (name, slug, parent_id, display_order, active) SELECT 'Dancing', 'dancing', p.id, 60, 't' FROM categories p WHERE p.slug = 'education-tuition' AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'dancing');
INSERT INTO categories (name, slug, parent_id, display_order, active) SELECT 'Drama & Theatre', 'drama-theatre', p.id, 70, 't' FROM categories p WHERE p.slug = 'education-tuition' AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'drama-theatre');
INSERT INTO categories (name, slug, parent_id, display_order, active) SELECT 'Art & Creative Classes', 'art-creative-classes', p.id, 80, 't' FROM categories p WHERE p.slug = 'education-tuition' AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'art-creative-classes');
INSERT INTO categories (name, slug, parent_id, display_order, active) SELECT 'Technology & Coding', 'technology-coding', p.id, 90, 't' FROM categories p WHERE p.slug = 'education-tuition' AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'technology-coding');
INSERT INTO categories (name, slug, parent_id, display_order, active) SELECT 'Other Education & Tuition', 'other-education-tuition', p.id, 100, 't' FROM categories p WHERE p.slug = 'education-tuition' AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'other-education-tuition');

-- Online Courses is a delivery mode, not an education type - retire it as a category. Deactivate
-- only (see rationale above); do not delete, do not reassign ads.
UPDATE categories SET active = false WHERE slug = 'online-courses' AND active = true;

-- Attribute definitions for the 5 new subject-driven categories, mirroring the existing
-- school-tuition `subject` (TEXT) and `classMode` (SELECT: the existing delivery-mode attribute -
-- see V3__category_attribute_master_data.sql) definitions exactly. `grade`/`curriculum` are
-- intentionally not added here - they don't apply to these categories (per the domain rules this
-- migration follows, level/curriculum stay optional/absent where they don't apply), and `medium`
-- is left out too since no concrete requirement for it was given for these categories. Other
-- Education & Tuition intentionally gets no attribute definitions, matching the precedent already
-- set by most "Other X" fallback categories elsewhere in this schema (e.g. Other Electronics,
-- Other Jobs, Other Food & Beverages).
INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'subject', 'Subject', 'TEXT', 't', 't', 't', NULL, 10, 't', now(), now() FROM categories c WHERE c.slug = 'music' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'subject');
INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'classMode', 'Class Mode', 'SELECT', 't', 't', 'f', NULL, 20, 't', now(), now() FROM categories c WHERE c.slug = 'music' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'classMode');

INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'subject', 'Subject', 'TEXT', 't', 't', 't', NULL, 10, 't', now(), now() FROM categories c WHERE c.slug = 'dancing' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'subject');
INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'classMode', 'Class Mode', 'SELECT', 't', 't', 'f', NULL, 20, 't', now(), now() FROM categories c WHERE c.slug = 'dancing' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'classMode');

INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'subject', 'Subject', 'TEXT', 't', 't', 't', NULL, 10, 't', now(), now() FROM categories c WHERE c.slug = 'drama-theatre' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'subject');
INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'classMode', 'Class Mode', 'SELECT', 't', 't', 'f', NULL, 20, 't', now(), now() FROM categories c WHERE c.slug = 'drama-theatre' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'classMode');

INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'subject', 'Subject', 'TEXT', 't', 't', 't', NULL, 10, 't', now(), now() FROM categories c WHERE c.slug = 'art-creative-classes' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'subject');
INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'classMode', 'Class Mode', 'SELECT', 't', 't', 'f', NULL, 20, 't', now(), now() FROM categories c WHERE c.slug = 'art-creative-classes' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'classMode');

INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'subject', 'Subject', 'TEXT', 't', 't', 't', NULL, 10, 't', now(), now() FROM categories c WHERE c.slug = 'technology-coding' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'subject');
INSERT INTO attribute_definitions (category_id, attribute_key, name, data_type, required, filterable, searchable, unit, display_order, active, created_at, updated_at) SELECT c.id, 'classMode', 'Class Mode', 'SELECT', 't', 't', 'f', NULL, 20, 't', now(), now() FROM categories c WHERE c.slug = 'technology-coding' AND NOT EXISTS (SELECT 1 FROM attribute_definitions WHERE category_id = c.id AND attribute_key = 'classMode');

-- classMode options, identical value/label/display_order to the existing school-tuition classMode
-- options (see V3) - no new option values invented.
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'PHYSICAL', 'Physical', 1, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'music' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'PHYSICAL');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'ONLINE', 'Online', 2, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'music' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'ONLINE');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'BOTH', 'Online & Physical', 3, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'music' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'BOTH');

INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'PHYSICAL', 'Physical', 1, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'dancing' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'PHYSICAL');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'ONLINE', 'Online', 2, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'dancing' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'ONLINE');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'BOTH', 'Online & Physical', 3, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'dancing' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'BOTH');

INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'PHYSICAL', 'Physical', 1, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'drama-theatre' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'PHYSICAL');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'ONLINE', 'Online', 2, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'drama-theatre' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'ONLINE');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'BOTH', 'Online & Physical', 3, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'drama-theatre' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'BOTH');

INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'PHYSICAL', 'Physical', 1, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'art-creative-classes' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'PHYSICAL');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'ONLINE', 'Online', 2, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'art-creative-classes' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'ONLINE');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'BOTH', 'Online & Physical', 3, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'art-creative-classes' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'BOTH');

INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'PHYSICAL', 'Physical', 1, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'technology-coding' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'PHYSICAL');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'ONLINE', 'Online', 2, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'technology-coding' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'ONLINE');
INSERT INTO attribute_options (attribute_definition_id, option_value, label, display_order, active) SELECT d.id, 'BOTH', 'Online & Physical', 3, 't' FROM attribute_definitions d JOIN categories c ON c.id = d.category_id WHERE c.slug = 'technology-coding' AND d.attribute_key = 'classMode' AND NOT EXISTS (SELECT 1 FROM attribute_options WHERE attribute_definition_id = d.id AND option_value = 'BOTH');
