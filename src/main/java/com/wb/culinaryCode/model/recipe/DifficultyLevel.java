package com.wb.culinaryCode.model.recipe;

// Constant names match the Postgres "difficulty_level" enum labels exactly,
// which Hibernate's NAMED_ENUM mapping requires for the native enum column.
public enum DifficultyLevel {
    easy, medium, hard
}
