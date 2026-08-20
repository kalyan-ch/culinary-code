-- ----------------------------------------------------------------------------
-- RECIPE VISIBILITY
--
-- Recipes are private to their owner by default. A user can publish one, which
-- makes it visible to every signed-in account in the shared browse view.
-- ----------------------------------------------------------------------------

ALTER TABLE recipes ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT false;

-- Partial index: the public browse query only ever looks for is_public = true, and
-- the published rows are expected to be a small fraction of the table.
CREATE INDEX idx_recipes_is_public ON recipes(is_public) WHERE is_public;
