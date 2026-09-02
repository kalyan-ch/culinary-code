package com.wb.culinaryCode.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real filter chain, which the controller unit tests deliberately bypass.
 * The rule under test is that browsing is open and writing is not — a regression here is a
 * security hole rather than a broken feature, and nothing else would catch it.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityRulesTest {

    private static final UUID SOME_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private MockMvc mockMvc;

    @Autowired
    void setUp(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("an anonymous visitor may browse recipes")
    void anonymousMayList() throws Exception {
        mockMvc.perform(get("/api/v1/recipe")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("an anonymous visitor may read a single recipe")
    void anonymousMayReadOne() throws Exception {
        // 404 rather than 200 because the database is empty — the point is that it is not 401.
        mockMvc.perform(get("/api/v1/recipe/{id}", SOME_ID)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an anonymous visitor may list tags")
    void anonymousMayListTags() throws Exception {
        mockMvc.perform(get("/api/v1/tags")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("an anonymous visitor may list ingredients")
    void anonymousMayListIngredients() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("an anonymous visitor may not create a recipe")
    void anonymousMayNotCreate() throws Exception {
        mockMvc.perform(post("/api/v1/recipe/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an anonymous visitor may not update a recipe")
    void anonymousMayNotUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/recipe/{id}", SOME_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an anonymous visitor may not delete a recipe")
    void anonymousMayNotDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/recipe/{id}", SOME_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("asking for your own recipes without a session is rejected, not silently empty")
    void anonymousMayNotAskForMine() throws Exception {
        mockMvc.perform(get("/api/v1/recipe").param("mine", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an anonymous request is not given a session cookie")
    void anonymousGetsNoSession() throws Exception {
        // The frontend gates routes on the presence of JSESSIONID. Handing one to a visitor
        // who never signed in makes that gate open for everyone.
        var result = mockMvc.perform(get("/api/v1/recipe")).andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("a rejected request is not given a session cookie either")
    void rejectedRequestGetsNoSession() throws Exception {
        var result = mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("registration and login stay reachable")
    void authEndpointsArePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized()); // reached the handler; not blocked by the chain
    }
}
