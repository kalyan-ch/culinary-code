package com.wb.culinaryCode.service;

import com.wb.culinaryCode.config.CulinaryCodeConfig;
import com.wb.culinaryCode.dao.IngredientRepository;
import com.wb.culinaryCode.model.recipe.Ingredient;
import com.wb.culinaryCode.model.recipe.rest.IngredientOptionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        ingredientService = new IngredientService(ingredientRepository, new CulinaryCodeConfig().modelMapper());
    }

    private static Ingredient ingredient(String name) {
        return Ingredient.builder().id(UUID.randomUUID()).name(name).build();
    }

    @Test
    @DisplayName("carries the category and default unit through, which is the point of the catalogue")
    void mapsIngredientFields() {
        var stored = Ingredient.builder()
                .id(UUID.randomUUID()).name("garlic").category("produce").defaultUnit("clove").build();
        when(ingredientRepository.findVisibleTo(USER_ID)).thenReturn(List.of(stored));

        var result = ingredientService.listIngredients(USER_ID);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(stored.getId());
            assertThat(dto.getName()).isEqualTo("garlic");
            assertThat(dto.getCategory()).isEqualTo("produce");
            assertThat(dto.getDefaultUnit()).isEqualTo("clove");
        });
    }

    @Test
    @DisplayName("sorts by name ignoring case, so 'Marmite' lands between 'kale' and 'nutmeg'")
    void sortsCaseInsensitively() {
        when(ingredientRepository.findVisibleTo(USER_ID)).thenReturn(List.of(
                ingredient("nutmeg"), ingredient("Marmite"), ingredient("kale"), ingredient("Basil")));

        var result = ingredientService.listIngredients(USER_ID);

        assertThat(result).extracting(IngredientOptionDTO::getName)
                .containsExactly("Basil", "kale", "Marmite", "nutmeg");
    }

    @Test
    @DisplayName("an anonymous viewer still gets the curated catalogue")
    void anonymousViewerGetsCuratedRows() {
        when(ingredientRepository.findVisibleTo(null)).thenReturn(List.of(ingredient("garlic")));

        assertThat(ingredientService.listIngredients(null))
                .extracting(IngredientOptionDTO::getName).containsExactly("garlic");
    }

    @Test
    @DisplayName("returns an empty list when there are no ingredients")
    void returnsEmptyList() {
        when(ingredientRepository.findVisibleTo(USER_ID)).thenReturn(List.of());

        assertThat(ingredientService.listIngredients(USER_ID)).isEmpty();
    }
}
