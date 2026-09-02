package com.slmanju.ceylonads.seed;

import com.slmanju.ceylonads.seed.SampleDataSeeder.SeedResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Local/dev-only sample-data endpoints.
 *
 * Enable with:
 *   ceylonads.seed.enabled=true
 *
 * Keep this disabled in production.
 */
@RestController
@RequestMapping("/api/dev/seed")
public class SampleDataSeedController {

    private final SampleDataSeeder seeder;

    public SampleDataSeedController(SampleDataSeeder seeder) {
        this.seeder = seeder;
    }

    @GetMapping("/people")
    public ResponseEntity<SeedResult> seedPeople() {
        return ResponseEntity.ok(seeder.seedPeople());
    }

    @GetMapping("/cars")
    public ResponseEntity<SeedResult> seedCars() {
        return ResponseEntity.ok(seeder.seedCarAds());
    }

    @GetMapping("/motorcycles")
    public ResponseEntity<SeedResult> seedMotorcycles() {
        return ResponseEntity.ok(seeder.seedMotorcycleAds());
    }

    @GetMapping("/property")
    public ResponseEntity<SeedResult> seedProperty() {
        return ResponseEntity.ok(seeder.seedPropertyAds());
    }

    @GetMapping("/mobiles")
    public ResponseEntity<SeedResult> seedMobiles() {
        return ResponseEntity.ok(seeder.seedMobileAds());
    }

    @GetMapping("/tuition")
    public ResponseEntity<SeedResult> seedTuition() {
        return ResponseEntity.ok(seeder.seedTuitionAds());
    }

    @GetMapping("/services")
    public ResponseEntity<SeedResult> seedServices() {
        return ResponseEntity.ok(seeder.seedServiceAds());
    }

    @GetMapping("/promotions")
    public ResponseEntity<SeedResult> seedPromotions() {
        return ResponseEntity.ok(seeder.seedPromotions());
    }

    @GetMapping("/promotion-campaign")
    public ResponseEntity<SeedResult> activateLaunchCampaign() {
        return ResponseEntity.ok(seeder.activateDevLaunchCampaign());
    }

}
