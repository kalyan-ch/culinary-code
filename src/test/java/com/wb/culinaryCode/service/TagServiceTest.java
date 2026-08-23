package com.wb.culinaryCode.service;

import com.wb.culinaryCode.config.CulinaryCodeConfig;
import com.wb.culinaryCode.dao.TagRepository;
import com.wb.culinaryCode.model.recipe.Tag;
import com.wb.culinaryCode.model.recipe.rest.TagDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository, new CulinaryCodeConfig().modelMapper());
    }

    private static Tag tag(String name) {
        return Tag.builder().id(UUID.randomUUID()).name(name).build();
    }

    @Test
    @DisplayName("maps id and name onto the DTO")
    void mapsTagFields() {
        var stored = tag("vegetarian");
        when(tagRepository.findVisibleTo(USER_ID)).thenReturn(List.of(stored));

        var result = tagService.listTags(USER_ID);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(stored.getId());
            assertThat(dto.getName()).isEqualTo("vegetarian");
        });
    }

    @Test
    @DisplayName("sorts by name ignoring case, so 'Quick' lands between 'apple' and 'zest'")
    void sortsCaseInsensitively() {
        when(tagRepository.findVisibleTo(USER_ID)).thenReturn(List.of(
                tag("zest"), tag("Quick"), tag("apple"), tag("Brine")));

        var result = tagService.listTags(USER_ID);

        assertThat(result).extracting(TagDTO::getName)
                .containsExactly("apple", "Brine", "Quick", "zest");
    }

    @Test
    @DisplayName("returns an empty list when there are no tags")
    void returnsEmptyList() {
        when(tagRepository.findVisibleTo(USER_ID)).thenReturn(List.of());

        assertThat(tagService.listTags(USER_ID)).isEmpty();
    }
}
