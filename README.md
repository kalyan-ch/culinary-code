# CulinaryCode

A Spring Boot REST API for managing recipes, ingredients, and users. Built with Java 21, Spring Boot 3.4, and PostgreSQL, using Flyway for schema migrations.

## Tech Stack

- **Language / Runtime:** Java 21
- **Framework:** Spring Boot 3.4.2 (Web, Data JPA)
- **Database:** PostgreSQL 16 (via Docker), schema-managed with Flyway
- **Build tool:** Gradle (wrapper included)
- **Mapping:** ModelMapper (entity ↔ DTO mapping)
- **API docs:** springdoc-openapi (Swagger UI)
- **Boilerplate reduction:** Lombok

## Project Structure

```
src/main/java/com/wb/culinaryCode/
├── CulinaryCodeApplication.java     # Spring Boot entry point
├── config/
│   └── CulinaryCodeConfig.java      # ModelMapper bean + custom entity→DTO mapping
├── controller/
│   ├── RecipeController.java        # /api/v1/recipe endpoints
│   └── auth/
│       └── UserAuthenticationController.java  # /api/v1/login (stub)
├── dao/
│   ├── RecipeRepository.java        # Spring Data JPA repository
│   └── IngredientRepository.java
├── model/recipe/
│   ├── Recipe.java                  # Recipe entity
│   ├── Ingredient.java              # Ingredient entity
│   ├── RecipeIngredient.java        # Join entity (recipe ↔ ingredient, with qty/unit/notes)
│   ├── RecipeUser.java              # User entity
│   └── rest/                        # DTOs: RecipeDTO, RecipeDetailDTO, RecipeCreateRequest, IngredientsDTO
└── service/
    └── RecipeService.java           # Business logic for recipe CRUD/read

src/main/resources/
├── application.yml                  # DB connection, server port (8090)
└── db/migration/                    # Flyway SQL migrations
    ├── V1__create_recipe_tables.sql
    └── V2__create_user_tables.sql
```

## Domain Model

- **Recipe** — name, description, method, preparation, servings, prep/cook time, notes, image URL, owning `userId`, list of cuisines, and a list of `RecipeIngredient`s.
- **Ingredient** — simple id/name lookup entity, reused across recipes.
- **RecipeIngredient** — join table linking a `Recipe` to an `Ingredient`, carrying quantity, unit, and notes specific to that recipe.
- **RecipeUser** — application user (username, password, email); not yet linked to `Recipe` via a foreign key (`Recipe.userId` is a plain column, no JPA relation).

All tables live in a dedicated `recipes` Postgres schema, created and versioned via two Flyway migrations.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/recipe/{recipeId}` | Fetch a single recipe with full detail (ingredients, cuisines, etc.), 404 if not found |
| `POST` | `/api/v1/recipe/create` | Create a new recipe from a `RecipeCreateRequest` |
| `GET` | `/api/v1/recipe/recipes?recipeIds=...` | Fetch a summary (`RecipeDTO`) for a batch of recipe IDs |
| `POST` | `/api/v1/login` | Placeholder login endpoint — returns a static success message, no real authentication yet (CORS enabled for `http://localhost:5173`, suggesting a Vite-based frontend) |

Swagger UI is available via `springdoc-openapi` once the app is running.

## Running Locally

1. Start the database:
   ```
   docker-compose up -d
   ```
   Spins up Postgres 16 on `localhost:5432` (db: `culinaryCode`, user/pass: `developer`/`postgresDev`).

2. Run the app:
   ```
   ./gradlew bootRun
   ```
   The API listens on port `8090`. Flyway migrations run automatically on startup, and Hibernate is set to `ddl-auto: update`.

## Testing

- `./gradlew test` runs the JUnit 5 test suite (JUnit Platform).
- Currently only a single Spring context-load smoke test (`CulinaryCodeApplicationTests`) exists — no controller/service/repository test coverage yet.

## Current State / Notes

This is an early-stage project:
- Only the **recipe** domain has real functionality; **authentication** is a stub (no password hashing, JWT, or session handling).
- No `Recipe` → `RecipeUser` JPA relationship — ownership is tracked by a raw `userId` long.
- No pagination, validation, or global exception handling on the REST layer yet.
- Test coverage is minimal (one smoke test).
