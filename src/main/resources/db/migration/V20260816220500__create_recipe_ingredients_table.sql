
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
