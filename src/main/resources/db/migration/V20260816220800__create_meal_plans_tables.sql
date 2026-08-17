
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
