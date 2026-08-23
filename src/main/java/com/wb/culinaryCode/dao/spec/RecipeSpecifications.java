package com.wb.culinaryCode.dao.spec;

import com.wb.culinaryCode.model.recipe.Recipe;
import com.wb.culinaryCode.model.recipe.Tag;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    /**
     * Everything the viewer may see: every published recipe, plus their own if they are signed
     * in. A null viewer is an anonymous visitor, who sees published recipes only.
     */
    public static Specification<Recipe> visibleTo(UUID userId) {
        return (root, query, cb) -> userId == null
                ? cb.isTrue(root.get("published"))
                : cb.or(cb.equal(root.get("userId"), userId), cb.isTrue(root.get("published")));
    }

    public static Specification<Recipe> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
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
