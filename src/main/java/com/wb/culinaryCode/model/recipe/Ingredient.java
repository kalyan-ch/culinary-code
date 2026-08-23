package com.wb.culinaryCode.model.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String category;

    @Column(name = "default_unit")
    private String defaultUnit;

    /** {@code null} marks a curated ingredient shared by everyone; otherwise its creator. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
