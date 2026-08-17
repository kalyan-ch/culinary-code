package com.wb.culinaryCode.model.recipe;

// Constant names match the Postgres "recipe_source_type" enum labels exactly,
// which Hibernate's NAMED_ENUM mapping requires for the native enum column.
public enum RecipeSourceType {
    manual, imported
}
