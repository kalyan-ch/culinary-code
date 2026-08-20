package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.recipe.rest.RecipeCreateRequest;
import com.wb.culinaryCode.model.recipe.rest.RecipeDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeDetailDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeUpdateRequest;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

/**
 * Every endpoint here requires a session — see {@code SecurityConfig}. The owner of a recipe
 * is always taken from that session, never from the request body.
 */
@RestController
@RequestMapping("/api/v1/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailDTO> getRecipeById(@PathVariable UUID recipeId,
                                                         @AuthenticationPrincipal AuthUser user) {
        return recipeService.getRecipeById(recipeId, user.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Defaults to everything the user can see; {@code mine=true} narrows it to their own. */
    @GetMapping
    public ResponseEntity<Page<RecipeDTO>> listRecipes(
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "false") boolean mine,
            Pageable pageable,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(recipeService.listRecipes(user.id(), cuisine, tag, mine, pageable));
    }

    @PostMapping("/create")
    public ResponseEntity<RecipeDetailDTO> createRecipe(@Valid @RequestBody RecipeCreateRequest request,
                                                        @AuthenticationPrincipal AuthUser user) {
        var created = recipeService.createRecipe(request, user.id());
        return ResponseEntity.created(URI.create("/api/v1/recipe/" + created.getId())).body(created);
    }

    @PutMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailDTO> updateRecipe(@PathVariable UUID recipeId,
                                                        @Valid @RequestBody RecipeUpdateRequest request,
                                                        @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(recipeService.updateRecipe(recipeId, request, user.id()));
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID recipeId,
                                             @AuthenticationPrincipal AuthUser user) {
        recipeService.deleteRecipe(recipeId, user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<RecipeDTO>> getRecipesByIds(@RequestParam List<UUID> recipeIds,
                                                           @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(recipeService.getRecipesByIds(recipeIds, user.id()));
    }
}
