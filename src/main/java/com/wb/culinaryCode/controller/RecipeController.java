package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.recipe.rest.RecipeCreateRequest;
import com.wb.culinaryCode.model.recipe.rest.RecipeDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeDetailDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeUpdateRequest;
import com.wb.culinaryCode.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailDTO> getRecipeById(@PathVariable UUID recipeId) {
        var recipeOpt = recipeService.getRecipeById(recipeId);

        return recipeOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<RecipeDTO>> listRecipes(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String cuisine,
            Pageable pageable) {
        return ResponseEntity.ok(recipeService.listRecipes(userId, cuisine, pageable));
    }

    @PostMapping("/create")
    public ResponseEntity<RecipeDetailDTO> createRecipe(@Valid @RequestBody RecipeCreateRequest request) {
        var created = recipeService.createRecipe(request);
        return ResponseEntity.created(URI.create("/api/v1/recipe/" + created.getId())).body(created);
    }

    @PutMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailDTO> updateRecipe(@PathVariable UUID recipeId,
                                                          @Valid @RequestBody RecipeUpdateRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(recipeId, request));
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID recipeId) {
        recipeService.deleteRecipe(recipeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<RecipeDTO>> getRecipesByIds(@RequestParam List<UUID> recipeIds) {
        return ResponseEntity.ok(recipeService.getRecipesByIds(recipeIds));
    }
}
