package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.IngredientRepository;
import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.dao.TagRepository;
import com.wb.culinaryCode.dao.spec.RecipeSpecifications;
import com.wb.culinaryCode.exception.RecipeNotFoundException;
import com.wb.culinaryCode.model.recipe.Ingredient;
import com.wb.culinaryCode.model.recipe.Recipe;
import com.wb.culinaryCode.model.recipe.RecipeIngredient;
import com.wb.culinaryCode.model.recipe.RecipeSourceType;
import com.wb.culinaryCode.model.recipe.RecipeStep;
import com.wb.culinaryCode.model.recipe.Tag;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    public Optional<RecipeDetailDTO> getRecipeById(UUID recipeId) {
        return recipeRepository.findById(recipeId).map(this::toDetailDto);
    }

    public List<RecipeDTO> getRecipesByIds(List<UUID> recipeIds) {
        return recipeRepository.findAllById(recipeIds).stream().map(this::toDto).toList();
    }

    public Page<RecipeDTO> listRecipes(UUID userId, String cuisine, String tag, Pageable pageable) {
        var spec = Specification.allOf(
                RecipeSpecifications.hasUserId(userId),
                RecipeSpecifications.hasCuisine(cuisine),
                RecipeSpecifications.hasTag(tag)
        );
        return recipeRepository.findAll(spec, pageable).map(this::toDto);
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
        recipe.setTags(resolveTags(request.getTags()));

        var saved = recipeRepository.save(recipe);
        return toDetailDto(saved);
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
        if (recipe.getTags() == null) {
            recipe.setTags(new HashSet<>());
        }

        // recipe_steps has a UNIQUE(recipe_id, step_number) constraint and recipe_tags a
        // composite (recipe_id, tag_id) primary key, so clearing and re-adding in one flush
        // can try to insert a row before the old one's delete has landed. Flushing the
        // removal first avoids the collision.
        recipe.getRecipeIngredients().clear();
        recipe.getSteps().clear();
        recipe.getTags().clear();
        recipeRepository.saveAndFlush(recipe);

        recipe.getRecipeIngredients().addAll(buildRecipeIngredients(request.getIngredients(), recipe));
        recipe.getSteps().addAll(buildRecipeSteps(request.getSteps(), recipe));
        recipe.getTags().addAll(resolveTags(request.getTags()));

        var saved = recipeRepository.save(recipe);
        return toDetailDto(saved);
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

    private Set<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null) {
            return new HashSet<>();
        }

        // Same per-call resolution cache as buildRecipeIngredients, for the same reason:
        // avoids two lookups for the same (case-insensitive) tag name racing each other.
        Map<String, Tag> resolvedByName = new HashMap<>();
        Set<Tag> result = new LinkedHashSet<>();
        for (String name : tagNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            var tag = resolvedByName.computeIfAbsent(name.toLowerCase(), key -> resolveTag(name));
            result.add(tag);
        }
        return result;
    }

    private Tag resolveTag(String name) {
        var existing = tagRepository.findByNameIgnoreCase(name);
        if (existing != null) {
            return existing;
        }
        return tagRepository.save(Tag.builder().name(name).build());
    }

    // ModelMapper can't reflectively convert Recipe.tags (Set<Tag>) into a List<String> of
    // names, so tags are mapped by hand after the rest of the DTO is populated normally.
    private RecipeDetailDTO toDetailDto(Recipe recipe) {
        var dto = modelMapper.map(recipe, RecipeDetailDTO.class);
        dto.setTags(tagNames(recipe));
        return dto;
    }

    private RecipeDTO toDto(Recipe recipe) {
        var dto = modelMapper.map(recipe, RecipeDTO.class);
        dto.setTags(tagNames(recipe));
        return dto;
    }

    private List<String> tagNames(Recipe recipe) {
        if (recipe.getTags() == null) {
            return List.of();
        }
        return recipe.getTags().stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
