package com.wb.culinaryCode.model.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeDetailDTO {
    private Long id;
    private String name;
    private String description;
    private Integer cookTime;
    private Integer prepTime;
    private String method;
    private Integer servings;
    private Long userId;
    private List<String> cuisines;
    private List<IngredientsDTO> ingredients;
}
