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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public Optional<RecipeDetailDTO> getRecipeById(UUID recipeId, UUID viewerId) {
        return recipeRepository.findById(recipeId)
                .filter(recipe -> isVisibleTo(recipe, viewerId))
                .map(recipe -> toDetailDto(recipe, viewerId));
    }

    public List<RecipeDTO> getRecipesByIds(List<UUID> recipeIds, UUID viewerId) {
        return recipeRepository.findAllById(recipeIds).stream()
                .filter(recipe -> isVisibleTo(recipe, viewerId))
                .map(recipe -> toDto(recipe, viewerId))
                .toList();
    }

    public Page<RecipeDTO> listRecipes(UUID viewerId, String cuisine, String tag, boolean mineOnly,
                                       Pageable pageable) {
        var spec = Specification.allOf(
                mineOnly ? RecipeSpecifications.ownedBy(viewerId) : RecipeSpecifications.visibleTo(viewerId),
                RecipeSpecifications.hasCuisine(cuisine),
                RecipeSpecifications.hasTag(tag)
        );
        return recipeRepository.findAll(spec, pageable).map(recipe -> toDto(recipe, viewerId));
    }

    @Transactional
    public RecipeDetailDTO createRecipe(RecipeCreateRequest request, UUID ownerId) {
        // Built explicitly rather than via modelMapper.map(request, Recipe.class):
        // ModelMapper's default matching treats "id" as a token-suffix match of
        // "userId", so it was mapping request.userId into recipe.id as well as
        // recipe.userId, turning the insert into a failed update-by-id.
        var recipe = Recipe.builder()
                .userId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .servings(request.getServings())
                .prepTimeMinutes(request.getPrepTimeMinutes())
                .cookTimeMinutes(request.getCookTimeMinutes())
                .cuisine(request.getCuisine())
                .imageUrl(request.getImageUrl())
                .published(request.isPublished())
                .sourceType(RecipeSourceType.manual)
                .build();

        resetChildren(recipe);
        replaceChildren(recipe, request.getIngredients(), request.getSteps(), request.getTags(), ownerId);

        var saved = recipeRepository.save(recipe);
        return toDetailDto(saved, ownerId);
    }

    @Transactional
    public RecipeDetailDTO updateRecipe(UUID recipeId, RecipeUpdateRequest request, UUID ownerId) {
        var recipe = requireOwned(recipeId, ownerId);

        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setServings(request.getServings());
        recipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(request.getCookTimeMinutes());
        recipe.setCuisine(request.getCuisine());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setPublished(request.isPublished());

        // recipe_steps has a UNIQUE(recipe_id, step_number) constraint and recipe_tags a
        // composite (recipe_id, tag_id) primary key, so clearing and re-adding in one flush
        // can try to insert a row before the old one's delete has landed. Flushing the
        // removal first avoids the collision.
        resetChildren(recipe);
        recipeRepository.saveAndFlush(recipe);
        replaceChildren(recipe, request.getIngredients(), request.getSteps(), request.getTags(), ownerId);

        var saved = recipeRepository.save(recipe);
        return toDetailDto(saved, ownerId);
    }

    @Transactional
    public void deleteRecipe(UUID recipeId, UUID ownerId) {
        recipeRepository.delete(requireOwned(recipeId, ownerId));
    }

    /**
     * A recipe you cannot see is reported as missing rather than forbidden — answering 403 for
     * someone else's private recipe would confirm that it exists. A published recipe you don't
     * own is a 403, because you can already see it and hiding it would only be confusing.
     */
    private Recipe requireOwned(UUID recipeId, UUID ownerId) {
        var recipe = recipeRepository.findById(recipeId)
                .filter(candidate -> isVisibleTo(candidate, ownerId))
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        if (!recipe.getUserId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This recipe belongs to someone else");
        }
        return recipe;
    }

    private boolean isVisibleTo(Recipe recipe, UUID viewerId) {
        return recipe.isPublished() || recipe.getUserId().equals(viewerId);
    }

    /**
     * Empties the child collections in place. The existing instances have to be reused rather
     * than replaced — swapping the collection on a managed entity trips Hibernate's
     * "collection with cascade=all-delete-orphan was no longer referenced".
     */
    private void resetChildren(Recipe recipe) {
        if (recipe.getRecipeIngredients() == null) {
            recipe.setRecipeIngredients(new ArrayList<>());
            recipe.setSteps(new ArrayList<>());
            recipe.setTags(new HashSet<>());
            return;
        }
        recipe.getRecipeIngredients().clear();
        recipe.getSteps().clear();
        recipe.getTags().clear();
    }

    private void replaceChildren(Recipe recipe, List<IngredientsDTO> ingredients,
                                 List<RecipeStepDTO> steps, List<String> tags, UUID ownerId) {
        recipe.getRecipeIngredients().addAll(buildRecipeIngredients(ingredients, recipe, ownerId));
        recipe.getSteps().addAll(buildRecipeSteps(steps, recipe));
        recipe.getTags().addAll(resolveTags(tags, ownerId));
    }

    private List<RecipeIngredient> buildRecipeIngredients(List<IngredientsDTO> ingredientDTOs, Recipe recipe,
                                                          UUID ownerId) {
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
                    dto.getName().toLowerCase(), key -> resolveIngredient(dto.getName(), ownerId));
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

    /**
     * Curated ingredients win, so everyone's "garlic" is the same row and keeps its aisle
     * category. Anything unrecognised is created against the user — an invented name never
     * pollutes the shared catalogue.
     */
    private Ingredient resolveIngredient(String name, UUID ownerId) {
        return ingredientRepository.findCommonByName(name)
                .or(() -> ingredientRepository.findOwnedByName(ownerId, name))
                .orElseGet(() -> ingredientRepository.save(
                        Ingredient.builder().name(name).userId(ownerId).build()));
    }

    private Set<Tag> resolveTags(List<String> tagNames, UUID ownerId) {
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
            result.add(resolvedByName.computeIfAbsent(name.toLowerCase(), key -> resolveTag(name, ownerId)));
        }
        return result;
    }

    /**
     * Curated tags win, so everyone tagging "vegetarian" shares one row. Anything else becomes
     * a tag owned by the user — nothing here can ever write into the curated set.
     */
    private Tag resolveTag(String name, UUID ownerId) {
        return tagRepository.findCommonByName(name)
                .or(() -> tagRepository.findOwnedByName(ownerId, name))
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name).userId(ownerId).build()));
    }

    // ModelMapper can't reflectively convert Recipe.tags (Set<Tag>) into a List<String> of
    // names, so tags are mapped by hand after the rest of the DTO is populated normally.
    private RecipeDetailDTO toDetailDto(Recipe recipe, UUID viewerId) {
        var dto = modelMapper.map(recipe, RecipeDetailDTO.class);
        dto.setTags(tagNames(recipe));
        dto.setOwned(recipe.getUserId().equals(viewerId));
        return dto;
    }

    private RecipeDTO toDto(Recipe recipe, UUID viewerId) {
        var dto = modelMapper.map(recipe, RecipeDTO.class);
        dto.setTags(tagNames(recipe));
        dto.setOwned(recipe.getUserId().equals(viewerId));
        return dto;
    }

    private List<String> tagNames(Recipe recipe) {
        if (recipe.getTags() == null) {
            return List.of();
        }
        return recipe.getTags().stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
