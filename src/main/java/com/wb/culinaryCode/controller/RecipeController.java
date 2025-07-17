package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.rest.RecipeDTO;
import com.wb.culinaryCode.model.rest.RecipeDetailDTO;
import com.wb.culinaryCode.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/")
    public ResponseEntity<String> getWelcomeMessage() {
        return ResponseEntity.ok("Welcome to Culinary Code!");
    }

    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<RecipeDetailDTO> getRecipeById(@PathVariable Long recipeId) {
        var recipeOpt = recipeService.getRecipeById(recipeId);

        return recipeOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<RecipeDTO>> getRecipesByIds(@RequestParam List<Long> recipeIds) {
        return ResponseEntity.ok(recipeService.getRecipesByIds(recipeIds));
    }
}
