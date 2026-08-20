package com.wb.culinaryCode.dao.spec;

import com.wb.culinaryCode.model.recipe.Recipe;
import com.wb.culinaryCode.model.recipe.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeSpecificationsTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private Root<Recipe> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Test
    @DisplayName("visibleTo ORs ownership together with the published flag")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void visibleToCombinesOwnershipAndPublished() {
        Path<Object> userPath = mock(Path.class);
        Path publishedPath = mock(Path.class);
        Predicate owned = mock(Predicate.class);
        Predicate published = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(root.get("userId")).thenReturn(userPath);
        when(root.get("published")).thenReturn(publishedPath);
        when(cb.equal(userPath, USER_ID)).thenReturn(owned);
        when(cb.isTrue(publishedPath)).thenReturn(published);
        when(cb.or(owned, published)).thenReturn(combined);

        assertThat(RecipeSpecifications.visibleTo(USER_ID).toPredicate(root, query, cb)).isSameAs(combined);
    }

    @Test
    @DisplayName("ownedBy matches the userId column exactly")
    void ownedByMatchesColumn() {
        Path<Object> path = mock(Path.class);
        Predicate expected = mock(Predicate.class);
        when(root.get("userId")).thenReturn(path);
        when(cb.equal(path, USER_ID)).thenReturn(expected);

        var predicate = RecipeSpecifications.ownedBy(USER_ID).toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(expected);
        verify(cb).equal(path, USER_ID);
    }

    @Test
    @DisplayName("hasCuisine(null) contributes no predicate")
    void hasCuisineNull() {
        assertThat(RecipeSpecifications.hasCuisine(null).toPredicate(root, query, cb)).isNull();

        verifyNoInteractions(cb);
        verifyNoInteractions(root);
    }

    @Test
    @DisplayName("hasCuisine matches the cuisine column exactly, preserving case")
    void hasCuisineMatchesColumn() {
        Path<Object> path = mock(Path.class);
        Predicate expected = mock(Predicate.class);
        when(root.get("cuisine")).thenReturn(path);
        when(cb.equal(path, "Italian")).thenReturn(expected);

        var predicate = RecipeSpecifications.hasCuisine("Italian").toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(expected);
        verify(cb).equal(path, "Italian");
    }

    @Test
    @DisplayName("hasTag(null) contributes no predicate and leaves the query alone")
    void hasTagNull() {
        assertThat(RecipeSpecifications.hasTag(null).toPredicate(root, query, cb)).isNull();

        verifyNoInteractions(cb);
        verifyNoInteractions(root);
        verify(query, never()).distinct(true);
    }

    @Test
    @DisplayName("hasTag joins tags, de-duplicates the result set and compares lower-cased names")
    @SuppressWarnings("unchecked")
    void hasTagJoinsAndLowercases() {
        Join<Recipe, Tag> tagsJoin = mock(Join.class);
        Path<Object> namePath = mock(Path.class);
        Expression<String> loweredName = mock(Expression.class);
        Predicate expected = mock(Predicate.class);

        when(root.join("tags")).thenReturn((Join) tagsJoin);
        when(tagsJoin.get("name")).thenReturn(namePath);
        when(cb.lower(any())).thenReturn(loweredName);
        when(cb.equal(loweredName, "quick")).thenReturn(expected);

        var predicate = RecipeSpecifications.hasTag("QuIcK").toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(expected);
        verify(query).distinct(true);
        verify(root).join("tags");
        // both sides lower-cased: the column via cb.lower, the argument via toLowerCase
        verify(cb).lower(any());
        verify(cb).equal(loweredName, "quick");
    }
}
