package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.model.rest.RecipeDTO;
import com.wb.culinaryCode.model.rest.RecipeDetailDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ModelMapper modelMapper;

    public Optional<RecipeDetailDTO> getRecipeById(Long recipeId){
        var recipe = recipeRepository.findById(recipeId);
        return recipe.map(value -> modelMapper.map(value, RecipeDetailDTO.class));
    }

    public List<RecipeDTO> getRecipesByIds(List<Long> recipeIds) {
        var recipes = recipeRepository.findAllById(recipeIds);
        return List.of(modelMapper.map(recipes, RecipeDTO[].class));
    }
}
