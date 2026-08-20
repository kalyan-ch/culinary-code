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
public class RecipeDTO {
    private UUID id;
    private String title;
    private String description;
    private String cuisine;
    private List<String> tags;
    private boolean published;
    private boolean owned;
}
