package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression coverage for AttributeFilterValidator only checking the first same-key
// AttributeDefinition it found instead of all of them - "subject" has one definition row per
// tuition category, so a value like CHESS (only defined under Other Education & Tuition) was
// rejected whenever a different category's subject definition (e.g. School Tuition's) happened to
// be picked first. See AttributeFilterValidator.validate() for the fix.
//
// Uses the same private-H2/ddl-auto=validate setup as TuitionFilterMetadataTests rather than
// LocalDataSeeder: the bug only reproduces against the real V10 master data, since
// LocalDataSeeder's hand-written fixtures model subject/grade as free-text TEXT attributes, not
// the per-category SELECT definitions this bug depends on.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:tuition-search-validation-test;DB_CLOSE_DELAY=-1"
})
class TuitionSearchAttributeValidationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchAcceptsANonAcademicSubjectOnlyDefinedUnderOneCategory() throws Exception {
        mockMvc.perform(get("/api/tuition/classes/search").param("attr.subject", "CHESS"))
                .andExpect(status().isOk());
    }

    @Test
    void searchAcceptsAnotherNonAcademicSubjectFromTheSameCategory() throws Exception {
        mockMvc.perform(get("/api/tuition/classes/search").param("attr.subject", "KARATE"))
                .andExpect(status().isOk());
    }

    @Test
    void searchStillAcceptsAnAcademicSubject() throws Exception {
        mockMvc.perform(get("/api/tuition/classes/search").param("attr.subject", "PHYSICS"))
                .andExpect(status().isOk());
    }

    @Test
    void searchRejectsAnUnknownSubjectWithAConciseMessage() throws Exception {
        mockMvc.perform(get("/api/tuition/classes/search").param("attr.subject", "NOT_A_REAL_SUBJECT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid subject value: NOT_A_REAL_SUBJECT"));
    }

    @Test
    void everySubjectAdvertisedByFilterMetadataIsAcceptedBySearch() throws Exception {
        String filtersResponse = mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode subjects = objectMapper.readTree(filtersResponse).get("subjects");
        assertTrue(subjects.size() > 0, "expected at least one subject option from /api/tuition/filters");

        for (JsonNode subject : subjects) {
            String value = subject.get("value").asText();
            mockMvc.perform(get("/api/tuition/classes/search").param("attr.subject", value))
                    .andExpect(status().isOk());
        }
    }
}
