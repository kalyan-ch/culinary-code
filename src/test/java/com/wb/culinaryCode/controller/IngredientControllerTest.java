package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.controller.advice.GlobalExceptionHandler;
import com.wb.culinaryCode.model.recipe.rest.IngredientOptionDTO;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.service.IngredientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** See {@link RecipeControllerTest} for why this uses standaloneSetup instead of {@code @WebMvcTest}. */
class IngredientControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final AuthUser SIGNED_IN =
            new AuthUser(USER_ID, "alice@example.com", "Alice Baker", "hash");

    private IngredientService ingredientService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ingredientService = mock(IngredientService.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SIGNED_IN, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new IngredientController(ingredientService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/ingredients returns the catalogue, default unit included")
    void listIngredients() throws Exception {
        var garlicId = UUID.randomUUID();
        when(ingredientService.listIngredients(USER_ID)).thenReturn(List.of(
                IngredientOptionDTO.builder()
                        .id(garlicId).name("garlic").category("produce").defaultUnit("clove").build(),
                IngredientOptionDTO.builder()
                        .id(UUID.randomUUID()).name("plain flour").category("pantry").defaultUnit("g").build()));

        mockMvc.perform(get("/api/v1/ingredients"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(garlicId.toString()))
                .andExpect(jsonPath("$[0].name").value("garlic"))
                .andExpect(jsonPath("$[0].defaultUnit").value("clove"))
                .andExpect(jsonPath("$[1].name").value("plain flour"));

        verify(ingredientService).listIngredients(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/ingredients returns an empty array when the catalogue is empty")
    void listIngredientsEmpty() throws Exception {
        when(ingredientService.listIngredients(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ingredients"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("a service failure is reported as a generic 500")
    void serviceFailureBecomes500() throws Exception {
        when(ingredientService.listIngredients(USER_ID)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/v1/ingredients"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
