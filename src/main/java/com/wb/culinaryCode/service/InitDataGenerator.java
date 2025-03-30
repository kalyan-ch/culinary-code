package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.IngredientRepository;
import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.model.Ingredient;
import com.wb.culinaryCode.model.Recipe;
import com.wb.culinaryCode.model.RecipeIngredient;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InitDataGenerator {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;


    @PostConstruct
    @Transactional
    public void generateData() {

        if(recipeRepository.count() > 0) {
            return;
        }

        //"Chicken Dum Biryani", "Gutti Vankaya Curry", "Paneer Butter Masala", "Mutton Curry", "Taro Root Fry"
        if(ingredientRepository.count() <= 0) {
            insertIngredients();
        }

        var paneerButterMasala = Recipe.builder()
                .name("Paneer Butter Masala")
                .description("A north indian dish made with Paneer in a rich buttery tomato gravy")
                .cookTime(40)
                .prepTime(10)
                .method("Heat half butter and oil in a pan. Add ginger, cloves garlic, cashews, green chilies, onions, tomatoes and saute until the tomatoes are mushy.\n" +
                        "Cool and blend to a smooth paste.\n" +
                        "Heat butter in a pan. Add the blended paste and cook for 5 minutes.\n" +
                        "Add paneer, water, salt and cook for 5 minutes.\n" +
                        "Add cream, garam masala, dried fenugreek leaves and cook for 2 minutes.\n" +
                        "Serve hot garnished with coriander leaves.")
                .cuisines(Arrays.asList("North Indian", "Vegetarian"))
                .servings(3)
                .userId(234L)
                .build();

        var recipeIngredients = Arrays.asList(
            createRecipeIngredient(paneerButterMasala,"Paneer", 500.0, "gram", "cubed"),
            createRecipeIngredient(paneerButterMasala,"Butter", 2.0, "tbsp", null),
            createRecipeIngredient(paneerButterMasala,"Red Chilli Powder", 2.0, "tbsp", null),
            createRecipeIngredient(paneerButterMasala,"Cashew", 15.0, "each", null),
            createRecipeIngredient(paneerButterMasala,"Onions", 200.0, "gram", "thinly sliced"),
            createRecipeIngredient(paneerButterMasala,"Tomato", 250.0, "gram", "sliced"),
            createRecipeIngredient(paneerButterMasala,"Green Chili", 5.0, "each", "split"),
            createRecipeIngredient(paneerButterMasala,"Ginger", 2.5, "cm", "peeled and chopped"),
            createRecipeIngredient(paneerButterMasala,"Garlic", 5.0, "cloves", null),
            createRecipeIngredient(paneerButterMasala,"Dried Fenugreek Leaves", 1.0, "tbsp", null),
            createRecipeIngredient(paneerButterMasala,"Garam Masala Powder", 1.5, "tbsp", null),
            createRecipeIngredient(paneerButterMasala,"Coriander Powder", 1.5, "tbsp", null),
            createRecipeIngredient(paneerButterMasala,"Avocado Oil", 1.5, "tbsp", null)
        );

        paneerButterMasala.setRecipeIngredients(recipeIngredients);
        recipeRepository.save(paneerButterMasala);

    }

    private RecipeIngredient createRecipeIngredient(Recipe recipe, String name, Double quantity, String unit, String notes) {
        var ingredient = ingredientRepository.findByName(name);
        if (ingredient == null) {
            ingredient = ingredientRepository.findByNameContainingIgnoreCase(name);
        }

        return RecipeIngredient.builder()
                .ingredient(ingredient)
                .quantity(quantity)
                .unit(unit)
                .notes(notes)
                .recipe(recipe)
                .build();

    }

    private void insertIngredients() {
        var ingredientList = new ArrayList<Ingredient>();
        String[] ingredients = {"Mutton", "Paneer", "Avocado Oil", "Indian Egg plant", "Onions", "Tomato", "Taro Root",
                "Green Chili", "Cumin Seeds", "Butter", "Coriander leaves (Cilantro)", "Mint leaves", "Cardamom", "Garlic",
                "Ginger", "Ginger Garlic Paste", "Cashew", "Red Chilli Powder", "Garam Masala powder", "Coriander powder", "Cumin powder"};
        for(String ingredient: ingredients) {
            ingredientList.add(Ingredient.builder().name(ingredient).build());
        }

        ingredientRepository.saveAll(ingredientList);

        ingredientRepository.findByNameContainingIgnoreCase("Paneer");
    }


}
