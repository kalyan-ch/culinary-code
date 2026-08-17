package com.wb.culinaryCode.model.recipe.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RecipeCreateRequest {
    private String title;
    private String description;
    private Integer cookTimeMinutes;
    private Integer prepTimeMinutes;
    private Integer servings;
    private UUID userId;
    private List<IngredientsDTO> ingredients;
    private List<RecipeStepDTO> steps;
    private String cuisine;
    private String imageUrl;
}
