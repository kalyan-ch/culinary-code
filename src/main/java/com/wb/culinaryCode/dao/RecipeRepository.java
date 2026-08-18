package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.recipe.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    Page<Recipe> findByUserId(UUID userId, Pageable pageable);
    Page<Recipe> findByCuisine(String cuisine, Pageable pageable);
    Page<Recipe> findByUserIdAndCuisine(UUID userId, String cuisine, Pageable pageable);
}
