package com.slmanju.ceylonads.category;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression guard for the V9__education_tuition_category_restructure.sql master-data migration.
//
// Deliberately overrides ddl-auto to "validate" and points at its own private H2 database, only
// for this test class's own Spring context (via @TestPropertySource - no shared config is
// touched), instead of the "local"/"test" profiles' default create-drop against the shared
// "ceylonads-test" database. Two compounding reasons:
//   1. create-drop makes Hibernate re-create every JPA-mapped table immediately after Flyway
//      populates them, wiping the Flyway-inserted master data (categories/attribute
//      definitions/options) before any test body runs. Every other test in this codebase that
//      needs category/attribute fixtures works around this by calling LocalDataSeeder.run(),
//      which recreates its own separate, hand-written subset of the tree (e.g. Education &
//      Tuition -> School Tuition only) rather than exercising the real Flyway migration.
//   2. The shared "ceylonads-test" H2 database uses DB_CLOSE_DELAY=-1, which keeps it alive across
//      Spring context boundaries for the whole test JVM - so even a ddl-auto override alone isn't
//      enough, since another test class may already have mutated that same physical database
//      (via LocalDataSeeder) before this class's context starts. A private database name makes
//      this class's view of Flyway's output independent of suite run order.
// Neither workaround can be used here since the whole point of this class is to verify what V9
// itself actually inserts.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:education-tuition-restructure-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class EducationTuitionCategoryRestructureTests {

    private static final Set<String> EXPECTED_EDUCATION_TUITION_CHILDREN = Set.of(
            "school-tuition", "higher-education", "language-classes", "professional-courses",
            "music", "dancing", "drama-theatre", "art-creative-classes", "technology-coding",
            "other-education-tuition");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void educationTuitionHasExactlyTheExpectedActiveChildren() throws Exception {
        JsonNode categories = fetchActiveCategories();

        JsonNode root = findBySlug(categories, "education-tuition");
        assertTrue(root != null, "education-tuition root category not found");
        long rootId = root.get("id").asLong();

        Set<String> actualChildren = StreamSupport.stream(categories.spliterator(), false)
                .filter(c -> !c.get("parentId").isNull() && c.get("parentId").asLong() == rootId)
                .map(c -> c.get("slug").asText())
                .collect(Collectors.toSet());

        assertEquals(EXPECTED_EDUCATION_TUITION_CHILDREN, actualChildren);
    }

    @Test
    void onlineCoursesIsNoLongerAnActiveCategory() throws Exception {
        JsonNode categories = fetchActiveCategories();
        assertTrue(findBySlug(categories, "online-courses") == null,
                "online-courses should have been deactivated");
    }

    @Test
    void educationTuitionSubtreeStaysTwoLevelsDeep() throws Exception {
        JsonNode categories = fetchActiveCategories();
        JsonNode root = findBySlug(categories, "education-tuition");
        assertTrue(root != null, "education-tuition root category not found");
        long rootId = root.get("id").asLong();

        Set<Long> childIds = StreamSupport.stream(categories.spliterator(), false)
                .filter(c -> !c.get("parentId").isNull() && c.get("parentId").asLong() == rootId)
                .map(c -> c.get("id").asLong())
                .collect(Collectors.toSet());

        boolean anyGrandchild = StreamSupport.stream(categories.spliterator(), false)
                .anyMatch(c -> !c.get("parentId").isNull() && childIds.contains(c.get("parentId").asLong()));

        assertTrue(!anyGrandchild, "Education & Tuition must remain exactly two levels deep");
    }

    @Test
    void noDuplicateCategorySlugsExistAnywhere() throws Exception {
        JsonNode categories = fetchActiveCategories();
        Set<String> seen = new HashSet<>();
        for (JsonNode c : categories) {
            String slug = c.get("slug").asText();
            assertTrue(seen.add(slug), "duplicate category slug found: " + slug);
        }
    }

    @Test
    void newCategoriesExposeSubjectAndClassModeAttributes() throws Exception {
        for (String slug : new String[] {"music", "dancing", "drama-theatre", "art-creative-classes", "technology-coding"}) {
            mockMvc.perform(get("/api/categories/" + slug + "/attributes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.key == 'subject')]").exists())
                    .andExpect(jsonPath("$[?(@.key == 'classMode')].options[?(@.value == 'ONLINE')]").exists());
        }
    }

    @Test
    void otherEducationTuitionHasNoAttributeDefinitions() throws Exception {
        mockMvc.perform(get("/api/categories/other-education-tuition/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void genericFiltersEndpointStillWorksForANewCategory() throws Exception {
        mockMvc.perform(get("/api/categories/dancing/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.slug").value("dancing"))
                .andExpect(jsonPath("$.filters[?(@.key == 'classMode')]").exists());
    }

    private JsonNode fetchActiveCategories() throws Exception {
        String response = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode findBySlug(JsonNode categories, String slug) {
        for (JsonNode c : categories) {
            if (c.get("slug").asText().equals(slug)) {
                return c;
            }
        }
        return null;
    }
}
