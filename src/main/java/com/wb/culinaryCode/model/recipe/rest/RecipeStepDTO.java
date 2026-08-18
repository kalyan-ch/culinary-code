package com.wb.culinaryCode.model.recipe.rest;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String instruction;

    private Integer timerMinutes;
}
