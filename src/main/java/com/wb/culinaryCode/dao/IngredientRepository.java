package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.recipe.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    /** Curated ingredients are the ones with no owner. */
    @Query("SELECT i FROM Ingredient i WHERE i.userId IS NULL AND lower(i.name) = lower(:name)")
    Optional<Ingredient> findCommonByName(@Param("name") String name);

    @Query("SELECT i FROM Ingredient i WHERE i.userId = :userId AND lower(i.name) = lower(:name)")
    Optional<Ingredient> findOwnedByName(@Param("userId") UUID userId, @Param("name") String name);

    /** What the viewer may pick from: the curated catalogue plus anything they invented. */
    @Query("SELECT i FROM Ingredient i WHERE i.userId IS NULL OR i.userId = :viewerId")
    List<Ingredient> findVisibleTo(@Param("viewerId") UUID viewerId);
}
