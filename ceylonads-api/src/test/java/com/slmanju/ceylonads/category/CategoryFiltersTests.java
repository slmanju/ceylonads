package com.slmanju.ceylonads.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class CategoryFiltersTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsFilterableAttributesForACategory() throws Exception {
        mockMvc.perform(get("/api/categories/cars/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.slug").value("cars"))
                .andExpect(jsonPath("$.filters[?(@.key == 'fuelType')]").exists())
                .andExpect(jsonPath("$.filters[?(@.key == 'fuelType')].dataType").value("SELECT"))
                .andExpect(jsonPath("$.filters[?(@.key == 'fuelType')].options[?(@.value == 'HYBRID')]").exists());
    }

    @Test
    void excludesNonFilterableAttributes() throws Exception {
        // "model" on Cars is TEXT and filterable=false (see LocalDataSeeder#seedCarAttributes).
        mockMvc.perform(get("/api/categories/cars/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters[?(@.key == 'model')]").doesNotExist());
    }

    @Test
    void doesNotLeakAnotherCategorysAttributes() throws Exception {
        mockMvc.perform(get("/api/categories/cars/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters[?(@.key == 'propertyType')]").doesNotExist())
                .andExpect(jsonPath("$.filters[?(@.key == 'bedrooms')]").doesNotExist());
    }

    @Test
    void unknownCategorySlugIsNotFound() throws Exception {
        mockMvc.perform(get("/api/categories/not-a-real-category/filters"))
                .andExpect(status().isNotFound());
    }
}
