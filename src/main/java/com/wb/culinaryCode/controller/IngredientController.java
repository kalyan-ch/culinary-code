package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.recipe.rest.IngredientOptionDTO;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /**
     * The catalogue the recipe form's picker offers. Anonymous callers get the curated set only;
     * a signed-in one also gets the ingredients they invented on earlier recipes.
     *
     * <p>The whole list comes back in one response, as {@code /api/v1/tags} does — it is a few
     * hundred short rows, so the picker filters it as the user types rather than asking the
     * server on every keystroke.
     */
    @GetMapping
    public ResponseEntity<List<IngredientOptionDTO>> listIngredients(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ingredientService.listIngredients(user == null ? null : user.id()));
    }
}
