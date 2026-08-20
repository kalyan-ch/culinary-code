package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.controller.advice.GlobalExceptionHandler;
import com.wb.culinaryCode.model.recipe.rest.TagDTO;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.service.TagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** See {@link RecipeControllerTest} for why this uses standaloneSetup instead of {@code @WebMvcTest}. */
class TagControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final AuthUser SIGNED_IN =
            new AuthUser(USER_ID, "alice@example.com", "Alice Baker", "hash");

    private TagService tagService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tagService = mock(TagService.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SIGNED_IN, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new TagController(tagService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/tags returns the tag list")
    void listTags() throws Exception {
        var quickId = UUID.randomUUID();
        when(tagService.listTags(USER_ID)).thenReturn(List.of(
                TagDTO.builder().id(quickId).name("quick").build(),
                TagDTO.builder().id(UUID.randomUUID()).name("vegetarian").build()));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(quickId.toString()))
                .andExpect(jsonPath("$[0].name").value("quick"))
                .andExpect(jsonPath("$[1].name").value("vegetarian"));

        verify(tagService).listTags(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/tags returns an empty array when there are no tags")
    void listTagsEmpty() throws Exception {
        when(tagService.listTags(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("a service failure is reported as a generic 500")
    void serviceFailureBecomes500() throws Exception {
        when(tagService.listTags(USER_ID)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
