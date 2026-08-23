package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.recipe.RecipeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeUserRepository extends JpaRepository<RecipeUser, UUID> {

    Optional<RecipeUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<RecipeUser> findByProviderAndProviderId(String provider, String providerId);
}
