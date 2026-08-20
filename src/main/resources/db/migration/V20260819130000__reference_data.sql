-- ----------------------------------------------------------------------------
-- SHARED REFERENCE DATA
--
-- Ingredients and tags are reference data rather than anyone's property, so this runs
-- in every environment (unlike the local-only sample data that used to live in db/seed).
-- An empty ingredient catalogue makes grocery-list merging and aisle grouping useless.
--
-- Both tables gain an owner column with the same meaning: a NULL user_id marks a curated
-- row everyone shares, and a populated one marks something a user invented for themselves.
-- Only a migration ever writes curated rows; the service always creates owned ones.
-- ----------------------------------------------------------------------------

ALTER TABLE tags ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE ingredients ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE CASCADE;

-- The old constraints were UNIQUE(name) across the whole table, which would stop two
-- different users each having a "weeknight" tag or a "gochujang" of their own.
ALTER TABLE tags DROP CONSTRAINT tags_name_key;
ALTER TABLE ingredients DROP CONSTRAINT ingredients_name_key;

CREATE UNIQUE INDEX idx_tags_common_name ON tags (lower(name)) WHERE user_id IS NULL;
CREATE UNIQUE INDEX idx_tags_user_name ON tags (user_id, lower(name)) WHERE user_id IS NOT NULL;
CREATE INDEX idx_tags_user_id ON tags (user_id) WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX idx_ingredients_common_name ON ingredients (lower(name)) WHERE user_id IS NULL;
CREATE UNIQUE INDEX idx_ingredients_user_name ON ingredients (user_id, lower(name)) WHERE user_id IS NOT NULL;
CREATE INDEX idx_ingredients_user_id ON ingredients (user_id) WHERE user_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Curated tags
-- ---------------------------------------------------------------------------

INSERT INTO tags (name, user_id) VALUES
    -- dietary
    ('vegetarian', NULL), ('vegan', NULL), ('pescatarian', NULL), ('gluten-free', NULL),
    ('dairy-free', NULL), ('nut-free', NULL), ('egg-free', NULL), ('low-carb', NULL),
    ('keto', NULL), ('high-protein', NULL), ('low-calorie', NULL), ('halal', NULL),
    -- course
    ('breakfast', NULL), ('brunch', NULL), ('lunch', NULL), ('dinner', NULL),
    ('appetizer', NULL), ('side', NULL), ('salad', NULL), ('soup', NULL),
    ('dessert', NULL), ('snack', NULL), ('drink', NULL), ('sauce', NULL),
    ('bread', NULL), ('curry', NULL), ('stew', NULL),
    -- effort and method
    ('quick', NULL), ('one-pot', NULL), ('no-cook', NULL), ('make-ahead', NULL),
    ('freezer-friendly', NULL), ('slow-cooker', NULL), ('air-fryer', NULL),
    ('grilled', NULL), ('baked', NULL), ('fried', NULL), ('leftovers', NULL),
    ('batch-cook', NULL),
    -- occasion
    ('weeknight', NULL), ('comfort-food', NULL), ('healthy', NULL), ('budget', NULL),
    ('party', NULL), ('kid-friendly', NULL), ('date-night', NULL), ('festive', NULL)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Curated ingredients
--
-- Names are canonical and lowercase; find-or-create matches case-insensitively, so a
-- user typing "Tomatoes" reuses the "tomato" row instead of inventing a duplicate.
-- Coverage is deliberately global rather than Anglo-centric — the cuisine list in the
-- UI spans Indian, West African, Caribbean, East and South-East Asian cooking.
-- ---------------------------------------------------------------------------

INSERT INTO ingredients (name, category, default_unit, user_id) VALUES
    -- alliums, roots and staples
    ('onion','produce','each',NULL), ('red onion','produce','each',NULL),
    ('spring onion','produce','each',NULL), ('shallot','produce','each',NULL),
    ('garlic','produce','clove',NULL), ('ginger','produce','g',NULL),
    ('potato','produce','each',NULL), ('sweet potato','produce','each',NULL),
    ('yam','produce','g',NULL), ('cassava','produce','g',NULL),
    ('plantain','produce','each',NULL), ('carrot','produce','each',NULL),
    ('beetroot','produce','each',NULL), ('parsnip','produce','each',NULL),
    ('turnip','produce','each',NULL), ('radish','produce','each',NULL),
    -- vegetables
    ('tomato','produce','each',NULL), ('cherry tomato','produce','g',NULL),
    ('bell pepper','produce','each',NULL), ('chilli','produce','each',NULL),
    ('scotch bonnet','produce','each',NULL), ('jalapeno','produce','each',NULL),
    ('cucumber','produce','each',NULL), ('courgette','produce','each',NULL),
    ('aubergine','produce','each',NULL), ('okra','produce','g',NULL),
    ('mushroom','produce','g',NULL), ('broccoli','produce','g',NULL),
    ('cauliflower','produce','g',NULL), ('cabbage','produce','g',NULL),
    ('spinach','produce','g',NULL), ('kale','produce','g',NULL),
    ('lettuce','produce','each',NULL), ('pak choi','produce','g',NULL),
    ('bean sprouts','produce','g',NULL), ('green beans','produce','g',NULL),
    ('peas','produce','g',NULL), ('sweetcorn','produce','g',NULL),
    ('leek','produce','each',NULL), ('celery','produce','stalk',NULL),
    ('fennel','produce','each',NULL), ('asparagus','produce','g',NULL),
    ('brussels sprouts','produce','g',NULL), ('butternut squash','produce','g',NULL),
    ('pumpkin','produce','g',NULL),
    -- fruit
    ('avocado','produce','each',NULL), ('lemon','produce','each',NULL),
    ('lime','produce','each',NULL), ('orange','produce','each',NULL),
    ('apple','produce','each',NULL), ('banana','produce','each',NULL),
    ('mango','produce','each',NULL), ('pineapple','produce','each',NULL),
    ('pomegranate','produce','each',NULL), ('dates','produce','g',NULL),
    ('raisins','pantry','g',NULL), ('coconut','produce','each',NULL),
    -- fresh herbs and aromatics
    ('basil','produce','g',NULL), ('thai basil','produce','g',NULL),
    ('coriander','produce','g',NULL), ('parsley','produce','g',NULL),
    ('mint','produce','g',NULL), ('dill','produce','g',NULL),
    ('chives','produce','g',NULL), ('sage','produce','g',NULL),
    ('rosemary','produce','g',NULL), ('thyme','produce','g',NULL),
    ('curry leaves','produce','g',NULL), ('lemongrass','produce','stalk',NULL),
    ('galangal','produce','g',NULL), ('kaffir lime leaves','produce','each',NULL),
    -- meat
    ('chicken breast','meat','each',NULL), ('chicken thigh','meat','each',NULL),
    ('whole chicken','meat','each',NULL), ('chicken wings','meat','g',NULL),
    ('beef mince','meat','g',NULL), ('beef steak','meat','g',NULL),
    ('stewing beef','meat','g',NULL), ('beef short rib','meat','g',NULL),
    ('pork shoulder','meat','g',NULL), ('pork chop','meat','each',NULL),
    ('pork belly','meat','g',NULL), ('lamb mince','meat','g',NULL),
    ('lamb shoulder','meat','g',NULL), ('goat meat','meat','g',NULL),
    ('bacon','meat','g',NULL), ('pancetta','meat','g',NULL),
    ('chorizo','meat','g',NULL), ('sausage','meat','each',NULL),
    ('ham','meat','g',NULL), ('duck breast','meat','each',NULL),
    ('turkey mince','meat','g',NULL),
    -- seafood
    ('salmon fillet','seafood','each',NULL), ('white fish fillet','seafood','each',NULL),
    ('cod','seafood','g',NULL), ('tilapia','seafood','each',NULL),
    ('mackerel','seafood','each',NULL), ('tuna','seafood','g',NULL),
    ('prawns','seafood','g',NULL), ('mussels','seafood','g',NULL),
    ('squid','seafood','g',NULL), ('crab','seafood','g',NULL),
    ('anchovies','seafood','g',NULL), ('smoked salmon','seafood','g',NULL),
    -- dairy and chilled
    ('milk','dairy','ml',NULL), ('double cream','dairy','ml',NULL),
    ('single cream','dairy','ml',NULL), ('soured cream','dairy','ml',NULL),
    ('greek yogurt','dairy','g',NULL), ('natural yogurt','dairy','g',NULL),
    ('butter','dairy','g',NULL), ('ghee','dairy','g',NULL),
    ('cheddar cheese','dairy','g',NULL), ('parmesan cheese','dairy','g',NULL),
    ('pecorino','dairy','g',NULL), ('mozzarella','dairy','g',NULL),
    ('feta cheese','dairy','g',NULL), ('halloumi','dairy','g',NULL),
    ('paneer','dairy','g',NULL), ('ricotta','dairy','g',NULL),
    ('mascarpone','dairy','g',NULL), ('cream cheese','dairy','g',NULL),
    ('eggs','dairy','each',NULL), ('tofu','chilled','g',NULL),
    ('tempeh','chilled','g',NULL),
    -- rice, grains, pasta, noodles
    ('basmati rice','pantry','g',NULL), ('jasmine rice','pantry','g',NULL),
    ('long grain rice','pantry','g',NULL), ('brown rice','pantry','g',NULL),
    ('arborio rice','pantry','g',NULL), ('spaghetti','pantry','g',NULL),
    ('penne','pantry','g',NULL), ('macaroni','pantry','g',NULL),
    ('lasagne sheets','pantry','g',NULL), ('rice noodles','pantry','g',NULL),
    ('egg noodles','pantry','g',NULL), ('udon noodles','pantry','g',NULL),
    ('couscous','pantry','g',NULL), ('bulgur wheat','pantry','g',NULL),
    ('quinoa','pantry','g',NULL), ('rolled oats','pantry','g',NULL),
    ('semolina','pantry','g',NULL), ('polenta','pantry','g',NULL),
    -- flours and baking
    ('plain flour','pantry','g',NULL), ('self-raising flour','pantry','g',NULL),
    ('bread flour','pantry','g',NULL), ('chickpea flour','pantry','g',NULL),
    ('rice flour','pantry','g',NULL), ('cornflour','pantry','tbsp',NULL),
    ('breadcrumbs','pantry','g',NULL), ('panko breadcrumbs','pantry','g',NULL),
    ('baking powder','pantry','tsp',NULL), ('bicarbonate of soda','pantry','tsp',NULL),
    ('dried yeast','pantry','g',NULL), ('cocoa powder','pantry','g',NULL),
    ('dark chocolate','pantry','g',NULL), ('desiccated coconut','pantry','g',NULL),
    -- pulses
    ('chickpeas','pantry','g',NULL), ('black beans','pantry','g',NULL),
    ('kidney beans','pantry','g',NULL), ('cannellini beans','pantry','g',NULL),
    ('black eyed beans','pantry','g',NULL), ('red lentils','pantry','g',NULL),
    ('green lentils','pantry','g',NULL), ('puy lentils','pantry','g',NULL),
    ('toor dal','pantry','g',NULL), ('chana dal','pantry','g',NULL),
    ('urad dal','pantry','g',NULL), ('split peas','pantry','g',NULL),
    -- tins, stocks, oils, sweeteners
    ('chopped tomatoes','pantry','g',NULL), ('tomato puree','pantry','g',NULL),
    ('passata','pantry','ml',NULL), ('sun-dried tomatoes','pantry','g',NULL),
    ('coconut milk','pantry','ml',NULL), ('coconut cream','pantry','ml',NULL),
    ('chicken stock','pantry','ml',NULL), ('vegetable stock','pantry','ml',NULL),
    ('beef stock','pantry','ml',NULL), ('olive oil','pantry','ml',NULL),
    ('vegetable oil','pantry','ml',NULL), ('sunflower oil','pantry','ml',NULL),
    ('sesame oil','pantry','ml',NULL), ('coconut oil','pantry','ml',NULL),
    ('palm oil','pantry','ml',NULL), ('mustard oil','pantry','ml',NULL),
    ('caster sugar','pantry','g',NULL), ('brown sugar','pantry','g',NULL),
    ('icing sugar','pantry','g',NULL), ('jaggery','pantry','g',NULL),
    ('honey','pantry','g',NULL), ('maple syrup','pantry','ml',NULL),
    ('golden syrup','pantry','ml',NULL), ('vanilla extract','pantry','tsp',NULL),
    -- nuts and seeds
    ('almonds','pantry','g',NULL), ('cashews','pantry','g',NULL),
    ('peanuts','pantry','g',NULL), ('walnuts','pantry','g',NULL),
    ('pine nuts','pantry','g',NULL), ('peanut butter','pantry','g',NULL),
    ('sesame seeds','pantry','g',NULL), ('pumpkin seeds','pantry','g',NULL),
    ('sunflower seeds','pantry','g',NULL), ('chia seeds','pantry','g',NULL),
    -- condiments and sauces
    ('soy sauce','condiment','ml',NULL), ('dark soy sauce','condiment','ml',NULL),
    ('tamari','condiment','ml',NULL), ('fish sauce','condiment','ml',NULL),
    ('oyster sauce','condiment','ml',NULL), ('hoisin sauce','condiment','ml',NULL),
    ('worcestershire sauce','condiment','ml',NULL), ('balsamic vinegar','condiment','ml',NULL),
    ('red wine vinegar','condiment','ml',NULL), ('white wine vinegar','condiment','ml',NULL),
    ('rice vinegar','condiment','ml',NULL), ('apple cider vinegar','condiment','ml',NULL),
    ('dijon mustard','condiment','tsp',NULL), ('wholegrain mustard','condiment','tsp',NULL),
    ('english mustard','condiment','tsp',NULL), ('mayonnaise','condiment','g',NULL),
    ('ketchup','condiment','g',NULL), ('sriracha','condiment','ml',NULL),
    ('hot sauce','condiment','ml',NULL), ('gochujang','condiment','g',NULL),
    ('miso paste','condiment','g',NULL), ('mirin','condiment','ml',NULL),
    ('harissa','condiment','g',NULL), ('tahini','condiment','g',NULL),
    ('tamarind paste','condiment','g',NULL), ('pomegranate molasses','condiment','ml',NULL),
    ('preserved lemon','condiment','each',NULL), ('capers','condiment','g',NULL),
    ('olives','condiment','g',NULL), ('nori','pantry','sheet',NULL),
    -- spices
    ('salt','spice','tsp',NULL), ('black pepper','spice','tsp',NULL),
    ('white pepper','spice','tsp',NULL), ('cumin','spice','tsp',NULL),
    ('cumin seeds','spice','tsp',NULL), ('ground coriander','spice','tsp',NULL),
    ('coriander seeds','spice','tsp',NULL), ('turmeric','spice','tsp',NULL),
    ('paprika','spice','tsp',NULL), ('smoked paprika','spice','tsp',NULL),
    ('cayenne pepper','spice','tsp',NULL), ('chilli powder','spice','tsp',NULL),
    ('chilli flakes','spice','tsp',NULL), ('kashmiri chilli powder','spice','tsp',NULL),
    ('garam masala','spice','tsp',NULL), ('curry powder','spice','tsp',NULL),
    ('tandoori masala','spice','tsp',NULL), ('chaat masala','spice','tsp',NULL),
    ('amchur','spice','tsp',NULL), ('fenugreek seeds','spice','tsp',NULL),
    ('fenugreek leaves','spice','tsp',NULL), ('mustard seeds','spice','tsp',NULL),
    ('nigella seeds','spice','tsp',NULL), ('asafoetida','spice','tsp',NULL),
    ('cinnamon','spice','tsp',NULL), ('cinnamon stick','spice','each',NULL),
    ('nutmeg','spice','tsp',NULL), ('cloves','spice','each',NULL),
    ('cardamom','spice','each',NULL), ('star anise','spice','each',NULL),
    ('bay leaf','spice','each',NULL), ('allspice','spice','tsp',NULL),
    ('saffron','spice','g',NULL), ('oregano','spice','tsp',NULL),
    ('dried thyme','spice','tsp',NULL), ('mixed herbs','spice','tsp',NULL),
    ('za''atar','spice','tsp',NULL), ('sumac','spice','tsp',NULL),
    ('ras el hanout','spice','tsp',NULL), ('berbere','spice','tsp',NULL),
    ('jerk seasoning','spice','tsp',NULL), ('chinese five spice','spice','tsp',NULL),
    ('szechuan peppercorns','spice','tsp',NULL),
    -- bakery and frozen
    ('bread','bakery','each',NULL), ('sourdough','bakery','each',NULL),
    ('baguette','bakery','each',NULL), ('tortilla wraps','bakery','each',NULL),
    ('pitta bread','bakery','each',NULL), ('naan','bakery','each',NULL),
    ('puff pastry','frozen','g',NULL), ('filo pastry','frozen','g',NULL),
    ('shortcrust pastry','frozen','g',NULL), ('frozen peas','frozen','g',NULL),
    ('frozen spinach','frozen','g',NULL), ('frozen berries','frozen','g',NULL)
ON CONFLICT DO NOTHING;
