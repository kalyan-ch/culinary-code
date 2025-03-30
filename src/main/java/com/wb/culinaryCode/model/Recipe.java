package com.wb.culinaryCode.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name ="recipe")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    private String name;
    private String description;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "method", length=2048)
    private String method;
    private String preparation;
    private int servings;

    @Column(name ="prep_time")
    private int prepTime;

    @Column(name ="cook_time")
    private int cookTime;

    @CreationTimestamp
    @Column(name ="created_at")
    private Instant createdAt;

    @Column(name ="updated_at")
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name="recipe_cuisines", joinColumns = @JoinColumn(name ="recipe_id"))
    @Column(name = "cuisine_name")
    private List<String> cuisines;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecipeIngredient> recipeIngredients;

}
