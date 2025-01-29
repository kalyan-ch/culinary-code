package com.wb.culinaryCode.model;

import jakarta.persistence.*;
import lombok.*;
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

    private String method;
    private String preparation;
    private int servings;

    @Column(name ="prep_time")
    private int prepTime;

    @Column(name ="cook_time")
    private int cookTime;

    @Column(name ="created_at")
    private Instant createdAt;

    @Column(name ="updated_at")
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name="recipe_cuisines", joinColumns = @JoinColumn(name ="recipe_id"))
    @Column(name = "cuisine_name")
    private List<String> cuisines;

}
