-- Required operational accounts. Passwords are BCrypt (cost 10, matching the app's
-- BCryptPasswordEncoder bean) hashes of generated passwords reported once to the requester
-- and never stored in plaintext here.
--
-- ADMIN accounts get an accounts row only, matching the existing 'admin' account (no Customer
-- profile). MODERATOR gets an accounts row plus a Customer profile, matching the existing
-- 'moderator1' account, since moderators can post ads like any other customer.

INSERT INTO accounts (username, email, password_hash, role, status, created_at, updated_at)
SELECT 'manjula', 'manjula@ceylonads.local',
       '$2a$10$/I0Thg6JwDcuiXyi0gD5YOKoeOGdgVttb110/KBHfaBP2e48t2xPS',
       'ADMIN', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'manjula');

INSERT INTO accounts (username, email, password_hash, role, status, created_at, updated_at)
SELECT 'chamila', 'chamila@ceylonads.local',
       '$2a$10$sFniigiOHCBNCLxuOfeqFu7eB7AwgYLdXjDoDnEE7wxZcjI/knfBa',
       'ADMIN', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'chamila');

INSERT INTO accounts (username, email, password_hash, role, status, created_at, updated_at)
SELECT 'chaminda', 'chaminda@ceylonads.local',
       '$2a$10$QzSPipS1CDRaSkTv648f0OQL/bH.HRoTreqhFkP3pOXsZZNEre6.O',
       'MODERATOR', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'chaminda');

INSERT INTO customers (account_id, display_name, phone, created_at)
SELECT a.id, 'Chaminda', NULL, now()
FROM accounts a
WHERE a.username = 'chaminda'
  AND NOT EXISTS (SELECT 1 FROM customers WHERE account_id = a.id);
