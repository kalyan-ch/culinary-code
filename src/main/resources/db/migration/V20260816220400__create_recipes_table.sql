
-- ----------------------------------------------------------------------------
-- RECIPES
-- ----------------------------------------------------------------------------

CREATE TABLE recipes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               TEXT NOT NULL,
    description         TEXT,
    servings            INTEGER CHECK (servings > 0),
    prep_time_minutes   INTEGER CHECK (prep_time_minutes >= 0),
    cook_time_minutes   INTEGER CHECK (cook_time_minutes >= 0),
    total_time_minutes  INTEGER GENERATED ALWAYS AS
                            (COALESCE(prep_time_minutes, 0) + COALESCE(cook_time_minutes, 0)) STORED,
    difficulty          difficulty_level,
    cuisine             TEXT,
    image_url           TEXT,
    source_type         recipe_source_type NOT NULL DEFAULT 'manual',
    source_url          TEXT,                    -- populated when imported
    source_name         TEXT,                    -- e.g. "NYT Cooking"
    is_favorite         BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipes_user_id ON recipes(user_id);

CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- for fuzzy title search
CREATE INDEX idx_recipes_title_trgm ON recipes USING gin (title gin_trgm_ops);
