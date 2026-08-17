
-- ----------------------------------------------------------------------------
-- Local-only seed data. Applied only when spring.flyway.locations includes
-- this folder (see application-local.yml) — never runs against prod.
-- ----------------------------------------------------------------------------

-- USERS
INSERT INTO users (id, email, display_name, password_hash) VALUES
    ('00000000-0000-0000-0000-000000000001', 'alice@example.com', 'Alice Baker', 'not-a-real-hash'),
    ('00000000-0000-0000-0000-000000000002', 'bob@example.com',   'Bob Cook',    'not-a-real-hash');

-- INGREDIENTS
INSERT INTO ingredients (id, name, category, default_unit) VALUES
    ('10000000-0000-0000-0000-000000000001', 'spaghetti',       'pantry',  'g'),
    ('10000000-0000-0000-0000-000000000002', 'garlic',          'produce', 'clove'),
    ('10000000-0000-0000-0000-000000000003', 'olive oil',       'pantry',  'ml'),
    ('10000000-0000-0000-0000-000000000004', 'parmesan cheese', 'dairy',   'g'),
    ('10000000-0000-0000-0000-000000000005', 'black pepper',    'pantry',  'g'),
    ('10000000-0000-0000-0000-000000000006', 'eggs',            'dairy',   'each'),
    ('10000000-0000-0000-0000-000000000007', 'pancetta',        'meat',    'g'),
    ('10000000-0000-0000-0000-000000000008', 'chicken breast',  'meat',    'g'),
    ('10000000-0000-0000-0000-000000000009', 'basil',           'produce', 'g'),
    ('10000000-0000-0000-0000-000000000010', 'tomato',          'produce', 'each');

-- TAGS
INSERT INTO tags (id, name) VALUES
    ('20000000-0000-0000-0000-000000000001', 'quick'),
    ('20000000-0000-0000-0000-000000000002', 'vegetarian'),
    ('20000000-0000-0000-0000-000000000003', 'comfort-food');

-- RECIPE 1: Spaghetti Carbonara
INSERT INTO recipes (id, user_id, title, description, servings, prep_time_minutes, cook_time_minutes, difficulty, cuisine, source_type) VALUES
    ('30000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
     'Spaghetti Carbonara', 'Classic Roman pasta with pancetta, egg, and parmesan.',
     4, 10, 15, 'medium', 'Italian', 'manual');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, sort_order) VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 400, 'g',    0),
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000007', 150, 'g',    1),
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000006', 3,   'each', 2),
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', 50,  'g',    3),
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005', 2,   'g',    4);

INSERT INTO recipe_steps (recipe_id, step_number, instruction) VALUES
    ('30000000-0000-0000-0000-000000000001', 1, 'Bring a large pot of salted water to a boil and cook the spaghetti until al dente.'),
    ('30000000-0000-0000-0000-000000000001', 2, 'While the pasta cooks, fry the pancetta in a pan until crisp.'),
    ('30000000-0000-0000-0000-000000000001', 3, 'Whisk the eggs and parmesan together in a bowl with plenty of black pepper.'),
    ('30000000-0000-0000-0000-000000000001', 4, 'Off the heat, toss the drained pasta with the pancetta, then the egg mixture, until creamy.');

INSERT INTO recipe_tags (recipe_id, tag_id) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003');

-- RECIPE 2: Garlic Butter Chicken
INSERT INTO recipes (id, user_id, title, description, servings, prep_time_minutes, cook_time_minutes, difficulty, cuisine, source_type) VALUES
    ('30000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
     'Garlic Butter Chicken', 'Pan-seared chicken breast finished in a garlicky butter sauce.',
     2, 10, 20, 'easy', 'American', 'manual');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, sort_order) VALUES
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000008', 2,  'each',  0),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 4,  'clove', 1),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', 30, 'ml',    2),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005', 2,  'g',     3);

INSERT INTO recipe_steps (recipe_id, step_number, instruction, timer_minutes) VALUES
    ('30000000-0000-0000-0000-000000000002', 1, 'Season the chicken breasts and sear in olive oil until golden, about 6 minutes per side.', 12),
    ('30000000-0000-0000-0000-000000000002', 2, 'Add minced garlic and butter to the pan and baste the chicken for 2 minutes.', 2),
    ('30000000-0000-0000-0000-000000000002', 3, 'Rest the chicken for a few minutes before slicing and serving.', NULL);

INSERT INTO recipe_tags (recipe_id, tag_id) VALUES
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001');

-- RECIPE 3: Caprese Salad
INSERT INTO recipes (id, user_id, title, description, servings, prep_time_minutes, cook_time_minutes, difficulty, cuisine, source_type, is_favorite) VALUES
    ('30000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001',
     'Caprese Salad', 'Simple salad of tomato, basil, and olive oil.',
     2, 10, 0, 'easy', 'Italian', 'manual', true);

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, sort_order) VALUES
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000010', 3,  'each', 0),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000009', 10, 'g',    1),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 20, 'ml',   2);

INSERT INTO recipe_steps (recipe_id, step_number, instruction) VALUES
    ('30000000-0000-0000-0000-000000000003', 1, 'Slice the tomatoes and arrange on a plate with torn basil leaves.'),
    ('30000000-0000-0000-0000-000000000003', 2, 'Drizzle with olive oil and season with salt and pepper.');

INSERT INTO recipe_tags (recipe_id, tag_id) VALUES
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001');
