package com.slmanju.ceylonads.location;

import com.slmanju.ceylonads.location.entity.Location;
import com.slmanju.ceylonads.location.entity.LocationType;
import com.slmanju.ceylonads.location.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the V14/V15 location master-data expansions: every canonical Sri Lankan district must
 * still be present, new cities must hang off a real district with no duplicates, and ONLINE must
 * never appear as a location (it is a tuition delivery-mode attribute, not a place).
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class LocationMasterDataTests {

    private static final Set<String> CANONICAL_DISTRICTS = Set.of(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo", "Galle", "Gampaha",
            "Hambantota", "Jaffna", "Kalutara", "Kandy", "Kegalle", "Kilinochchi", "Kurunegala",
            "Mannar", "Matale", "Matara", "Moneragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya");

    @Autowired
    private LocationRepository locations;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allCanonicalDistrictsExist() {
        Set<String> districtNames = locations.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(l -> l.getType() == LocationType.DISTRICT)
                .map(Location::getName)
                .collect(Collectors.toSet());

        for (String district : CANONICAL_DISTRICTS) {
            assertTrue(districtNames.contains(district), "Missing canonical district: " + district);
        }
    }

    @Test
    void newCitiesHaveValidDistrictParent() {
        Map<String, String> expectedParentSlug = Map.ofEntries(
                Map.entry("nawala", "colombo-district"),
                Map.entry("kirulapone", "colombo-district"),
                Map.entry("pannipitiya", "colombo-district"),
                Map.entry("wellawatte", "colombo-district"),
                Map.entry("avissawella", "colombo-district"),
                Map.entry("katana", "gampaha-district"),
                Map.entry("weliweriya", "gampaha-district"),
                // V15 (panthi.lk reference cross-check)
                Map.entry("kotahena", "colombo-district"),
                Map.entry("wellampitiya", "colombo-district"),
                Map.entry("peliyagoda", "gampaha-district"),
                Map.entry("ampitiya", "kandy-district"),
                Map.entry("habarana", "anuradhapura-district"),
                Map.entry("madulla", "moneragala-district"),
                Map.entry("kayts", "jaffna-district"),
                Map.entry("thirukkovil", "ampara-district"));

        expectedParentSlug.forEach((citySlug, districtSlug) -> {
            Location city = locations.findBySlugAndActiveTrue(citySlug)
                    .orElseThrow(() -> new AssertionError("Missing city: " + citySlug));
            assertEquals(LocationType.CITY, city.getType());
            assertNotNull(city.getParent(), "City has no parent: " + citySlug);
            assertEquals(districtSlug, city.getParent().getSlug());
        });
    }

    @Test
    void noDuplicateNormalizedNameUnderSameParent() {
        List<Location> all = locations.findAllByActiveTrueOrderByNameAsc();

        Map<String, Long> counts = all.stream()
                .filter(l -> l.getParent() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getParent().getId() + "::" + l.getName().trim().toLowerCase(Locale.ROOT),
                        Collectors.counting()));

        List<String> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        assertTrue(duplicates.isEmpty(), "Duplicate (parent, name) entries found: " + duplicates);
    }

    @Test
    void noOnlineLocationWasAdded() {
        boolean hasOnlineLocation = locations.findAllByActiveTrueOrderByNameAsc().stream()
                .anyMatch(l -> l.getName().trim().equalsIgnoreCase("Online")
                        || l.getSlug().trim().equalsIgnoreCase("online"));

        assertFalse(hasOnlineLocation, "ONLINE must remain a delivery mode, not a location");
    }

    @Test
    void representativePlacesResolveToCorrectDistrict() {
        assertParentDistrict("nugegoda", "colombo-district");
        assertParentDistrict("kottawa", "colombo-district");
        assertParentDistrict("negombo", "gampaha-district");
        assertParentDistrict("gampola", "kandy-district");
        assertParentDistrict("katugastota", "kandy-district");
    }

    private void assertParentDistrict(String citySlug, String expectedDistrictSlug) {
        Location city = locations.findBySlugAndActiveTrue(citySlug)
                .orElseThrow(() -> new AssertionError("Missing city: " + citySlug));
        assertEquals(expectedDistrictSlug, city.getParent().getSlug());
    }

    /**
     * The panthi.lk reference lists Kataragama under Moneragala, but V2 (already applied) seeded
     * it under Hambantota. V15 deliberately does not touch it or add a second 'kataragama' -
     * this test documents that the original placement is untouched and no duplicate was created.
     */
    @Test
    void kataragamaHierarchyMismatchIsDocumentedNotDuplicated() {
        assertParentDistrict("kataragama", "hambantota-district");

        long kataragamaCount = locations.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(l -> l.getName().trim().equalsIgnoreCase("Kataragama"))
                .count();
        assertEquals(1, kataragamaCount, "Kataragama must exist exactly once, not duplicated under Moneragala");
    }

    @Test
    void locationApiReturnsNewlyAddedCities() throws Exception {
        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("Nawala"), "API response missing Nawala");
                    assertTrue(body.contains("Katana"), "API response missing Katana");
                    assertTrue(body.contains("Kotahena"), "API response missing Kotahena");
                });
    }

    @Test
    void existingLocationsRemainIntact() {
        assertTrue(locations.findBySlugAndActiveTrue("colombo").isPresent());
        assertTrue(locations.findBySlugAndActiveTrue("jaffna").isPresent());
        assertTrue(locations.findBySlugAndActiveTrue("moneragala").isPresent());
        // 9 provinces + 26 district rows + 258 original cities (V2) + 13 (V14) + 84 (V15)
        assertTrue(locations.findAllByActiveTrueOrderByNameAsc().size() >= 390);
    }
}
