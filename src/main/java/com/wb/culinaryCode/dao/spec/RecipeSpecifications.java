package com.wb.culinaryCode.dao.spec;

import com.wb.culinaryCode.model.recipe.Recipe;
import com.wb.culinaryCode.model.recipe.Tag;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    public static Specification<Recipe> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Recipe> hasCuisine(String cuisine) {
        return (root, query, cb) -> cuisine == null ? null : cb.equal(root.get("cuisine"), cuisine);
    }

    public static Specification<Recipe> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null) {
                return null;
            }
            query.distinct(true);
            Join<Recipe, Tag> tagsJoin = root.join("tags");
            return cb.equal(cb.lower(tagsJoin.get("name")), tagName.toLowerCase());
        };
    }
}
