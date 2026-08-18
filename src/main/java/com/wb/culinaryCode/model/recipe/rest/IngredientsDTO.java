package com.wb.culinaryCode.model.recipe.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientsDTO {
    private UUID id;

    @NotBlank
    private String name;

    private String unit;
    private String notes;

    @PositiveOrZero
    private BigDecimal quantity;
}
