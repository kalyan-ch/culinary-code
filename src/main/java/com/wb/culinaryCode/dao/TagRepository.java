package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.recipe.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    /** Curated tags are the ones with no owner. */
    @Query("SELECT t FROM Tag t WHERE t.userId IS NULL AND lower(t.name) = lower(:name)")
    Optional<Tag> findCommonByName(@Param("name") String name);

    @Query("SELECT t FROM Tag t WHERE t.userId = :userId AND lower(t.name) = lower(:name)")
    Optional<Tag> findOwnedByName(@Param("userId") UUID userId, @Param("name") String name);

    /** What the viewer may pick from or filter by: the curated set plus their own. */
    @Query("SELECT t FROM Tag t WHERE t.userId IS NULL OR t.userId = :viewerId")
    List<Tag> findVisibleTo(@Param("viewerId") UUID viewerId);
}
