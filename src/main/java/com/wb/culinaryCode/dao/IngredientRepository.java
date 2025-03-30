package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    public Ingredient findByNameContainingIgnoreCase(String name);
    public Ingredient findByName(String name);
}
