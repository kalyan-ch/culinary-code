package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.recipe.rest.RecipeCreateRequest;
import com.wb.culinaryCode.model.recipe.rest.RecipeDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeDetailDTO;
import com.wb.culinaryCode.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailDTO> getRecipeById(@PathVariable Long recipeId) {
        var recipeOpt = recipeService.getRecipeById(recipeId);

        return recipeOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<String> createRecipe(RecipeCreateRequest request) {
        recipeService.createRecipe(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<RecipeDTO>> getRecipesByIds(@RequestParam List<Long> recipeIds) {
        return ResponseEntity.ok(recipeService.getRecipesByIds(recipeIds));
    }
}
