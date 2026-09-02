package com.wb.culinaryCode.model.recipe.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One entry of the ingredient catalogue, as offered to the recipe form's picker. Distinct from
 * {@link IngredientsDTO}, which is a line on a recipe and carries that recipe's quantity.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientOptionDTO {
    private UUID id;
    private String name;
    private String category;
    private String defaultUnit;
}
