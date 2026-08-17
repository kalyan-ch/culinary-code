
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
