
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
