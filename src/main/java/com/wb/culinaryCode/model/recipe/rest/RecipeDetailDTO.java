package com.wb.culinaryCode.model.recipe.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeDetailDTO {
    private UUID id;
    private String title;
    private String description;
    private Integer cookTimeMinutes;
    private Integer prepTimeMinutes;
    private Integer servings;
    private UUID userId;
    private String cuisine;
    private List<IngredientsDTO> ingredients;
    private List<RecipeStepDTO> steps;
}
