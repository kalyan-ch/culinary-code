package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.IngredientRepository;
import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.exception.RecipeNotFoundException;
import com.wb.culinaryCode.model.recipe.Ingredient;
import com.wb.culinaryCode.model.recipe.Recipe;
import com.wb.culinaryCode.model.recipe.RecipeIngredient;
import com.wb.culinaryCode.model.recipe.RecipeSourceType;
import com.wb.culinaryCode.model.recipe.RecipeStep;
import com.wb.culinaryCode.model.recipe.rest.IngredientsDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeCreateRequest;
import com.wb.culinaryCode.model.recipe.rest.RecipeDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeDetailDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeStepDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final ModelMapper modelMapper;

    public Optional<RecipeDetailDTO> getRecipeById(UUID recipeId) {
        var recipe = recipeRepository.findById(recipeId);
        return recipe.map(value -> modelMapper.map(value, RecipeDetailDTO.class));
    }

    public List<RecipeDTO> getRecipesByIds(List<UUID> recipeIds) {
        var recipes = recipeRepository.findAllById(recipeIds);
        return List.of(modelMapper.map(recipes, RecipeDTO[].class));
    }

    public Page<RecipeDTO> listRecipes(UUID userId, String cuisine, Pageable pageable) {
        Page<Recipe> recipes;
        if (userId != null && cuisine != null) {
            recipes = recipeRepository.findByUserIdAndCuisine(userId, cuisine, pageable);
        } else if (userId != null) {
            recipes = recipeRepository.findByUserId(userId, pageable);
        } else if (cuisine != null) {
            recipes = recipeRepository.findByCuisine(cuisine, pageable);
        } else {
            recipes = recipeRepository.findAll(pageable);
        }
        return recipes.map(recipe -> modelMapper.map(recipe, RecipeDTO.class));
    }

    @Transactional
    public RecipeDetailDTO createRecipe(RecipeCreateRequest request) {
        // Built explicitly rather than via modelMapper.map(request, Recipe.class):
        // ModelMapper's default matching treats "id" as a token-suffix match of
        // "userId", so it was mapping request.userId into recipe.id as well as
        // recipe.userId, turning the insert into a failed update-by-id.
        var recipe = Recipe.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .description(request.getDescription())
                .servings(request.getServings())
                .prepTimeMinutes(request.getPrepTimeMinutes())
                .cookTimeMinutes(request.getCookTimeMinutes())
                .cuisine(request.getCuisine())
                .imageUrl(request.getImageUrl())
                .sourceType(RecipeSourceType.manual)
                .build();

        recipe.setRecipeIngredients(buildRecipeIngredients(request.getIngredients(), recipe));
        recipe.setSteps(buildRecipeSteps(request.getSteps(), recipe));

        var saved = recipeRepository.save(recipe);
        return modelMapper.map(saved, RecipeDetailDTO.class);
    }

    @Transactional
    public RecipeDetailDTO updateRecipe(UUID recipeId, RecipeUpdateRequest request) {
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setServings(request.getServings());
        recipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(request.getCookTimeMinutes());
        recipe.setCuisine(request.getCuisine());
        recipe.setImageUrl(request.getImageUrl());

        if (recipe.getRecipeIngredients() == null) {
            recipe.setRecipeIngredients(new ArrayList<>());
        }
        if (recipe.getSteps() == null) {
            recipe.setSteps(new ArrayList<>());
        }

        // recipe_steps has a UNIQUE(recipe_id, step_number) constraint, so clearing and
        // re-adding in one flush can try to insert the new step_number=1 before the old
        // row's delete has landed. Flushing the removal first avoids the collision.
        recipe.getRecipeIngredients().clear();
        recipe.getSteps().clear();
        recipeRepository.saveAndFlush(recipe);

        recipe.getRecipeIngredients().addAll(buildRecipeIngredients(request.getIngredients(), recipe));
        recipe.getSteps().addAll(buildRecipeSteps(request.getSteps(), recipe));

        var saved = recipeRepository.save(recipe);
        return modelMapper.map(saved, RecipeDetailDTO.class);
    }

    @Transactional
    public void deleteRecipe(UUID recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new RecipeNotFoundException(recipeId);
        }
        recipeRepository.deleteById(recipeId);
    }

    private List<RecipeIngredient> buildRecipeIngredients(List<IngredientsDTO> ingredientDTOs, Recipe recipe) {
        if (ingredientDTOs == null) {
            return new ArrayList<>();
        }

        // Resolved ingredients are cached per-call (keyed by lowercased name) so that
        // repeated names within the same request reuse one Ingredient instance instead
        // of racing two DB lookups before either insert is visible to the other.
        Map<String, Ingredient> resolvedByName = new HashMap<>();
        var result = new ArrayList<RecipeIngredient>();
        for (int i = 0; i < ingredientDTOs.size(); i++) {
            var dto = ingredientDTOs.get(i);
            var ingredient = resolvedByName.computeIfAbsent(
                    dto.getName().toLowerCase(), key -> resolveIngredient(dto.getName()));
            result.add(RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredient(ingredient)
                    .quantity(dto.getQuantity())
                    .unit(dto.getUnit())
                    .notes(dto.getNotes())
                    .sortOrder(i)
                    .build());
        }
        return result;
    }

    private List<RecipeStep> buildRecipeSteps(List<RecipeStepDTO> stepDTOs, Recipe recipe) {
        if (stepDTOs == null) {
            return new ArrayList<>();
        }

        var result = new ArrayList<RecipeStep>();
        for (int i = 0; i < stepDTOs.size(); i++) {
            var dto = stepDTOs.get(i);
            result.add(RecipeStep.builder()
                    .recipe(recipe)
                    .stepNumber(dto.getStepNumber() != null ? dto.getStepNumber() : i + 1)
                    .instruction(dto.getInstruction())
                    .timerMinutes(dto.getTimerMinutes())
                    .build());
        }
        return result;
    }

    private Ingredient resolveIngredient(String name) {
        var existing = ingredientRepository.findByNameIgnoreCase(name);
        if (existing != null) {
            return existing;
        }
        return ingredientRepository.save(Ingredient.builder().name(name).build());
    }
}
