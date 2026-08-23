package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.controller.advice.GlobalExceptionHandler;
import com.wb.culinaryCode.exception.RecipeNotFoundException;
import com.wb.culinaryCode.model.recipe.rest.IngredientsDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeCreateRequest;
import com.wb.culinaryCode.model.recipe.rest.RecipeDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeDetailDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeStepDTO;
import com.wb.culinaryCode.model.recipe.rest.RecipeUpdateRequest;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.service.RecipeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC-level tests for {@link RecipeController}.
 *
 * <p>Spring Boot 4 moved {@code @WebMvcTest} into the separate {@code spring-boot-webmvc-test}
 * artifact, which this project does not depend on, so the controller is wired by hand with
 * {@link MockMvcBuilders#standaloneSetup} — same request mapping, argument resolution, validation
 * and {@code @RestControllerAdvice} handling, without a Spring context or a database.
 */
class RecipeControllerTest {

    private static final UUID RECIPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final AuthUser SIGNED_IN =
            new AuthUser(USER_ID, "alice@example.com", "Alice Baker", "hash");

    private RecipeService recipeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        recipeService = mock(RecipeService.class);

        // standaloneSetup has no security filter chain, so the principal the controller reads
        // via @AuthenticationPrincipal has to be put in place directly.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SIGNED_IN, null, List.of()));

        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new RecipeController(recipeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private String json(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    private static RecipeDetailDTO detailDto() {
        return RecipeDetailDTO.builder()
                .owned(true)
                .id(RECIPE_ID)
                .title("Caprese Salad")
                .description("Simple salad of tomato, basil, and olive oil.")
                .servings(2)
                .prepTimeMinutes(10)
                .cookTimeMinutes(0)
                .cuisine("Italian")
                .ingredients(List.of(IngredientsDTO.builder()
                        .id(UUID.randomUUID())
                        .name("tomato")
                        .unit("each")
                        .quantity(new BigDecimal("3.00"))
                        .build()))
                .steps(List.of(RecipeStepDTO.builder()
                        .stepNumber(1)
                        .instruction("Slice the tomatoes.")
                        .build()))
                .tags(List.of("quick", "vegetarian"))
                .build();
    }

    private static RecipeCreateRequest.RecipeCreateRequestBuilder validCreateRequest() {
        return RecipeCreateRequest.builder()
                .title("Caprese Salad")
                .servings(2)
                .prepTimeMinutes(10)
                .cookTimeMinutes(0)
                .ingredients(List.of(IngredientsDTO.builder()
                        .name("tomato")
                        .quantity(new BigDecimal("3"))
                        .unit("each")
                        .build()))
                .steps(List.of(RecipeStepDTO.builder()
                        .stepNumber(1)
                        .instruction("Slice the tomatoes.")
                        .build()))
                .tags(List.of("quick"));
    }

    private static RecipeUpdateRequest.RecipeUpdateRequestBuilder validUpdateRequest() {
        return RecipeUpdateRequest.builder()
                .title("Caprese Salad")
                .servings(2)
                .ingredients(List.of())
                .steps(List.of())
                .tags(List.of());
    }

    @Test
    @DisplayName("GET /{id} returns the recipe detail")
    void getByIdReturnsDetail() throws Exception {
        when(recipeService.getRecipeById(RECIPE_ID, USER_ID)).thenReturn(Optional.of(detailDto()));

        mockMvc.perform(get("/api/v1/recipe/{id}", RECIPE_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(RECIPE_ID.toString()))
                .andExpect(jsonPath("$.title").value("Caprese Salad"))
                .andExpect(jsonPath("$.owned").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("tomato"))
                .andExpect(jsonPath("$.ingredients[0].unit").value("each"))
                .andExpect(jsonPath("$.steps[0].instruction").value("Slice the tomatoes."))
                .andExpect(jsonPath("$.tags[0]").value("quick"))
                .andExpect(jsonPath("$.tags[1]").value("vegetarian"));
    }

    @Test
    @DisplayName("GET /{id} returns 404 when the service has no such recipe")
    void getByIdReturns404() throws Exception {
        when(recipeService.getRecipeById(RECIPE_ID, USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/recipe/{id}", RECIPE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /create returns 201 with a Location header and the created body")
    void createReturns201() throws Exception {
        when(recipeService.createRecipe(any(RecipeCreateRequest.class), eq(USER_ID))).thenReturn(detailDto());

        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest().build())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/recipe/" + RECIPE_ID))
                .andExpect(jsonPath("$.id").value(RECIPE_ID.toString()))
                .andExpect(jsonPath("$.title").value("Caprese Salad"));
    }

    @Test
    @DisplayName("POST /create forwards the deserialized request to the service")
    void createForwardsRequestBody() throws Exception {
        when(recipeService.createRecipe(any(RecipeCreateRequest.class), eq(USER_ID))).thenReturn(detailDto());
        var captor = ArgumentCaptor.forClass(RecipeCreateRequest.class);

        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest().cuisine("Italian").build())))
                .andExpect(status().isCreated());

        verify(recipeService).createRecipe(captor.capture(), eq(USER_ID));
        var sent = captor.getValue();
        assertThat(sent.getTitle()).isEqualTo("Caprese Salad");
        assertThat(sent.getCuisine()).isEqualTo("Italian");
        assertThat(sent.getServings()).isEqualTo(2);
        assertThat(sent.getIngredients()).singleElement().satisfies(i -> {
            assertThat(i.getName()).isEqualTo("tomato");
            assertThat(i.getQuantity()).isEqualByComparingTo("3");
            assertThat(i.getUnit()).isEqualTo("each");
        });
        assertThat(sent.getSteps()).singleElement().satisfies(s -> {
            assertThat(s.getStepNumber()).isEqualTo(1);
            assertThat(s.getInstruction()).isEqualTo("Slice the tomatoes.");
        });
        assertThat(sent.getTags()).containsExactly("quick");
    }

    @Test
    @DisplayName("POST /create rejects a blank title with a field error")
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest().title("   ").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists());

        verify(recipeService, never()).createRecipe(any(RecipeCreateRequest.class), eq(USER_ID));
    }


    @Test
    @DisplayName("POST /create rejects zero servings and negative prep/cook times")
    void createRejectsNonPositiveNumbers() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest()
                                .servings(0)
                                .prepTimeMinutes(-1)
                                .cookTimeMinutes(-5)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.servings").exists())
                .andExpect(jsonPath("$.errors.prepTimeMinutes").exists())
                .andExpect(jsonPath("$.errors.cookTimeMinutes").exists());

        verify(recipeService, never()).createRecipe(any(RecipeCreateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("POST /create rejects a blank nested ingredient name")
    void createRejectsBlankIngredientName() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest()
                                .ingredients(List.of(IngredientsDTO.builder()
                                        .name(" ")
                                        .quantity(new BigDecimal("1"))
                                        .build()))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['ingredients[0].name']").exists());

        verify(recipeService, never()).createRecipe(any(RecipeCreateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("POST /create rejects a negative ingredient quantity")
    void createRejectsNegativeQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest()
                                .ingredients(List.of(IngredientsDTO.builder()
                                        .name("tomato")
                                        .quantity(new BigDecimal("-1"))
                                        .build()))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['ingredients[0].quantity']").exists());

        verify(recipeService, never()).createRecipe(any(RecipeCreateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("POST /create rejects a blank nested step instruction")
    void createRejectsBlankStepInstruction() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest()
                                .steps(List.of(RecipeStepDTO.builder()
                                        .stepNumber(1)
                                        .instruction("")
                                        .build()))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['steps[0].instruction']").exists());

        verify(recipeService, never()).createRecipe(any(RecipeCreateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("PUT /{id} returns the updated recipe")
    void updateReturns200() throws Exception {
        when(recipeService.updateRecipe(eq(RECIPE_ID), any(RecipeUpdateRequest.class), eq(USER_ID)))
                .thenReturn(detailDto());

        mockMvc.perform(put("/api/v1/recipe/{id}", RECIPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdateRequest().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RECIPE_ID.toString()))
                .andExpect(jsonPath("$.title").value("Caprese Salad"));
    }

    @Test
    @DisplayName("PUT /{id} surfaces RecipeNotFoundException as 404 with a message")
    void updateReturns404() throws Exception {
        when(recipeService.updateRecipe(eq(RECIPE_ID), any(RecipeUpdateRequest.class), eq(USER_ID)))
                .thenThrow(new RecipeNotFoundException(RECIPE_ID));

        mockMvc.perform(put("/api/v1/recipe/{id}", RECIPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdateRequest().build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recipe not found: " + RECIPE_ID));
    }

    @Test
    @DisplayName("PUT /{id} validates the body before touching the service")
    void updateRejectsBlankTitle() throws Exception {
        mockMvc.perform(put("/api/v1/recipe/{id}", RECIPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdateRequest().title("").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verify(recipeService, never()).updateRecipe(any(UUID.class), any(RecipeUpdateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("DELETE /{id} returns 204 with an empty body")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/recipe/{id}", RECIPE_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(recipeService).deleteRecipe(RECIPE_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /{id} surfaces RecipeNotFoundException as 404")
    void deleteReturns404() throws Exception {
        doThrow(new RecipeNotFoundException(RECIPE_ID)).when(recipeService).deleteRecipe(RECIPE_ID, USER_ID);

        mockMvc.perform(delete("/api/v1/recipe/{id}", RECIPE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recipe not found: " + RECIPE_ID));
    }


    @Test
    @DisplayName("GET / passes nulls when no filters are supplied")
    void listWithoutFiltersPassesNulls() throws Exception {
        when(recipeService.listRecipes(any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/recipe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(recipeService).listRecipes(eq(USER_ID), eq(null), eq(null), anyBoolean(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /recipes returns the batch lookup results in order")
    void getRecipesByIds() throws Exception {
        var otherId = UUID.randomUUID();
        when(recipeService.getRecipesByIds(List.of(RECIPE_ID, otherId), USER_ID))
                .thenReturn(List.of(
                        RecipeDTO.builder().id(RECIPE_ID).title("Caprese Salad").build(),
                        RecipeDTO.builder().id(otherId).title("Jollof Rice").build()));

        mockMvc.perform(get("/api/v1/recipe/recipes")
                        .param("recipeIds", RECIPE_ID.toString(), otherId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Caprese Salad"))
                .andExpect(jsonPath("$[1].title").value("Jollof Rice"));
    }

    @Test
    @DisplayName("an unexpected service failure is reported as a generic 500 without leaking detail")
    void unexpectedFailureBecomes500() throws Exception {
        when(recipeService.createRecipe(any(RecipeCreateRequest.class), eq(USER_ID)))
                .thenThrow(new IllegalStateException("connection pool exhausted"));

        var body = mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest().build())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("connection pool exhausted");
    }

    /**
     * The framework's own client-error exceptions have to be handled explicitly in
     * {@link GlobalExceptionHandler}: handlers on a {@code @RestControllerAdvice} run ahead of
     * Spring's DefaultHandlerExceptionResolver, so a catch-all {@code Exception} handler alone
     * would report plainly malformed requests as 500s.
     */
    @Test
    @DisplayName("a non-UUID path variable is a 400, not a 500")
    void malformedPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/recipe/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'recipeId'"));

        verify(recipeService, never()).getRecipeById(any(UUID.class), eq(USER_ID));
    }

    @Test
    @DisplayName("a malformed JSON body is a 400, not a 500")
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(recipeService, never()).createRecipe(any(RecipeCreateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("a missing required request param is a 400, not a 500")
    void missingRequiredParamIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/recipe/recipes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'recipeIds'"));

        verify(recipeService, never()).getRecipesByIds(any(), eq(USER_ID));
    }

    @Test
    @DisplayName("a database constraint violation is reported as a 400 without leaking SQL")
    void dataIntegrityViolationIsBadRequest() throws Exception {
        when(recipeService.createRecipe(any(RecipeCreateRequest.class), eq(USER_ID)))
                .thenThrow(new DataIntegrityViolationException(
                        "could not execute statement [ERROR: numeric field overflow]"));

        var body = mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreateRequest().build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("One or more values are invalid or conflict with existing data"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("numeric field overflow");
    }

    @Test
    @DisplayName("an unsupported HTTP method is a 405, not a 500")
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/{id}", RECIPE_ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("POST is not supported by this endpoint"));
    }
}
