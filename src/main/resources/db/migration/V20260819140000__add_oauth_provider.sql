-- ----------------------------------------------------------------------------
-- OAUTH PROVIDER LINKAGE
--
-- An account signed in through Google still needs a users row, because recipes.user_id
-- is a real foreign key. password_hash was already nullable for exactly this case.
--
-- Matching is on (provider, provider_id) and never on email alone: a provider's email
-- can change over time, its subject id cannot.
-- ----------------------------------------------------------------------------

ALTER TABLE users ADD COLUMN provider    TEXT;  -- null for password-only accounts
ALTER TABLE users ADD COLUMN provider_id TEXT;  -- the provider's stable subject id

CREATE UNIQUE INDEX idx_users_provider ON users (provider, provider_id)
    WHERE provider IS NOT NULL;

-- One provider per account. A second provider would need a user_identities table
-- instead; see AUTH_PLAN.md.
ALTER TABLE users ADD CONSTRAINT chk_users_provider_pair
    CHECK ((provider IS NULL) = (provider_id IS NULL));

-- An account must be reachable somehow: either a password or a linked provider.
ALTER TABLE users ADD CONSTRAINT chk_users_has_credential
    CHECK (password_hash IS NOT NULL OR provider IS NOT NULL);
