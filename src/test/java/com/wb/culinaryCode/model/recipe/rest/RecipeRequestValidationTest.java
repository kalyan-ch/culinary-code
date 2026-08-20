package com.wb.culinaryCode.model.recipe.rest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Bean-validation rules on the request DTOs, independent of Spring MVC. */
class RecipeRequestValidationTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void openValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static Set<String> violatedPaths(Object candidate) {
        return validator.validate(candidate).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static IngredientsDTO ingredient(String name, String quantity) {
        return IngredientsDTO.builder()
                .name(name)
                .quantity(quantity == null ? null : new BigDecimal(quantity))
                .build();
    }

    private static RecipeCreateRequest.RecipeCreateRequestBuilder validCreate() {
        return RecipeCreateRequest.builder()
                .title("Caprese Salad")
                .servings(2)
                .prepTimeMinutes(10)
                .cookTimeMinutes(0)
                .ingredients(List.of(ingredient("tomato", "3")))
                .steps(List.of(RecipeStepDTO.builder().stepNumber(1).instruction("Slice.").build()));
    }

    private static RecipeUpdateRequest.RecipeUpdateRequestBuilder validUpdate() {
        return RecipeUpdateRequest.builder()
                .title("Caprese Salad")
                .servings(2)
                .prepTimeMinutes(10)
                .cookTimeMinutes(0)
                .ingredients(List.of(ingredient("tomato", "3")))
                .steps(List.of(RecipeStepDTO.builder().stepNumber(1).instruction("Slice.").build()));
    }

    @Nested
    @DisplayName("RecipeCreateRequest")
    class CreateRequest {

        @Test
        @DisplayName("a fully populated request has no violations")
        void validRequestPasses() {
            assertThat(violatedPaths(validCreate().build())).isEmpty();
        }

        @Test
        @DisplayName("a minimal request needs only a title and userId")
        void minimalRequestPasses() {
            assertThat(violatedPaths(RecipeCreateRequest.builder()
                    .title("Toast")
                    .build())).isEmpty();
        }

        @Test
        @DisplayName("title must not be null, empty or whitespace")
        void titleMustNotBeBlank() {
            assertThat(violatedPaths(validCreate().title(null).build())).contains("title");
            assertThat(violatedPaths(validCreate().title("").build())).contains("title");
            assertThat(violatedPaths(validCreate().title("   ").build())).contains("title");
        }

        @Test
        @DisplayName("servings must be positive — zero and negatives are rejected")
        void servingsMustBePositive() {
            assertThat(violatedPaths(validCreate().servings(0).build())).contains("servings");
            assertThat(violatedPaths(validCreate().servings(-3).build())).contains("servings");
            assertThat(violatedPaths(validCreate().servings(1).build())).doesNotContain("servings");
            assertThat(violatedPaths(validCreate().servings(null).build())).doesNotContain("servings");
        }

        @Test
        @DisplayName("prep and cook times may be zero but not negative")
        void timesMayBeZero() {
            assertThat(violatedPaths(validCreate().prepTimeMinutes(0).cookTimeMinutes(0).build()))
                    .isEmpty();
            var negative = violatedPaths(validCreate().prepTimeMinutes(-1).cookTimeMinutes(-1).build());
            assertThat(negative).contains("prepTimeMinutes", "cookTimeMinutes");
        }

        @Test
        @DisplayName("nested ingredient violations are reported with an indexed path")
        void nestedIngredientViolations() {
            var paths = violatedPaths(validCreate()
                    .ingredients(List.of(ingredient("tomato", "3"), ingredient(" ", "-1")))
                    .build());

            assertThat(paths).contains("ingredients[1].name", "ingredients[1].quantity");
            assertThat(paths).noneMatch(p -> p.startsWith("ingredients[0]"));
        }

        @Test
        @DisplayName("nested step violations are reported with an indexed path")
        void nestedStepViolations() {
            var paths = violatedPaths(validCreate()
                    .steps(List.of(
                            RecipeStepDTO.builder().stepNumber(1).instruction("Slice.").build(),
                            RecipeStepDTO.builder().stepNumber(2).instruction("").build()))
                    .build());

            assertThat(paths).contains("steps[1].instruction");
            assertThat(paths).noneMatch(p -> p.startsWith("steps[0]"));
        }

        @Test
        @DisplayName("cuisine, description and tags are unconstrained")
        void freeTextFieldsUnconstrained() {
            assertThat(violatedPaths(validCreate()
                    .cuisine("Some cuisine that is not on any list")
                    .description("")
                    .tags(List.of("", "   "))
                    .build())).isEmpty();
        }

        @Test
        @DisplayName("a quantity too large for NUMERIC(10,2) is NOT caught by validation")
        void oversizedQuantityIsNotRejected() {
            // recipe_ingredients.quantity is NUMERIC(10,2), so anything >= 10^8 fails at INSERT
            // time with a DataIntegrityViolationException that the advice turns into a 500.
            // Documented here because nothing in the DTO layer stops it.
            assertThat(violatedPaths(validCreate()
                    .ingredients(List.of(ingredient("flour", "999999999")))
                    .build())).isEmpty();
        }
    }

    @Nested
    @DisplayName("RecipeUpdateRequest")
    class UpdateRequest {

        @Test
        @DisplayName("a fully populated request has no violations")
        void validRequestPasses() {
            assertThat(violatedPaths(validUpdate().build())).isEmpty();
        }

        @Test
        @DisplayName("title must not be blank")
        void titleMustNotBeBlank() {
            assertThat(violatedPaths(validUpdate().title("  ").build())).contains("title");
        }

        @Test
        @DisplayName("carries no userId constraint — ownership is not re-sent on update")
        void hasNoUserIdConstraint() {
            assertThat(violatedPaths(validUpdate().build())).isEmpty();
            assertThat(RecipeUpdateRequest.class.getDeclaredFields())
                    .noneMatch(f -> f.getName().equals("userId"));
        }

        @Test
        @DisplayName("servings must be positive and times non-negative")
        void numericConstraints() {
            assertThat(violatedPaths(validUpdate().servings(0).build())).contains("servings");
            assertThat(violatedPaths(validUpdate().prepTimeMinutes(-1).build()))
                    .contains("prepTimeMinutes");
            assertThat(violatedPaths(validUpdate().cookTimeMinutes(-1).build()))
                    .contains("cookTimeMinutes");
        }

        @Test
        @DisplayName("nested ingredient and step violations are reported with indexed paths")
        void nestedViolations() {
            var paths = violatedPaths(validUpdate()
                    .ingredients(List.of(ingredient(null, "1")))
                    .steps(List.of(RecipeStepDTO.builder().instruction(null).build()))
                    .build());

            assertThat(paths).contains("ingredients[0].name", "steps[0].instruction");
        }
    }

    @Nested
    @DisplayName("IngredientsDTO")
    class Ingredients {

        @Test
        @DisplayName("name is mandatory, quantity is optional but must not be negative")
        void nameAndQuantityRules() {
            assertThat(violatedPaths(ingredient("tomato", null))).isEmpty();
            assertThat(violatedPaths(ingredient("tomato", "0"))).isEmpty();
            assertThat(violatedPaths(ingredient("tomato", "0.25"))).isEmpty();
            assertThat(violatedPaths(ingredient(null, "1"))).containsExactly("name");
            assertThat(violatedPaths(ingredient("tomato", "-0.5"))).containsExactly("quantity");
        }
    }

    @Nested
    @DisplayName("RecipeStepDTO")
    class Steps {

        @Test
        @DisplayName("instruction is mandatory; stepNumber and timerMinutes are free")
        void instructionMandatory() {
            assertThat(violatedPaths(RecipeStepDTO.builder()
                    .instruction("Stir")
                    .build())).isEmpty();
            assertThat(violatedPaths(RecipeStepDTO.builder()
                    .stepNumber(-4)
                    .instruction("Stir")
                    .timerMinutes(-10)
                    .build())).isEmpty();
            assertThat(violatedPaths(RecipeStepDTO.builder()
                    .stepNumber(1)
                    .instruction("   ")
                    .build())).containsExactly("instruction");
        }
    }
}
