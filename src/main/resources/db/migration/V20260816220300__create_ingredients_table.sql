
-- ----------------------------------------------------------------------------
-- INGREDIENTS (normalized master list — powers grocery-list merging)
-- ----------------------------------------------------------------------------

CREATE TABLE ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL UNIQUE,        -- canonical, lowercase name e.g. "garlic"
    category        TEXT,                        -- e.g. 'produce', 'dairy', 'pantry' (for aisle grouping)
    default_unit    TEXT,                        -- e.g. 'g', 'ml', 'each'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
