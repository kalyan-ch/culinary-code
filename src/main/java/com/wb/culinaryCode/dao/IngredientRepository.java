package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.recipe.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    public Ingredient findByNameContainingIgnoreCase(String name);
    public Ingredient findByName(String name);
}
