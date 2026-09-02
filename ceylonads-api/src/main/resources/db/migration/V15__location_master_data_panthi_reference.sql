-- Sri Lanka city/locality master data expansion, sourced from a manual district-by-district
-- comparison against the publicly visible location taxonomy on panthi.lk/post-free-ad (place
-- names only - no ad content, phone numbers, or Panthi IDs were collected or copied).
-- WHERE NOT EXISTS guards make this idempotent and safe to re-run.
--
-- Spelling/duplicate variants seen in the reference source were NOT re-added because a
-- canonical entry already exists from V2/V14: Kolpity -> Kollupitiya, Kotte -> Sri
-- Jayawardenepura Kotte, Kantalai -> Kantale, Muttur -> Mutur, Ginigathena -> Ginigathhena,
-- Thalawakele -> Talawakele, Pilimatalawa -> Pilimathalawa, Alutgama -> Aluthgama,
-- Pudukudiyirippu -> Puthukkudiyiruppu, Kaluvanchikudy -> Kaluwanchikudy, Kekanadurra ->
-- Kekanadura, Kahawaththa -> Kahawatta, Nanaddan -> Nanattan.
--
-- Known hierarchy note (not changed here): the reference source lists Kataragama under
-- Moneragala district, but V2 already seeded 'kataragama' (slug) under Hambantota district.
-- V2 is an already-applied migration and is not edited by this file, and a second 'kataragama'
-- location cannot be added under Moneragala without violating the global-unique slug
-- constraint / creating a duplicate place. Left as-is; flagged for manual follow-up.
--
-- Ambiguous placement, followed as given in the reference source: Habarana -> Anuradhapura,
-- Udawalawe -> Ratnapura (both towns sit near a district boundary and are also commonly
-- associated with a neighbouring district).

-- Colombo district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Aluthkade', 'aluthkade', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'aluthkade');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Angoda', 'angoda', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'angoda');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Cinnamon Gardens', 'cinnamon-gardens', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'cinnamon-gardens');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Dematagoda', 'dematagoda', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'dematagoda');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Fort', 'fort', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'fort');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Grandpass', 'grandpass', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'grandpass');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Havelock Town', 'havelock-town', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'havelock-town');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Hokandara', 'hokandara', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'hokandara');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kahanthota', 'kahanthota', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kahanthota');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kalubowila', 'kalubowila', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kalubowila');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kochchikade', 'kochchikade', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kochchikade');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kolonnawa', 'kolonnawa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kolonnawa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kotahena', 'kotahena', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kotahena');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Madampitiya', 'madampitiya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'madampitiya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Maradana', 'maradana', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'maradana');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Mattakkuliya', 'mattakkuliya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'mattakkuliya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Modara', 'modara', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'modara');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Narahenpita', 'narahenpita', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'narahenpita');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Padukka', 'padukka', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'padukka');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pamankada', 'pamankada', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pamankada');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Panchikawatte', 'panchikawatte', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'panchikawatte');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pelawatte', 'pelawatte', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pelawatte');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pettah', 'pettah', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pettah');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Slave Island', 'slave-island', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'slave-island');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Ward Place', 'ward-place', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'ward-place');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Wellampitiya', 'wellampitiya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'colombo-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'wellampitiya');

-- Gampaha district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Delgoda', 'delgoda', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'gampaha-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'delgoda');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Dompe', 'dompe', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'gampaha-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'dompe');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Peliyagoda', 'peliyagoda', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'gampaha-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'peliyagoda');

-- Kalutara district: reference source has no new localities beyond V2 (Alutgama = existing Aluthgama).

-- Kandy district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Ampitiya', 'ampitiya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kandy-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'ampitiya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Gelioya', 'gelioya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kandy-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'gelioya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Madawala Bazaar', 'madawala-bazaar', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kandy-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'madawala-bazaar');

-- Matale district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Palapathwela', 'palapathwela', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'matale-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'palapathwela');

-- Nuwara Eliya district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Dickoya', 'dickoya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'nuwara-eliya-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'dickoya');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Lindula', 'lindula', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'nuwara-eliya-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'lindula');

-- Galle district: reference source has no new localities beyond V2.

-- Matara district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kamburugamuwa', 'kamburugamuwa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'matara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kamburugamuwa');

-- Hambantota district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Angunukolapelessa', 'angunukolapelessa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'hambantota-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'angunukolapelessa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Middeniya', 'middeniya', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'hambantota-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'middeniya');

-- Jaffna district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Chankanai', 'chankanai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'chankanai');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Karaveddy', 'karaveddy', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'karaveddy');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kayts', 'kayts', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kayts');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Manipay', 'manipay', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'manipay');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Maruthankerney', 'maruthankerney', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'maruthankerney');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Sandilipay', 'sandilipay', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'sandilipay');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Uduvil', 'uduvil', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'uduvil');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Valvettithurai', 'valvettithurai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'jaffna-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'valvettithurai');

-- Kilinochchi district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kandavalai', 'kandavalai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kilinochchi-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kandavalai');

-- Mannar district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Adampan', 'adampan', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'mannar-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'adampan');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Chilawathurai', 'chilawathurai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'mannar-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'chilawathurai');

-- Mullaitivu district: reference source has no new localities beyond V2.

-- Vavuniya district: reference source has no new localities beyond V2.

-- Ampara district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Addalaichchenai', 'addalaichchenai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'addalaichchenai');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Alayadivembu', 'alayadivembu', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'alayadivembu');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Damana', 'damana', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'damana');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Irakkamam', 'irakkamam', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'irakkamam');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Karaitivu', 'karaitivu', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'karaitivu');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Lahugala', 'lahugala', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'lahugala');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Navithanveli', 'navithanveli', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'navithanveli');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Padiyathalawa', 'padiyathalawa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'padiyathalawa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Thirukkovil', 'thirukkovil', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ampara-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'thirukkovil');

-- Batticaloa district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Araiyampathy', 'araiyampathy', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'batticaloa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'araiyampathy');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kiran', 'kiran', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'batticaloa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kiran');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kokkadichcholai', 'kokkadichcholai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'batticaloa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kokkadichcholai');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pasikudah', 'pasikudah', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'batticaloa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pasikudah');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Vavunathivu', 'vavunathivu', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'batticaloa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'vavunathivu');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Vellavely', 'vellavely', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'batticaloa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'vellavely');

-- Trincomalee district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'China Bay', 'china-bay', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'trincomalee-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'china-bay');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pulmoddai', 'pulmoddai', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'trincomalee-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pulmoddai');

-- Kurunegala district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Giriulla', 'giriulla', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kurunegala-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'giriulla');

-- Puttalam district: reference source has no new localities beyond V2.

-- Anuradhapura district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Galnewa', 'galnewa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'anuradhapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'galnewa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Habarana', 'habarana', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'anuradhapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'habarana');

-- Polonnaruwa district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Bakamuna', 'bakamuna', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'polonnaruwa-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'bakamuna');

-- Badulla district: reference source has no new localities beyond V2.

-- Moneragala district (kataragama intentionally NOT added here - see header note)
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Madulla', 'madulla', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'moneragala-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'madulla');

-- Kegalle district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Galigamuwa', 'galigamuwa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kegalle-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'galigamuwa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kitulgala', 'kitulgala', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'kegalle-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kitulgala');

-- Ratnapura district
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Ayagama', 'ayagama', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'ayagama');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Elapatha', 'elapatha', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'elapatha');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Imbulpe', 'imbulpe', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'imbulpe');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kalthota', 'kalthota', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kalthota');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Kiriella', 'kiriella', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'kiriella');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Opanayaka', 'opanayaka', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'opanayaka');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pallebedda', 'pallebedda', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pallebedda');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Panamure', 'panamure', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'panamure');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Pohorabawa', 'pohorabawa', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'pohorabawa');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Udawalawe', 'udawalawe', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'udawalawe');
INSERT INTO locations (name, slug, type, parent_id, active) SELECT 'Weligepola', 'weligepola', 'CITY', p.id, 't' FROM locations p WHERE p.slug = 'ratnapura-district' AND NOT EXISTS (SELECT 1 FROM locations WHERE slug = 'weligepola');
