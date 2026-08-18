package com.wb.culinaryCode.dao;

import com.wb.culinaryCode.model.recipe.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Tag findByNameIgnoreCase(String name);
}
