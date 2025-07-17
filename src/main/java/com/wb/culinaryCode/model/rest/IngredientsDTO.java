package com.wb.culinaryCode.model.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientsDTO {
    private Long id;
    private String name;
    private String unit;
    private String notes;
    private Double quantity;
}
