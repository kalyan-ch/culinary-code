
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
