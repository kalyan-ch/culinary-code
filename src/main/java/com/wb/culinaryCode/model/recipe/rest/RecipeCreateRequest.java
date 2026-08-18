package com.wb.culinaryCode.model.recipe.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @NotBlank
    private String title;

    private String description;

    @PositiveOrZero
    private Integer cookTimeMinutes;

    @PositiveOrZero
    private Integer prepTimeMinutes;

    @Positive
    private Integer servings;

    @NotNull
    private UUID userId;

    private List<@Valid IngredientsDTO> ingredients;

    private List<@Valid RecipeStepDTO> steps;

    private List<String> tags;

    private String cuisine;
    private String imageUrl;
}
