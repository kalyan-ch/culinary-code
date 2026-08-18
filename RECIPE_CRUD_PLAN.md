# Recipe CRUD — Combined Backend + UI Plan

Status: **backend done and verified** (2026-08-17). UI phase not started — resume there next.

## Backend implementation notes
All 6 backend tasks below are implemented and manually verified end-to-end (create/read/list/update/delete, validation, 404s, ingredient dedup, cascade delete). Several real bugs surfaced only when actually exercising the endpoints against Postgres, not from code review alone:

- **ModelMapper mis-mapped `userId` onto the entity's `id`.** `modelMapper.map(request, Recipe.class)` matched `RecipeCreateRequest.userId` to *both* `Recipe.userId` and `Recipe.id` (its default STANDARD matching treats "id" as a token-suffix match of "userId" when the destination has no exact-name source to match against). This made every create silently try to `UPDATE` a row using the submitted `userId` as the recipe's id instead of `INSERT`ing a new one. Fixed by building the `Recipe` explicitly in `createRecipe` instead of via reflection-based mapping.
- **`source_type` and `sort_order` are `NOT NULL` in Postgres with defaults, but Hibernate always sends explicit values for columns that aren't `insertable=false`**, overriding the DB default with `NULL` when the Java field is unset. Fixed by setting `sourceType = manual` explicitly on create, and `sortOrder` from each ingredient's list index.
- **Ingredient case-insensitive dedup raced itself within one request.** Resolving two differently-cased spellings of the same ingredient name (e.g. "flour" then "Flour") in the same create call returned two different `Ingredient` rows — the second lookup ran before the first `save()` was visible. Fixed with a per-request resolution cache (`Map<String, Ingredient>` keyed by lowercased name) so repeats within one request never hit the DB twice.
- **Update violated `recipe_steps`' `UNIQUE(recipe_id, step_number)` constraint.** Clearing and re-adding the `steps` collection in one flush could insert the new `step_number=1` before the old row's delete had landed. Fixed by flushing the removal (`saveAndFlush` after `.clear()`) before adding the new children.
- **The generic exception handler was swallowing errors with no server-side logging**, which made all of the above nearly undiagnosable from the API response alone (`{"message":"An unexpected error occurred"}`). Added `log.error(...)` to the catch-all handler.

## Decisions already made
- **Ingredient resolution**: on create/update, look up ingredient by name (case-insensitive); auto-create it if it doesn't exist yet. No separate ingredient-management CRUD needed for this phase.
- **Update strategy for nested collections**: full replace (delete-then-recreate `recipeIngredients`/`steps` on update) rather than diffing — simplest correct approach given `orphanRemoval = true` is already set on both `@OneToMany`s.
- **Users**: out of scope for this phase. `RecipeCreateRequest.userId` remains a required field with no UI-side source yet (no auth, no user picker) — revisit once auth exists. Until then, exercise create via API client/Postman/Swagger with a manually supplied seeded user id (e.g. `00000000-0000-0000-0000-000000000001`).

---

## Backend (`culinary-code`) — ✅ done

### Original state (before this phase)
- `POST /api/v1/recipe/create` exists but is **broken** for nested data: `RecipeIngredient`/`RecipeStep` are `@ManyToOne` children with `recipe_id NOT NULL` in the DB. `RecipeService.createRecipe` maps the whole request via ModelMapper, which builds the child objects and puts them in `recipe.recipeIngredients`/`recipe.steps` but never sets `child.recipe` back, and never resolves `RecipeIngredient.ingredient` from the DTO's ingredient name. Saving as-is will violate the `recipe_id NOT NULL` constraint.
- It also returns `204 No Content` with no way to know the new recipe's id.
- `GET /{recipeId}` (single) and `GET /recipes?recipeIds=` (batch by id) exist and work.
- No "list/browse all" endpoint, no update, no delete.
- No validation, no `@ControllerAdvice`/global error handling — bad requests currently surface as raw 500s.

### Tasks (all ✅ done)
1. **Fix Create** (`RecipeService.createRecipe`)
   - Map scalar fields (`title`, `description`, `servings`, times, `cuisine`, `imageUrl`) via ModelMapper as today.
   - For each `IngredientsDTO`: resolve `Ingredient` via `findByName` (case-insensitive), create if missing; build `RecipeIngredient` with `.recipe(recipe)` set explicitly.
   - For each `RecipeStepDTO`: build `RecipeStep` with `.recipe(recipe)` set explicitly; assign `stepNumber` from list order if not provided.
   - Save via `recipeRepository.save(recipe)`.
   - Change controller to return `201 Created` with the created `RecipeDetailDTO` and a `Location: /api/v1/recipe/{id}` header, instead of `204` with nothing.

2. **Add "list recipes"** (Read - browse)
   - `GET /api/v1/recipe` with `Pageable` (page/size/sort), optional `userId` and `cuisine` query filters.
   - `RecipeRepository` gets derived-query methods (`findByUserId`, `findByCuisine`) or a `Specification` if filters need to combine.
   - Returns `Page<RecipeDTO>` using the existing `RecipeDTO`.

3. **Add Update**
   - `PUT /api/v1/recipe/{recipeId}` with a new `RecipeUpdateRequest` (same shape as `RecipeCreateRequest` minus `userId`).
   - Full replace of `recipeIngredients`/`steps` (clear + rebuild the same way as Create).
   - 404 if the id doesn't exist.

4. **Add Delete**
   - `DELETE /api/v1/recipe/{recipeId}` → `recipeRepository.deleteById`. DB `ON DELETE CASCADE` already cleans up `recipe_ingredients`, `recipe_steps`, `recipe_tags`.
   - 404 if the id doesn't exist, `204` on success.

5. **Validation**
   - Add `jakarta.validation` annotations to `RecipeCreateRequest`/`RecipeUpdateRequest` mirroring the DB `CHECK` constraints: `@NotBlank title`, `@Positive servings`, `@PositiveOrZero` on prep/cook time, `@NotNull userId` (create only). Add `@Valid` on controller method params.

6. **Error handling**
   - Add `GlobalExceptionHandler` (`@RestControllerAdvice`): bean-validation failures → 400 with field errors, "not found" → 404 with a message, everything else → 500 with a generic body.

### Files touched
- `RecipeController.java` — add list/update/delete endpoints, fix create's response.
- `RecipeService.java` — rewrite `createRecipe`, add `updateRecipe`, `deleteRecipe`, `listRecipes`.
- `RecipeRepository.java` — add filter/paging query methods.
- New: `RecipeUpdateRequest.java`, `GlobalExceptionHandler.java`.
- `IngredientRepository` — reused as-is for find-or-create lookup.

---

## Frontend (`culinary-code-ui`)

### Current state
- No API client/fetch layer exists anywhere in `src/` — zero `fetch`/`axios` calls today.
- `CreateRecipeForm` collects `recipeName`, `preparation`, `method`, `notes`, `cuisines` (array via `MultiSelect`) — none of these match the backend's `title`, `steps[]`, single `cuisine`.
- `IngredientTable`'s unit `<select>` has no `name` attribute, so it wouldn't even POST today.
- No recipe list, detail, or edit pages exist — only the create form.
- `Sidebar` (cuisine filter checkboxes) is built but unused/unrendered.

### Tasks
1. **API client layer** (new)
   - `src/lib/api/client.ts` — thin fetch wrapper reading base URL from `NEXT_PUBLIC_API_URL` (defaults to `http://localhost:8090`), handling JSON parsing and non-2xx errors.
   - `src/lib/api/types.ts` — TS interfaces mirroring the backend DTOs: `RecipeDTO`, `RecipeDetailDTO`, `RecipeCreateRequest`/`RecipeUpdateRequest`, `IngredientsDTO`, `RecipeStepDTO` (`id: string` for UUIDs, `cuisine: string`, `title: string`, `steps: RecipeStepDTO[]`).
   - `src/lib/api/recipes.ts` — typed functions: `listRecipes()`, `getRecipe(id)`, `createRecipe(body)`, `updateRecipe(id, body)`, `deleteRecipe(id)`.

2. **Fix `CreateRecipeForm`**
   - Rename `recipeName` → `title`.
   - Replace the `cuisines` `MultiSelect` with a single cuisine text input/select.
   - Replace `preparation`/`method`/`notes` free-text blobs with a **Steps** section: a repeatable list of ordered instruction inputs, producing `RecipeStepDTO[]`.
   - Fix `IngredientTable`'s unit `<select>` to actually have a `name` attribute.
   - Wire the real submit handler to call `createRecipe()`, then redirect to `/recipes/[id]` using the id from the `201` response.
   - `userId`: leave as a manually-set placeholder/constant for now (see "Decisions already made" — no user picker in this phase).

3. **Recipe list page** (`/recipes`)
   - Calls `listRecipes()`; renders cards (title, description, cuisine, prep/cook time).
   - Reuses `Sidebar`'s cuisine checkboxes as an actual filter wired to query params, instead of leaving it unrendered.

4. **Recipe detail page** (`/recipes/[id]`)
   - Calls `getRecipe(id)`; renders title/description/servings/times, ingredient list, ordered steps.
   - Edit and Delete buttons.

5. **Recipe edit page** (`/recipes/[id]/edit`)
   - Reuses `CreateRecipeForm` in "edit mode" (prefilled via `getRecipe(id)`, submits `updateRecipe` instead of `createRecipe`).

6. **Delete flow**
   - Confirm dialog on detail/list page → `deleteRecipe(id)` → redirect to `/recipes`.

---

## Suggested implementation order
1. ✅ Backend: fix Create (currently broken) — tasks 1, 5, 6.
2. ✅ Backend: list/update/delete — tasks 2, 3, 4.
3. ⬅️ **Resume here** — UI: API client layer.
4. UI: fix `CreateRecipeForm` + wire real submit.
5. UI: list page, detail page.
6. UI: edit page, delete flow.
