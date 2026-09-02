package com.slmanju.ceylonads.tuition;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
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

// Isolated filter metadata endpoint for the Tuition search UI - see TuitionFilterMetadataService
// for why this doesn't reuse /api/categories/{slug}/filters.
//
// Deliberately overrides ddl-auto to "validate" and points at its own private H2 database (see
// EducationTuitionCategoryRestructureTests for the full rationale) instead of the "local"/"test"
// profiles' default create-drop against the shared database. create-drop wipes the Flyway-inserted
// master data (this endpoint's actual data source) immediately after Flyway populates it; every
// other tuition test that needs fixtures works around this by calling LocalDataSeeder.run(), which
// recreates its own separate, hand-written School Tuition subject/grade as free-text TEXT
// attributes - the exact bug this endpoint's fix does not apply to. That workaround can't be used
// here since the whole point of this class is to verify the real V10 master-data migration output.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:tuition-filter-metadata-test;DB_CLOSE_DELAY=-1"
})
class TuitionFilterMetadataTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void resetStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    void returnsOk() throws Exception {
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsNonEmptySubjectsAcrossTheWholeVertical() throws Exception {
        // Spans School Tuition, Language Classes, Music, Dancing, Technology & Coding and Other
        // Education & Tuition - not just one child category (see V10 migration).
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[?(@.value == 'PHYSICS')].label").value("Physics"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'CHEMISTRY')].label").value("Chemistry"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'MATHEMATICS')].label").value("Mathematics"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'COMBINED_MATHEMATICS')].label").value("Combined Mathematics"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'SPOKEN_ENGLISH')].label").value("Spoken English"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'IELTS')].label").value("IELTS"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'PIANO')].label").value("Piano"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'GUITAR')].label").value("Guitar"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'KANDYAN_DANCING')].label").value("Kandyan Dancing"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'BHARATANATYAM')].label").value("Bharatanatyam"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'ART')].label").value("Art"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'CHESS')].label").value("Chess"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'CODING')].label").value("Coding"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'PROGRAMMING')].label").value("Programming"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'KARATE')].label").value("Karate"))
                .andExpect(jsonPath("$.subjects[?(@.value == 'SWIMMING')].label").value("Swimming"));
    }

    @Test
    void doesNotDuplicateASubjectSharedByMultipleCategories() throws Exception {
        // ACCOUNTING is attached to both School Tuition and Professional Courses - the merged
        // list must dedupe by stable value, not list it twice.
        String response = mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int occurrences = response.split("\"ACCOUNTING\"", -1).length - 1;
        assertTrue(occurrences == 1, "expected ACCOUNTING to appear exactly once, found " + occurrences + " in: " + response);
    }

    @Test
    void returnsAllLevelOptions() throws Exception {
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levels[?(@.value == 'PRIMARY')].label").value("Primary"))
                .andExpect(jsonPath("$.levels[?(@.value == 'GRADE_6_9')].label").value("Grade 6-9"))
                .andExpect(jsonPath("$.levels[?(@.value == 'OL')].label").value("O/L"))
                .andExpect(jsonPath("$.levels[?(@.value == 'AL')].label").value("A/L"))
                .andExpect(jsonPath("$.levels[?(@.value == 'IGCSE')].label").value("IGCSE"))
                .andExpect(jsonPath("$.levels[?(@.value == 'AS_LEVEL')].label").value("AS Level"))
                .andExpect(jsonPath("$.levels[?(@.value == 'A_LEVEL')].label").value("A Level"))
                .andExpect(jsonPath("$.levels[?(@.value == 'UNIVERSITY')].label").value("University"))
                .andExpect(jsonPath("$.levels[?(@.value == 'PROFESSIONAL')].label").value("Professional"));
    }

    @Test
    void levelsAreOrderedByDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levels[0].value").value("PRIMARY"))
                .andExpect(jsonPath("$.levels[1].value").value("GRADE_6_9"))
                .andExpect(jsonPath("$.levels[2].value").value("OL"))
                .andExpect(jsonPath("$.levels[3].value").value("AL"))
                .andExpect(jsonPath("$.levels[8].value").value("PROFESSIONAL"));
    }

    @Test
    void curriculaKeepStableValuesWithCorrectedLabels() throws Exception {
        // LOCAL/EDEXCEL values are preserved (existing ad_attribute_values compatibility) with
        // corrected display labels; CAMBRIDGE/IB/PROFESSIONAL labels were already correct.
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curricula[?(@.value == 'LOCAL')].label").value("Sri Lankan National"))
                .andExpect(jsonPath("$.curricula[?(@.value == 'EDEXCEL')].label").value("Pearson Edexcel"))
                .andExpect(jsonPath("$.curricula[?(@.value == 'CAMBRIDGE')].label").value("Cambridge"))
                .andExpect(jsonPath("$.curricula[?(@.value == 'IB')].label").value("IB"))
                .andExpect(jsonPath("$.curricula[?(@.value == 'PROFESSIONAL')].label").value("Professional"))
                .andExpect(jsonPath("$.curricula[0].value").value("LOCAL"))
                .andExpect(jsonPath("$.curricula[1].value").value("CAMBRIDGE"))
                .andExpect(jsonPath("$.curricula[2].value").value("EDEXCEL"));
    }

    @Test
    void returnsMediums() throws Exception {
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediums[?(@.value == 'SINHALA')].label").value("Sinhala"))
                .andExpect(jsonPath("$.mediums[?(@.value == 'ENGLISH')].label").value("English"))
                .andExpect(jsonPath("$.mediums[?(@.value == 'TAMIL')].label").value("Tamil"));
    }

    @Test
    void deliveryModesKeepBothAsStableValueAndIncludeHomeVisit() throws Exception {
        // BOTH is preserved (Option A from the task) rather than renamed to HYBRID, so no existing
        // ad using BOTH is broken; HOME_VISIT is newly added.
        mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryModes[?(@.value == 'PHYSICAL')].label").value("Physical"))
                .andExpect(jsonPath("$.deliveryModes[?(@.value == 'ONLINE')].label").value("Online"))
                .andExpect(jsonPath("$.deliveryModes[?(@.value == 'BOTH')].label").value("Online & Physical"))
                .andExpect(jsonPath("$.deliveryModes[?(@.value == 'HOME_VISIT')].label").value("Home Visit"))
                .andExpect(jsonPath("$.deliveryModes[0].value").value("PHYSICAL"))
                .andExpect(jsonPath("$.deliveryModes[1].value").value("ONLINE"))
                .andExpect(jsonPath("$.deliveryModes[2].value").value("BOTH"))
                .andExpect(jsonPath("$.deliveryModes[3].value").value("HOME_VISIT"));
    }

    @Test
    void doesNotReturnUnrelatedCategoryAttributes() throws Exception {
        // brand/condition/fuelType belong to mobiles/cars, not tuition.
        String response = mockMvc.perform(get("/api/tuition/filters"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(!response.contains("\"HYBRID\""), "should not leak car fuelType options: " + response);
    }

    @Test
    void usesABoundedQueryCount() throws Exception {
        statistics.clear();
        mockMvc.perform(get("/api/tuition/filters")).andExpect(status().isOk());

        long actual = statistics.getPrepareStatementCount();
        assertTrue(actual <= 4, "GET /api/tuition/filters issued " + actual + " SQL statements, expected <= 4");
    }
}
