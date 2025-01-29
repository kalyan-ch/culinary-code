package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.RecipeRepository;
import com.wb.culinaryCode.model.Recipe;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InitDataGenerator {

    private final RecipeRepository recipeRepository;

    @PostConstruct
    @Transactional
    public void generateData() {
        String[] recipeNames = {"Chicken Dum Biryani", "Chicken Curry Andhra Style", "Gutti Vankaya Curry",
                "Paneer Butter Masala", "Paneer Tikka", "Mutton Gongura Biryani", "Mutton Curry",
                "Jalapeno Hummus", "Chama Dumpa Fry", "Pulihora"};
        String[][] cuisines = {{"Indian", "South Indian"}, {"Indian", "South Indian"}, {"Indian", "South Indian", "Vegetarian"},
                {"North Indian", "Vegetarian"}, {"North Indian", "Vegetarian"}, {"Indian", "South Indian"},
                {"Indian", "South Indian"}, {"Mediterranean", "Vegetarian"}, {"Indian", "South Indian", "Vegetarian"},
                {"Indian", "South Indian", "Vegetarian"}};

        IntStream.range(0, 10).forEach(i ->
                createRecipe(recipeNames[i], recipeNames[i], Arrays.asList(cuisines[i]), "Method for " + recipeNames[i]));
    }

    private void createRecipe(String name, String desc,
                              List<String> cuisines, String method) {
        var recipe = Recipe.builder()
                .name(name)
                .description(desc)
                .method(method)
                .cuisines(cuisines)
                .servings(2)
                .createdAt(Instant.now())
                .cookTime(30)
                .userId(2341L)
                .build();

        recipeRepository.save(recipe);
    }

}
