package com.wb.culinaryCode.exception;

import java.util.UUID;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(UUID recipeId) {
        super("Recipe not found: " + recipeId);
    }
}
