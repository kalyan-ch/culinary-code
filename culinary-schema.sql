
-- ============================================================================
-- Recipe Management App — PostgreSQL Schema
-- Features: create recipe, import recipe, meal planning, grocery list generation
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- for gen_random_uuid()

-- ----------------------------------------------------------------------------
-- ENUM TYPES
-- ----------------------------------------------------------------------------

CREATE TYPE recipe_source_type AS ENUM ('manual', 'imported');
CREATE TYPE meal_type AS ENUM ('breakfast', 'lunch', 'dinner', 'snack');
CREATE TYPE grocery_list_status AS ENUM ('active', 'completed', 'archived');
CREATE TYPE difficulty_level AS ENUM ('easy', 'medium', 'hard');

-- ----------------------------------------------------------------------------
-- USERS
-- ----------------------------------------------------------------------------

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL UNIQUE,
    display_name    TEXT NOT NULL,
    password_hash   TEXT,                       -- null if using OAuth-only
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

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
CREATE INDEX idx_recipes_title_trgm ON recipes USING gin (title gin_trgm_ops);
-- Requires: CREATE EXTENSION IF NOT EXISTS pg_trgm; (enable if you want fuzzy title search)

-- ----------------------------------------------------------------------------
-- RECIPE INGREDIENTS (join table with quantity/unit; keeps raw imported text)
-- ----------------------------------------------------------------------------

CREATE TABLE recipe_ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id   UUID REFERENCES ingredients(id) ON DELETE SET NULL, -- nullable until normalized
    quantity        NUMERIC(10,2),
    unit            TEXT,                         -- e.g. 'cup', 'g', 'tbsp'
    notes           TEXT,                         -- e.g. "finely chopped"
    raw_text        TEXT,                         -- original line, esp. useful for imports
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id);
CREATE INDEX idx_recipe_ingredients_ingredient_id ON recipe_ingredients(ingredient_id);

-- ----------------------------------------------------------------------------
-- RECIPE STEPS (instructions)
-- ----------------------------------------------------------------------------

CREATE TABLE recipe_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_number     INTEGER NOT NULL,
    instruction     TEXT NOT NULL,
    timer_minutes   INTEGER,                      -- optional built-in timer
    UNIQUE (recipe_id, step_number)
);

CREATE INDEX idx_recipe_steps_recipe_id ON recipe_steps(recipe_id);

-- ----------------------------------------------------------------------------
-- TAGS (e.g. "vegetarian", "quick", "dessert")
-- ----------------------------------------------------------------------------

CREATE TABLE tags (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name    TEXT NOT NULL UNIQUE
);

CREATE TABLE recipe_tags (
    recipe_id   UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, tag_id)
);

-- ----------------------------------------------------------------------------
-- MEAL PLANS
-- ----------------------------------------------------------------------------

CREATE TABLE meal_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,                -- e.g. "Week of Aug 11"
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date >= start_date)
);

CREATE INDEX idx_meal_plans_user_id ON meal_plans(user_id);

CREATE TABLE meal_plan_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id        UUID NOT NULL REFERENCES meal_plans(id) ON DELETE CASCADE,
    recipe_id           UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    planned_date         DATE NOT NULL,
    meal_type            meal_type NOT NULL,
    servings_planned      INTEGER NOT NULL DEFAULT 1 CHECK (servings_planned > 0),
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_meal_plan_entries_plan_id ON meal_plan_entries(meal_plan_id);
CREATE INDEX idx_meal_plan_entries_recipe_id ON meal_plan_entries(recipe_id);
CREATE INDEX idx_meal_plan_entries_date ON meal_plan_entries(planned_date);

-- ----------------------------------------------------------------------------
-- GROCERY LISTS (can be generated from a meal plan, or created standalone)
-- ----------------------------------------------------------------------------

CREATE TABLE grocery_lists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    meal_plan_id    UUID REFERENCES meal_plans(id) ON DELETE SET NULL,  -- null if manually created
    name            TEXT NOT NULL,
    status          grocery_list_status NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_grocery_lists_user_id ON grocery_lists(user_id);
CREATE INDEX idx_grocery_lists_meal_plan_id ON grocery_lists(meal_plan_id);

CREATE TABLE grocery_list_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grocery_list_id     UUID NOT NULL REFERENCES grocery_lists(id) ON DELETE CASCADE,
    ingredient_id       UUID REFERENCES ingredients(id) ON DELETE SET NULL, -- null for freeform items
    custom_item_name    TEXT,                     -- used when not tied to a normalized ingredient
    quantity            NUMERIC(10,2),
    unit                TEXT,
    category            TEXT,                     -- aisle/category, copied from ingredient at generation time
    is_checked          BOOLEAN NOT NULL DEFAULT false,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ingredient_id IS NOT NULL OR custom_item_name IS NOT NULL)
);

CREATE INDEX idx_grocery_list_items_list_id ON grocery_list_items(grocery_list_id);
CREATE INDEX idx_grocery_list_items_ingredient_id ON grocery_list_items(ingredient_id);

-- Provenance: which recipe_ingredients were merged into a given grocery list item
-- (lets you show "from: Chicken Tikka Masala, Butter Chicken" and recompute if a
--  meal plan entry changes servings or is removed)
CREATE TABLE grocery_list_item_sources (
    grocery_list_item_id   UUID NOT NULL REFERENCES grocery_list_items(id) ON DELETE CASCADE,
    recipe_ingredient_id    UUID NOT NULL REFERENCES recipe_ingredients(id) ON DELETE CASCADE,
    meal_plan_entry_id       UUID REFERENCES meal_plan_entries(id) ON DELETE CASCADE,
    contributed_quantity     NUMERIC(10,2),        -- quantity scaled for planned servings
    PRIMARY KEY (grocery_list_item_id, recipe_ingredient_id, meal_plan_entry_id)
);

-- ----------------------------------------------------------------------------
-- updated_at TRIGGER (generic helper applied to a few tables)
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_recipes_updated_at
    BEFORE UPDATE ON recipes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_meal_plans_updated_at
    BEFORE UPDATE ON meal_plans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_grocery_lists_updated_at
    BEFORE UPDATE ON grocery_lists
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- Notes on how each feature maps to this schema
-- ============================================================================
-- Create a recipe:        INSERT into recipes, recipe_ingredients, recipe_steps
--                          (source_type = 'manual')
-- Import a recipe:        Parse the source (URL/paste) in the application layer, then
--                          INSERT directly into recipes/recipe_ingredients/recipe_steps
--                          (source_type = 'imported', source_url/source_name populated)
-- Create a meal plan:     INSERT into meal_plans, then meal_plan_entries linking
--                          recipes to dates/meal_type
-- Grocery list from plan: INSERT grocery_lists (meal_plan_id = X), then aggregate
--                          recipe_ingredients across all meal_plan_entries for that
--                          plan (scaled by servings_planned / recipe.servings),
--                          group by ingredient_id, INSERT grocery_list_items,
--                          and record grocery_list_item_sources for provenance.
-- ============================================================================