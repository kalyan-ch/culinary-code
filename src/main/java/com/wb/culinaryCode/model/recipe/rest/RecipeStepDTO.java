package com.wb.culinaryCode.model.recipe.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeStepDTO {
    private Integer stepNumber;
    private String instruction;
    private Integer timerMinutes;
}
