
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
