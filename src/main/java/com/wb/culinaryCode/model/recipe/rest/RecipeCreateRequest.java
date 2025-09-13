package com.wb.culinaryCode.model.recipe.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RecipeCreateRequest {
    private String name;
    private String description;
    private Integer cookTime;
    private Integer prepTime;
    private String method;
    private String preparation;
    private Integer servings;
    private Long userId;
    private List<IngredientsDTO> ingredients;
    private List<String> cuisines;
    private String notes;
    private String imageUrl;
}
