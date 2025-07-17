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
public class RecipeDTO {
    private Long id;
    private String name;
    private String description;
    private List<String> cuisines;
}
