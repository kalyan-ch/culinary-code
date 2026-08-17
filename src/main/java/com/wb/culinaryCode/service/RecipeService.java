package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.model.recipe.Recipe;
import com.wb.culinaryCode.model.recipe.rest.RecipeCreateRequest;
import com.wb.culinaryCode.model.recipe.rest.RecipeDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeDetailDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ModelMapper modelMapper;

    public Optional<RecipeDetailDTO> getRecipeById(UUID recipeId) {
        var recipe = recipeRepository.findById(recipeId);
        return recipe.map(value -> modelMapper.map(value, RecipeDetailDTO.class));
    }

    public List<RecipeDTO> getRecipesByIds(List<UUID> recipeIds) {
        var recipes = recipeRepository.findAllById(recipeIds);
        return List.of(modelMapper.map(recipes, RecipeDTO[].class));
    }

    public void createRecipe(RecipeCreateRequest request) {
        var recipe = modelMapper.map(request, Recipe.class);
        recipeRepository.save(recipe);
    }
}
