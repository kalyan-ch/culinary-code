package com.wb.culinaryCode.service;

import com.wb.culinaryCode.config.CulinaryCodeConfig;
import com.wb.culinaryCode.dao.IngredientRepository;
import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.dao.TagRepository;
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
import com.wb.culinaryCode.model.recipe.rest.RecipeStepDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RECIPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000009");

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private TagRepository tagRepository;

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        // The real ModelMapper bean, so the RecipeIngredient -> IngredientsDTO PropertyMap
        // (which reaches through to ingredient.name) is exercised rather than stubbed away.
        recipeService = new RecipeService(
                recipeRepository,
                ingredientRepository,
                tagRepository,
                new CulinaryCodeConfig().modelMapper());
    }

    private void echoSave() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static IngredientsDTO ingredient(String name, String quantity, String unit, String notes) {
        return IngredientsDTO.builder()
                .name(name)
                .quantity(quantity == null ? null : new BigDecimal(quantity))
                .unit(unit)
                .notes(notes)
                .build();
    }

    private static RecipeStepDTO step(Integer stepNumber, String instruction, Integer timerMinutes) {
        return RecipeStepDTO.builder()
                .stepNumber(stepNumber)
                .instruction(instruction)
                .timerMinutes(timerMinutes)
                .build();
    }

    private static RecipeCreateRequest.RecipeCreateRequestBuilder createRequest() {
        return RecipeCreateRequest.builder()
                .title("Jollof Rice")
                .ingredients(new ArrayList<>())
                .steps(new ArrayList<>())
                .tags(new ArrayList<>());
    }

    private static Ingredient storedIngredient(String name) {
        return Ingredient.builder().id(UUID.randomUUID()).name(name).build();
    }

    private static Tag storedTag(String name) {
        return Tag.builder().id(UUID.randomUUID()).name(name).build();
    }

    /** A fully populated persisted recipe, of the shape findById would return. */
    private static Recipe persistedRecipe() {
        var recipe = Recipe.builder()
                .userId(USER_ID)
                .id(RECIPE_ID)
                .title("Caprese Salad")
                .description("Simple salad of tomato, basil, and olive oil.")
                .servings(2)
                .prepTimeMinutes(10)
                .cookTimeMinutes(0)
                .cuisine("Italian")
                .sourceType(RecipeSourceType.manual)
                .recipeIngredients(new ArrayList<>())
                .steps(new ArrayList<>())
                .tags(new HashSet<>())
                .build();

        recipe.getRecipeIngredients().add(RecipeIngredient.builder()
                .id(UUID.randomUUID())
                .recipe(recipe)
                .ingredient(storedIngredient("tomato"))
                .quantity(new BigDecimal("3.00"))
                .unit("each")
                .notes("ripe")
                .sortOrder(0)
                .build());
        recipe.getSteps().add(RecipeStep.builder()
                .id(UUID.randomUUID())
                .recipe(recipe)
                .stepNumber(1)
                .instruction("Slice the tomatoes.")
                .timerMinutes(5)
                .build());
        recipe.getTags().add(storedTag("vegetarian"));
        recipe.getTags().add(storedTag("Quick"));
        return recipe;
    }

    @Nested
    @DisplayName("createRecipe")
    class CreateRecipe {

        @Test
        @DisplayName("leaves the id unset so the DB generates it, and does not leak userId into it")
        void doesNotLeakUserIdIntoId() {
            echoSave();

            recipeService.createRecipe(createRequest().build(), USER_ID);

            verify(recipeRepository).save(recipeCaptor.capture());
            var saved = recipeCaptor.getValue();
            assertThat(saved.getId()).isNull();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("copies the scalar fields and stamps sourceType=manual")
        void copiesScalarFields() {
            echoSave();

            recipeService.createRecipe(createRequest()
                    .title("Jollof Rice")
                    .description("West African rice dish")
                    .servings(4)
                    .prepTimeMinutes(15)
                    .cookTimeMinutes(30)
                    .cuisine("Nigerian")
                    .imageUrl("https://example.com/jollof.jpg")
                    .build(), USER_ID);

            verify(recipeRepository).save(recipeCaptor.capture());
            var saved = recipeCaptor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Jollof Rice");
            assertThat(saved.getDescription()).isEqualTo("West African rice dish");
            assertThat(saved.getServings()).isEqualTo(4);
            assertThat(saved.getPrepTimeMinutes()).isEqualTo(15);
            assertThat(saved.getCookTimeMinutes()).isEqualTo(30);
            assertThat(saved.getCuisine()).isEqualTo("Nigerian");
            assertThat(saved.getImageUrl()).isEqualTo("https://example.com/jollof.jpg");
            assertThat(saved.getSourceType()).isEqualTo(RecipeSourceType.manual);
        }

        @Test
        @DisplayName("numbers ingredient sortOrder by request order and back-links the recipe")
        void assignsSortOrderInRequestOrder() {
            echoSave();
            when(ingredientRepository.save(any(Ingredient.class)))
                    .thenAnswer(inv -> storedIngredient(((Ingredient) inv.getArgument(0)).getName()));

            recipeService.createRecipe(createRequest()
                    .ingredients(List.of(
                            ingredient("Rice", "2", "cup", null),
                            ingredient("Tomato", "3", "each", "chopped"),
                            ingredient("Stock", "480", "ml", null)))
                    .build(), USER_ID);

            verify(recipeRepository).save(recipeCaptor.capture());
            var saved = recipeCaptor.getValue();
            assertThat(saved.getRecipeIngredients())
                    .extracting(ri -> ri.getIngredient().getName(), RecipeIngredient::getSortOrder)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("Rice", 0),
                            org.assertj.core.groups.Tuple.tuple("Tomato", 1),
                            org.assertj.core.groups.Tuple.tuple("Stock", 2));
            assertThat(saved.getRecipeIngredients())
                    .allSatisfy(ri -> assertThat(ri.getRecipe()).isSameAs(saved));
            assertThat(saved.getRecipeIngredients().get(1).getQuantity()).isEqualByComparingTo("3");
            assertThat(saved.getRecipeIngredients().get(1).getUnit()).isEqualTo("each");
            assertThat(saved.getRecipeIngredients().get(1).getNotes()).isEqualTo("chopped");
        }

        @Test
        @DisplayName("defaults missing step numbers to the request position")
        void defaultsStepNumbers() {
            echoSave();

            recipeService.createRecipe(createRequest()
                    .steps(List.of(
                            step(null, "Char the peppers", null),
                            step(null, "Blend the sauce", 10),
                            step(null, "Simmer the rice", null)))
                    .build(), USER_ID);

            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getSteps())
                    .extracting(RecipeStep::getStepNumber, RecipeStep::getInstruction, RecipeStep::getTimerMinutes)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(1, "Char the peppers", null),
                            org.assertj.core.groups.Tuple.tuple(2, "Blend the sauce", 10),
                            org.assertj.core.groups.Tuple.tuple(3, "Simmer the rice", null));
        }

        @Test
        @DisplayName("keeps explicit step numbers when the client supplies them")
        void keepsExplicitStepNumbers() {
            echoSave();

            recipeService.createRecipe(createRequest()
                    .steps(List.of(step(7, "Rest the meat", null), step(9, "Serve", null)))
                    .build(), USER_ID);

            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getSteps())
                    .extracting(RecipeStep::getStepNumber)
                    .containsExactly(7, 9);
        }

        @Test
        @DisplayName("reuses an existing ingredient regardless of case instead of inserting a duplicate")
        void reusesExistingIngredientIgnoringCase() {
            echoSave();
            var existing = storedIngredient("salt");
            when(ingredientRepository.findCommonByName("SALT")).thenReturn(java.util.Optional.of(existing));

            recipeService.createRecipe(createRequest()
                    .ingredients(List.of(ingredient("SALT", "1", "tsp", null)))
                    .build(), USER_ID);

            verify(ingredientRepository, never()).save(any(Ingredient.class));
            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getRecipeIngredients().getFirst().getIngredient())
                    .isSameAs(existing);
        }

        @Test
        @DisplayName("resolves a repeated ingredient name once and shares the instance")
        void resolvesRepeatedIngredientNameOnce() {
            echoSave();
            when(ingredientRepository.save(any(Ingredient.class)))
                    .thenAnswer(inv -> storedIngredient(((Ingredient) inv.getArgument(0)).getName()));

            recipeService.createRecipe(createRequest()
                    .ingredients(List.of(
                            ingredient("Salt", "1", "tsp", "for the sauce"),
                            ingredient("salt", "2", "tsp", "for the rice")))
                    .build(), USER_ID);

            verify(ingredientRepository, times(1)).findCommonByName(anyString());
            verify(ingredientRepository, times(1)).save(any(Ingredient.class));
            verify(recipeRepository).save(recipeCaptor.capture());
            var ingredients = recipeCaptor.getValue().getRecipeIngredients();
            assertThat(ingredients).hasSize(2);
            assertThat(ingredients.get(0).getIngredient()).isSameAs(ingredients.get(1).getIngredient());
        }

        @Test
        @DisplayName("creates a tag only when no case-insensitive match exists")
        void createsTagOnlyWhenMissing() {
            echoSave();
            var existing = storedTag("quick");
            when(tagRepository.findCommonByName("Quick")).thenReturn(java.util.Optional.of(existing));
            when(tagRepository.save(any(Tag.class)))
                    .thenAnswer(inv -> storedTag(((Tag) inv.getArgument(0)).getName()));

            recipeService.createRecipe(createRequest()
                    .tags(List.of("Quick", "weeknight"))
                    .build(), USER_ID);

            verify(tagRepository, times(1)).save(any(Tag.class));
            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getTags())
                    .extracting(Tag::getName)
                    .containsExactlyInAnyOrder("quick", "weeknight");
        }

        @Test
        @DisplayName("an unknown ingredient is created owned by the user, not as a curated one")
        void unknownIngredientIsOwnedByTheUser() {
            echoSave();
            when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> inv.getArgument(0));

            recipeService.createRecipe(createRequest()
                    .ingredients(List.of(IngredientsDTO.builder().name("egusi").build()))
                    .build(), USER_ID);

            var saved = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(saved.capture());
            assertThat(saved.getValue().getName()).isEqualTo("egusi");
            assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("reuses a curated ingredient rather than creating a per-user copy")
        void reusesCuratedIngredient() {
            echoSave();
            var curated = storedIngredient("garlic");
            when(ingredientRepository.findCommonByName("Garlic")).thenReturn(java.util.Optional.of(curated));

            recipeService.createRecipe(createRequest()
                    .ingredients(List.of(IngredientsDTO.builder().name("Garlic").build()))
                    .build(), USER_ID);

            verify(ingredientRepository, never()).save(any(Ingredient.class));
        }

        @Test
        @DisplayName("reuses a curated tag rather than creating a per-user copy")
        void reusesCuratedTag() {
            echoSave();
            var curated = storedTag("vegetarian");
            when(tagRepository.findCommonByName("Vegetarian")).thenReturn(java.util.Optional.of(curated));

            recipeService.createRecipe(createRequest().tags(List.of("Vegetarian")).build(), USER_ID);

            verify(tagRepository, never()).save(any(Tag.class));
            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getTags()).containsExactly(curated);
        }

        @Test
        @DisplayName("an invented tag is created owned by the user, never as a curated one")
        void inventedTagIsOwnedByTheUser() {
            echoSave();
            when(tagRepository.save(any(Tag.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            recipeService.createRecipe(createRequest().tags(List.of("sunday-batch")).build(), USER_ID);

            var saved = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(saved.capture());
            assertThat(saved.getValue().getName()).isEqualTo("sunday-batch");
            assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("dedupes tags that differ only by case")
        void dedupesTagsIgnoringCase() {
            echoSave();
            when(tagRepository.save(any(Tag.class)))
                    .thenAnswer(inv -> storedTag(((Tag) inv.getArgument(0)).getName()));

            recipeService.createRecipe(createRequest()
                    .tags(List.of("Quick", "quick", "QUICK"))
                    .build(), USER_ID);

            verify(tagRepository, times(1)).findCommonByName(anyString());
            verify(tagRepository, times(1)).save(any(Tag.class));
            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getTags()).hasSize(1);
        }

        @Test
        @DisplayName("skips null and blank tag names")
        void skipsNullAndBlankTags() {
            echoSave();
            when(tagRepository.findCommonByName("real")).thenReturn(java.util.Optional.of(storedTag("real")));

            recipeService.createRecipe(createRequest()
                    .tags(Arrays.asList("real", null, "", "   "))
                    .build(), USER_ID);

            verify(tagRepository, times(1)).findCommonByName(anyString());
            verify(recipeRepository).save(recipeCaptor.capture());
            assertThat(recipeCaptor.getValue().getTags())
                    .extracting(Tag::getName)
                    .containsExactly("real");
        }

        @Test
        @DisplayName("treats null ingredient/step/tag collections as empty")
        void handlesNullCollections() {
            echoSave();

            var result = recipeService.createRecipe(RecipeCreateRequest.builder()
                    .title("Bare Bones")
                    .build(), USER_ID);

            verify(recipeRepository).save(recipeCaptor.capture());
            var saved = recipeCaptor.getValue();
            assertThat(saved.getRecipeIngredients()).isEmpty();
            assertThat(saved.getSteps()).isEmpty();
            assertThat(saved.getTags()).isEmpty();
            assertThat(result.getIngredients()).isEmpty();
            assertThat(result.getSteps()).isEmpty();
            assertThat(result.getTags()).isEmpty();
        }

        @Test
        @DisplayName("returns a detail DTO with tags sorted case-insensitively")
        void returnsDetailDtoWithSortedTags() {
            echoSave();
            when(tagRepository.findCommonByName("Zest")).thenReturn(java.util.Optional.of(storedTag("Zest")));
            when(tagRepository.findCommonByName("apple")).thenReturn(java.util.Optional.of(storedTag("apple")));
            when(tagRepository.findCommonByName("Brine")).thenReturn(java.util.Optional.of(storedTag("Brine")));
            when(ingredientRepository.findCommonByName("Rice")).thenReturn(java.util.Optional.of(storedIngredient("Rice")));

            var result = recipeService.createRecipe(createRequest()
                    .title("Jollof Rice")
                    .servings(4)
                    .ingredients(List.of(ingredient("Rice", "2", "cup", "rinsed")))
                    .steps(List.of(step(1, "Cook it", 30)))
                    .tags(List.of("Zest", "apple", "Brine"))
                    .build(), USER_ID);

            assertThat(result.getTitle()).isEqualTo("Jollof Rice");
            assertThat(result.getServings()).isEqualTo(4);
            assertThat(result.isOwned()).isTrue();
            assertThat(result.getTags()).containsExactly("apple", "Brine", "Zest");
            assertThat(result.getIngredients()).singleElement().satisfies(dto -> {
                assertThat(dto.getName()).isEqualTo("Rice");
                assertThat(dto.getQuantity()).isEqualByComparingTo("2");
                assertThat(dto.getUnit()).isEqualTo("cup");
                assertThat(dto.getNotes()).isEqualTo("rinsed");
            });
            assertThat(result.getSteps()).singleElement().satisfies(dto -> {
                assertThat(dto.getStepNumber()).isEqualTo(1);
                assertThat(dto.getInstruction()).isEqualTo("Cook it");
                assertThat(dto.getTimerMinutes()).isEqualTo(30);
            });
        }
    }

    @Nested
    @DisplayName("updateRecipe")
    class UpdateRecipe {

        private RecipeUpdateRequest.RecipeUpdateRequestBuilder updateRequest() {
            return RecipeUpdateRequest.builder()
                    .title("Caprese Salad v2")
                    .description("Now with more basil")
                    .servings(6)
                    .prepTimeMinutes(12)
                    .cookTimeMinutes(1)
                    .cuisine("Italian")
                    .imageUrl("https://example.com/caprese.jpg")
                    .ingredients(new ArrayList<>())
                    .steps(new ArrayList<>())
                    .tags(new ArrayList<>());
        }

        @Test
        @DisplayName("throws RecipeNotFoundException when the recipe is missing")
        void throwsWhenMissing() {
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> recipeService.updateRecipe(RECIPE_ID, updateRequest().build(), USER_ID))
                    .isInstanceOf(RecipeNotFoundException.class)
                    .hasMessageContaining(RECIPE_ID.toString());

            verify(recipeRepository, never()).save(any(Recipe.class));
        }

        @Test
        @DisplayName("overwrites the scalar fields")
        void overwritesScalarFields() {
            var existing = persistedRecipe();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(existing));
            when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
            echoSave();

            recipeService.updateRecipe(RECIPE_ID, updateRequest().build(), USER_ID);

            assertThat(existing.getTitle()).isEqualTo("Caprese Salad v2");
            assertThat(existing.getDescription()).isEqualTo("Now with more basil");
            assertThat(existing.getServings()).isEqualTo(6);
            assertThat(existing.getPrepTimeMinutes()).isEqualTo(12);
            assertThat(existing.getCookTimeMinutes()).isEqualTo(1);
            assertThat(existing.getImageUrl()).isEqualTo("https://example.com/caprese.jpg");
        }

        @Test
        @DisplayName("leaves ownership and provenance untouched")
        void preservesOwnershipAndProvenance() {
            var existing = persistedRecipe();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(existing));
            when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
            echoSave();

            recipeService.updateRecipe(RECIPE_ID, updateRequest().build(), USER_ID);

            assertThat(existing.getId()).isEqualTo(RECIPE_ID);
            assertThat(existing.getUserId()).isEqualTo(USER_ID);
            assertThat(existing.getSourceType()).isEqualTo(RecipeSourceType.manual);
        }

        @Test
        @DisplayName("flushes the child-row deletes before re-inserting them")
        void flushesRemovalsBeforeReinserting() {
            var existing = persistedRecipe();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(existing));
            when(ingredientRepository.findCommonByName("basil")).thenReturn(java.util.Optional.of(storedIngredient("basil")));
            when(tagRepository.findCommonByName("summer")).thenReturn(java.util.Optional.of(storedTag("summer")));
            echoSave();

            // recipe_steps has UNIQUE(recipe_id, step_number) and recipe_tags a composite PK, so the
            // deletes must reach the DB before the replacement rows are inserted. At flush time the
            // collections must therefore be empty.
            doAnswer(inv -> {
                Recipe flushed = inv.getArgument(0);
                assertThat(flushed.getRecipeIngredients()).isEmpty();
                assertThat(flushed.getSteps()).isEmpty();
                assertThat(flushed.getTags()).isEmpty();
                return flushed;
            }).when(recipeRepository).saveAndFlush(any(Recipe.class));

            recipeService.updateRecipe(RECIPE_ID, updateRequest()
                    .ingredients(List.of(ingredient("basil", "10", "g", null)))
                    .steps(List.of(step(1, "Tear the basil", null), step(2, "Plate it", null)))
                    .tags(List.of("summer"))
                    .build(), USER_ID);

            InOrder order = inOrder(recipeRepository);
            order.verify(recipeRepository).findById(RECIPE_ID);
            order.verify(recipeRepository).saveAndFlush(any(Recipe.class));
            order.verify(recipeRepository).save(any(Recipe.class));
        }

        @Test
        @DisplayName("replaces the child collections with the request payload")
        void replacesChildCollections() {
            var existing = persistedRecipe();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(existing));
            when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ingredientRepository.findCommonByName("basil")).thenReturn(java.util.Optional.of(storedIngredient("basil")));
            when(ingredientRepository.findCommonByName("mozzarella")).thenReturn(java.util.Optional.of(storedIngredient("mozzarella")));
            when(tagRepository.findCommonByName("summer")).thenReturn(java.util.Optional.of(storedTag("summer")));
            echoSave();

            var result = recipeService.updateRecipe(RECIPE_ID, updateRequest()
                    .ingredients(List.of(
                            ingredient("basil", "10", "g", null),
                            ingredient("mozzarella", "125", "g", "torn")))
                    .steps(List.of(step(1, "Tear the basil", null), step(2, "Plate it", 2)))
                    .tags(List.of("summer"))
                    .build(), USER_ID);

            assertThat(existing.getRecipeIngredients())
                    .extracting(ri -> ri.getIngredient().getName(), RecipeIngredient::getSortOrder)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("basil", 0),
                            org.assertj.core.groups.Tuple.tuple("mozzarella", 1));
            assertThat(existing.getSteps())
                    .extracting(RecipeStep::getStepNumber, RecipeStep::getInstruction)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(1, "Tear the basil"),
                            org.assertj.core.groups.Tuple.tuple(2, "Plate it"));
            assertThat(existing.getTags()).extracting(Tag::getName).containsExactly("summer");
            assertThat(result.getTags()).containsExactly("summer");
            assertThat(result.getIngredients()).extracting(IngredientsDTO::getName)
                    .containsExactly("basil", "mozzarella");
        }

        @Test
        @DisplayName("initializes null child collections on the stored recipe")
        void initializesNullCollections() {
            var existing = Recipe.builder()
                .userId(USER_ID)
                    .id(RECIPE_ID)
                    .title("Sparse")
                    .sourceType(RecipeSourceType.manual)
                    .build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(existing));
            when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ingredientRepository.findCommonByName("basil")).thenReturn(java.util.Optional.of(storedIngredient("basil")));
            echoSave();

            recipeService.updateRecipe(RECIPE_ID, updateRequest()
                    .ingredients(List.of(ingredient("basil", "10", "g", null)))
                    .steps(List.of(step(1, "Tear the basil", null)))
                    .build(), USER_ID);

            assertThat(existing.getRecipeIngredients()).hasSize(1);
            assertThat(existing.getSteps()).hasSize(1);
            assertThat(existing.getTags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteRecipe")
    class DeleteRecipe {

        @Test
        @DisplayName("throws when the recipe does not exist")
        void throwsWhenMissing() {
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> recipeService.deleteRecipe(RECIPE_ID, USER_ID))
                    .isInstanceOf(RecipeNotFoundException.class)
                    .hasMessageContaining(RECIPE_ID.toString());

            verify(recipeRepository, never()).delete(any(Recipe.class));
        }

        @Test
        @DisplayName("deletes the recipe when the caller owns it")
        void deletesWhenPresent() {
            var recipe = Recipe.builder().id(RECIPE_ID).userId(USER_ID).title("Caprese Salad").build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(recipe));

            recipeService.deleteRecipe(RECIPE_ID, USER_ID);

            verify(recipeRepository).delete(recipe);
        }

        @Test
        @DisplayName("refuses to delete a published recipe belonging to someone else")
        void refusesSomeoneElsesPublishedRecipe() {
            var recipe = Recipe.builder().id(RECIPE_ID).userId(OTHER_USER_ID).published(true).build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(recipe));

            assertThatThrownBy(() -> recipeService.deleteRecipe(RECIPE_ID, USER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");

            verify(recipeRepository, never()).delete(any(Recipe.class));
        }

        @Test
        @DisplayName("reports someone else's private recipe as missing rather than forbidden")
        void hidesSomeoneElsesPrivateRecipe() {
            var recipe = Recipe.builder().id(RECIPE_ID).userId(OTHER_USER_ID).build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(recipe));

            assertThatThrownBy(() -> recipeService.deleteRecipe(RECIPE_ID, USER_ID))
                    .isInstanceOf(RecipeNotFoundException.class);

            verify(recipeRepository, never()).delete(any(Recipe.class));
        }
    }

    @Nested
    @DisplayName("anonymous viewers")
    class AnonymousViewers {

        @Test
        @DisplayName("a published recipe is readable with no session, and is not owned")
        void publishedRecipeIsReadable() {
            var recipe = Recipe.builder().id(RECIPE_ID).userId(OTHER_USER_ID)
                    .title("Jollof Rice").published(true).build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(recipe));

            var result = recipeService.getRecipeById(RECIPE_ID, null).orElseThrow();

            assertThat(result.getTitle()).isEqualTo("Jollof Rice");
            assertThat(result.isOwned()).isFalse();
        }

        @Test
        @DisplayName("an unpublished recipe is invisible with no session")
        void unpublishedRecipeIsHidden() {
            var recipe = Recipe.builder().id(RECIPE_ID).userId(OTHER_USER_ID)
                    .title("Private").published(false).build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(recipe));

            assertThat(recipeService.getRecipeById(RECIPE_ID, null)).isEmpty();
        }

        @Test
        @DisplayName("a batch lookup drops the recipes an anonymous viewer may not see")
        void batchLookupFiltersUnpublished() {
            var published = Recipe.builder().id(RECIPE_ID).userId(OTHER_USER_ID)
                    .title("Public").published(true).build();
            var privateOne = Recipe.builder().id(UUID.randomUUID()).userId(OTHER_USER_ID)
                    .title("Private").published(false).build();
            when(recipeRepository.findAllById(any())).thenReturn(List.of(published, privateOne));

            var result = recipeService.getRecipesByIds(List.of(RECIPE_ID), null);

            assertThat(result).extracting(RecipeDTO::getTitle).containsExactly("Public");
        }
    }

    @Nested
    @DisplayName("read paths")
    class ReadPaths {

        @Test
        @DisplayName("getRecipeById returns empty when the recipe is missing")
        void getByIdReturnsEmpty() {
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.empty());

            assertThat(recipeService.getRecipeById(RECIPE_ID, USER_ID)).isEmpty();
        }

        @Test
        @DisplayName("getRecipeById maps ingredients, steps and case-insensitively sorted tags")
        void getByIdMapsDetail() {
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(persistedRecipe()));

            var detail = recipeService.getRecipeById(RECIPE_ID, USER_ID).orElseThrow();

            assertThat(detail.getId()).isEqualTo(RECIPE_ID);
            assertThat(detail.getTitle()).isEqualTo("Caprese Salad");
            assertThat(detail.isOwned()).isTrue();
            assertThat(detail.getCuisine()).isEqualTo("Italian");
            assertThat(detail.getIngredients()).singleElement().satisfies(dto -> {
                assertThat(dto.getName()).isEqualTo("tomato");
                assertThat(dto.getQuantity()).isEqualByComparingTo("3.00");
                assertThat(dto.getUnit()).isEqualTo("each");
                assertThat(dto.getNotes()).isEqualTo("ripe");
            });
            assertThat(detail.getSteps()).singleElement().satisfies(dto -> {
                assertThat(dto.getStepNumber()).isEqualTo(1);
                assertThat(dto.getInstruction()).isEqualTo("Slice the tomatoes.");
                assertThat(dto.getTimerMinutes()).isEqualTo(5);
            });
            // "Quick" before "vegetarian" only holds under a case-insensitive sort.
            assertThat(detail.getTags()).containsExactly("Quick", "vegetarian");
        }

        @Test
        @DisplayName("listRecipes forwards the pageable and maps the page contents")
        void listRecipesMapsPage() {
            Pageable pageable = PageRequest.of(2, 5);
            when(recipeRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(persistedRecipe()), pageable, 11));

            Page<com.wb.culinaryCode.model.recipe.rest.RecipeDTO> page =
                    recipeService.listRecipes(USER_ID, "Italian", "quick", false, pageable);

            assertThat(page.getTotalElements()).isEqualTo(11);
            assertThat(page.getNumber()).isEqualTo(2);
            assertThat(page.getContent()).singleElement().satisfies(dto -> {
                assertThat(dto.getId()).isEqualTo(RECIPE_ID);
                assertThat(dto.getTitle()).isEqualTo("Caprese Salad");
                assertThat(dto.getCuisine()).isEqualTo("Italian");
                assertThat(dto.getTags()).containsExactly("Quick", "vegetarian");
            });
            verify(recipeRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("listRecipes still queries when every filter is null")
        void listRecipesWithoutFilters() {
            Pageable pageable = PageRequest.of(0, 12);
            when(recipeRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            var page = recipeService.listRecipes(USER_ID, null, null, false, pageable);

            assertThat(page.getContent()).isEmpty();
            verify(recipeRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("getRecipesByIds maps every recipe the repository returns")
        void getRecipesByIdsMapsAll() {
            var ids = List.of(RECIPE_ID);
            when(recipeRepository.findAllById(ids)).thenReturn(List.of(persistedRecipe()));

            var result = recipeService.getRecipesByIds(ids, USER_ID);

            assertThat(result).singleElement().satisfies(dto -> {
                assertThat(dto.getId()).isEqualTo(RECIPE_ID);
                assertThat(dto.getTitle()).isEqualTo("Caprese Salad");
                assertThat(dto.getTags()).containsExactly("Quick", "vegetarian");
            });
        }

        @Test
        @DisplayName("a recipe with no tags maps to an empty tag list, not null")
        void nullTagsMapToEmptyList() {
            var recipe = Recipe.builder()
                .userId(USER_ID)
                    .id(RECIPE_ID)
                    .title("Untagged")
                    .build();
            when(recipeRepository.findById(RECIPE_ID)).thenReturn(java.util.Optional.of(recipe));

            var detail = recipeService.getRecipeById(RECIPE_ID, USER_ID).orElseThrow();

            assertThat(detail.getTags()).isNotNull().isEmpty();
        }
    }
}
