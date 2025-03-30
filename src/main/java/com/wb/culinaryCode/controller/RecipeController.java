package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.Recipe;
import com.wb.culinaryCode.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @GetMapping("/")
    public ResponseEntity<String> getWelcomeMessage() {
        return ResponseEntity.ok("Welcome to Culinary Code!");
    }

    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable Long recipeId) {
        var recipeOpt = recipeService.getRecipeById(recipeId);

        return recipeOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<Recipe>> getRecipesByIds(@RequestParam List<Long> recipeIds) {
        return ResponseEntity.ok(recipeService.getRecipesByIds(recipeIds));
    }
}
