package com.slmanju.ceylonads.seed;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdAttributeValueRepository;
import com.slmanju.ceylonads.ad.repository.AdLocationRepository;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.ad.service.AdAttributeService;
import com.slmanju.ceylonads.ad.service.AdLocationService;
import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.entity.Role;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import com.slmanju.ceylonads.media.dto.StoredMedia;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LOCAL/DEV sample data only.
 *
 * Stable reference/master data (categories, locations, attribute definitions/options,
 * promotion slots and promotion plans) is owned by SQL and MUST NOT be created here.
 */
@Component
public class SampleDataSeeder {

    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final CategoryRepository categories;
    private final AdRepository ads;
    private final MediaRepository media;
    private final MediaStorage storage;
    private final PasswordEncoder passwordEncoder;
    private final PromotionPlanRepository promotionPlans;
    private final PromotionRepository promotions;
    private final PromotionCampaignRepository promotionCampaigns;
    private final AdAttributeService adAttributeService;
    private final AdLocationService adLocationService;
    private final AdAttributeValueRepository adAttributeValues;
    private final AdLocationRepository adLocations;

    private static final Map<String, Integer> IMAGE_POOL_SIZE = Map.of(
            "cars", 19,
            "motorcycles", 6,
            "property", 17,
            "phones", 10,
            "education", 9,
            "services", 15);

    private final Map<String, Integer> imageCursor = new HashMap<>();

    public SampleDataSeeder(
            AccountRepository accounts,
            CustomerRepository customers,
            CategoryRepository categories,
            AdRepository ads,
            MediaRepository media,
            MediaStorage storage,
            PasswordEncoder passwordEncoder,
            PromotionPlanRepository promotionPlans,
            PromotionRepository promotions,
            PromotionCampaignRepository promotionCampaigns,
            AdAttributeService adAttributeService,
            AdLocationService adLocationService,
            AdAttributeValueRepository adAttributeValues,
            AdLocationRepository adLocations) {
        this.accounts = accounts;
        this.customers = customers;
        this.categories = categories;
        this.ads = ads;
        this.media = media;
        this.storage = storage;
        this.passwordEncoder = passwordEncoder;
        this.promotionPlans = promotionPlans;
        this.promotions = promotions;
        this.promotionCampaigns = promotionCampaigns;
        this.adAttributeService = adAttributeService;
        this.adLocationService = adLocationService;
        this.adAttributeValues = adAttributeValues;
        this.adLocations = adLocations;
    }

    public record SeedResult(String group, int created, int skipped, String message) {
    }

    @Transactional
    public SeedResult seedPeople() {
        // Checked by a specific sample username rather than customers.count() > 0: Flyway's
        // operational MODERATOR account (chaminda) also has a Customer profile, so a fresh
        // Flyway-initialized database already has one Customer row before any sample data runs.
        if (accounts.existsByUsernameIgnoreCase("kamal")) {
            return new SeedResult("people", 0, (int) customers.count(),
                    "Sample customers already exist; people seed skipped.");
        }

        record Person(String username, String displayName, String phone, String email) {}
        List<Person> people = List.of(
                new Person("kamal", "Kamal Perera", "0771234567", "kamal@example.com"),
                new Person("nimal", "Nimal Silva", "0715558899", "nimal@example.com"),
                new Person("sunil", "Sunil Fernando", "0772223344", "sunil.fernando@example.com"),
                new Person("chamari", "Chamari Jayasuriya", "0763334455", "chamari.jayasuriya@example.com"),
                new Person("dilani", "Dilani Rathnayake", "0754445566", "dilani.rathnayake@example.com"),
                new Person("ruwan", "Ruwan Gunawardena", "0715556677", "ruwan.gunawardena@example.com"),
                new Person("priyanka", "Priyanka Wickramasinghe", "0776667788", "priyanka.wickrama@example.com"),
                new Person("ashan", "Ashan Mendis", "0727778899", "ashan.mendis@example.com"),
                new Person("malithi", "Malithi Karunaratne", "0718889900", "malithi.karu@example.com"),
                new Person("nadeesha", "Nadeesha Abeysekera", "0759990011", "nadeesha.abey@example.com"),
                new Person("roshan", "Roshan Bandara", "0771112233", "roshan.bandara@example.com"),
                new Person("kumari", "Kumari Dissanayake", "0762223345", "kumari.dissa@example.com"),
                new Person("thilina", "Thilina Wijesinghe", "0713334456", "thilina.wije@example.com"),
                new Person("anushka", "Anushka Senanayake", "0774445567", "anushka.sena@example.com"),
                new Person("sanjeewa", "Sanjeewa Ekanayake", "0755556678", "sanjeewa.eka@example.com"),
                new Person("hasini", "Hasini Peiris", "0716667789", "hasini.peiris@example.com"));

        int created = 0;
        for (Person p : people) {
            Account account = accounts.save(new Account(
                    p.username(), passwordEncoder.encode("customer123"), p.email(), Role.CUSTOMER));
            customers.save(new Customer(account, p.displayName(), p.phone()));
            created++;
        }

        if (!accounts.existsByUsernameIgnoreCase("moderator1")) {
            Account moderator = accounts.save(new Account(
                    "moderator1", passwordEncoder.encode("moderator123"), "moderator1@ceylonads.local", Role.MODERATOR));
            customers.save(new Customer(moderator, "CeylonAds Moderator", "0770000000"));
            created++;
        }

        return new SeedResult("people", created, 0, "Sample customer and moderator accounts created.");
    }

    @Transactional
    public SeedResult seedCarAds() {
        return seedAds("cars", carAdSeeds());
    }

    @Transactional
    public SeedResult seedMotorcycleAds() {
        return seedAds("motorcycles", motorcycleAdSeeds());
    }

    @Transactional
    public SeedResult seedPropertyAds() {
        return seedAds("property", propertyAdSeeds());
    }

    @Transactional
    public SeedResult seedMobileAds() {
        return seedAds("mobiles", mobileAdSeeds());
    }

    @Transactional
    public SeedResult seedTuitionAds() {
        List<AdSeed> seeds = tuitionAdSeeds();
        cleanupStaleTuitionAds(seeds.stream().map(AdSeed::title).collect(Collectors.toSet()));
        return seedAds("tuition", seeds);
    }

    // Reconciles the TUITION-channel ad set down to exactly the curated list above: anything with
    // sourceChannel=TUITION whose title isn't in the current list is stale (e.g. left over from a
    // previously-removed ~1,000-ad performance dataset, or a superseded curated title) and is
    // deleted here, along with everything that references it, before seedAds() (re-)creates the
    // current list. This keeps repeated seed runs duplicate-safe and keeps the Tuition sample
    // count stable, regardless of what a database accumulated from an older seeding mechanism.
    private void cleanupStaleTuitionAds(Set<String> currentTitles) {
        List<Ad> stale = ads.findBySourceChannel(SourceChannel.TUITION).stream()
                .filter(ad -> !currentTitles.contains(ad.getTitle()))
                .toList();
        if (stale.isEmpty()) {
            return;
        }

        List<Long> staleIds = stale.stream().map(Ad::getId).toList();

        List<Media> staleMedia = media.findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc(staleIds);
        for (Media m : staleMedia) {
            try {
                storage.delete(m.getStorageKey());
            } catch (Exception e) {
                System.err.println("Failed to delete stale tuition media file '" + m.getStorageKey() + "': " + e.getMessage());
            }
        }
        media.deleteAll(staleMedia);

        promotions.deleteAll(promotions.findByAdIdIn(staleIds));

        for (Long adId : staleIds) {
            adAttributeValues.deleteByAdId(adId);
            adLocations.deleteByAdId(adId);
        }

        ads.deleteAll(stale);
    }

    @Transactional
    public SeedResult seedServiceAds() {
        return seedAds("services", serviceAdSeeds());
    }

    @Transactional
    public SeedResult seedPromotions() {
        for (String code : List.of(
                "HOME_FEATURED_7D",
                "HOME_BANNER_7D",
                "TOP_SEARCH_7D",
                "PROPERTY_FEATURED_7D",
                "VEHICLES_FEATURED_7D",
                "TUITION_HOME_FEATURED_30D")) {
            requireMasterData("promotion plan", code, promotionPlans.findByCode(code).isPresent());
        }

        if (promotions.count() > 0) {
            return new SeedResult("promotions", 0, (int) promotions.count(),
                    "Promotions already exist; promotion seed skipped.");
        }

        Map<String, Ad> adsByTitle = ads.findAll().stream()
                .collect(Collectors.toMap(Ad::getTitle, ad -> ad, (a, b) -> a));

        Customer bannerAdvertiser = findCustomer("hasini");
        int before = (int) promotions.count();
        seedSamplePromotionsIfMissing(adsByTitle, bannerAdvertiser);
        int after = (int) promotions.count();
        return new SeedResult("promotions", after - before, 0, "Sample promotions created.");
    }

    // DEV-only: makes the ezClass launch campaign (EZCLASS_LAUNCH_990, master data from
    // V18/V20__*.sql) immediately live and testable via GET /api/tuition/promotions/campaign,
    // without ever touching production Flyway migrations. The campaign row itself - pricing, plan
    // mappings, presentation copy (headline/message/ctaLabel), customer_visible/show_banner/
    // show_modal - already exists from master data; this only re-windows its [starts_at, ends_at)
    // to cover "now" and flips active=true, since a fixed-at-migration-time window goes stale the
    // longer a dev database has existed. Idempotent and safe to call on every startup: it always
    // recomputes the window relative to the current instant rather than accumulating state, and
    // touches no other campaign fields (pricing/name/plans stay exactly as master data defined
    // them). EZCLASS_HALF_PRICE is explicitly deactivated in the same pass so DEV never ends up
    // with two overlapping active storefront campaigns for TUITION - see
    // PromotionCampaignService#requireNoOverlappingStorefrontCampaign, which this seeder
    // deliberately bypasses (see PromotionCampaign#activateForDevStorefront) and must therefore
    // preserve by hand.
    @Transactional
    public SeedResult activateDevLaunchCampaign() {
        PromotionCampaign launch = promotionCampaigns.findByCode("EZCLASS_LAUNCH_990").orElse(null);
        requireMasterData("promotion campaign", "EZCLASS_LAUNCH_990", launch != null);

        Instant now = Instant.now();
        launch.activateForDevStorefront(now.minus(Duration.ofHours(1)), now.plus(Duration.ofDays(90)));

        promotionCampaigns.findByCode("EZCLASS_HALF_PRICE").ifPresent(halfPrice -> halfPrice.setActive(false));

        return new SeedResult("promotion-campaign", 0, 0,
                "EZCLASS_LAUNCH_990 activated for DEV (active=true, window now-1h .. now+90d); "
                        + "EZCLASS_HALF_PRICE deactivated to avoid overlap.");
    }

    private SeedResult seedAds(String group, List<AdSeed> seeds) {
        requireCustomers();
        Map<String, Ad> existingByTitle = ads.findAll().stream()
                .collect(Collectors.toMap(Ad::getTitle, a -> a, (a, b) -> a));

        int created = 0;
        int skipped = 0;
        Instant now = Instant.now();
        int i = 0;

        for (AdSeed seed : seeds) {
            Ad existing = existingByTitle.get(seed.title());
            if (existing != null) {
                skipped++;
                if ("tuition".equals(group)) {
                    // A database seeded before source_channel existed would otherwise keep this ad
                    // at its MAIN_SITE default forever, since nothing else ever revisits existing rows.
                    if (existing.getSourceChannel() != SourceChannel.TUITION) {
                        existing.assignSourceChannel(SourceChannel.TUITION);
                        ads.save(existing);
                    }
                    // Reassociate media even for an already-existing curated ad: this is how a
                    // poster-art revision (or a DB that already has these ads from a previous
                    // seed run) gets the current image set without recreating the ad record.
                    reconcileTuitionMedia(existing, tuitionAdIndex(seed.title()));
                }
                continue;
            }

            Category category = requireCategory(resolveCategorySlug(seed));
            Customer seller = findCustomer(seed.sellerKey());

            Ad ad = new Ad(
                    seed.title(),
                    seed.description(),
                    new BigDecimal(seed.price()),
                    category,
                    seller);
            // "tuition" is the only sample group that belongs to the Tuition storefront/vertical;
            // every other group (cars, motorcycles, property, mobiles, services) stays MAIN_SITE,
            // the entity default.
            if ("tuition".equals(group)) {
                ad.assignSourceChannel(SourceChannel.TUITION);
            }

            switch (seed.status()) {
                case ACTIVE -> ad.approve(null);
                case REJECTED -> ad.reject(null);
                case DEACTIVATED -> {
                    ad.approve(null);
                    ad.deactivate();
                }
                default -> {
                    // Leave constructor default (normally PENDING_REVIEW).
                }
            }

            Instant createdAt = now.minus(Duration.ofDays(seed.daysAgo()))
                    .minus(Duration.ofHours((i * 7L) % 24))
                    .minus(Duration.ofMinutes((i * 13L) % 60));
            ad.backdateCreatedAt(createdAt);

            ad = ads.save(ad);

            Map<String, String> attributes = attributesFor(seed, category.getSlug());
            if (!attributes.isEmpty()) {
                adAttributeService.replaceValues(ad, attributes);
            }
            List<String> locationSlugs = seed.locationKey() == null ? List.of() : List.of(seed.locationKey());
            adLocationService.replaceLocations(ad, locationSlugs, attributes);

            if ("tuition".equals(group)) {
                reconcileTuitionMedia(ad, tuitionAdIndex(seed.title()));
            } else {
                try {
                    attachImages(ad, seed.imageGroup(), i);
                } catch (Exception e) {
                    // Sample media failure should not prevent the rest of the sample data from loading.
                    System.err.println("Failed to attach sample images for '" + seed.title() + "': " + e.getMessage());
                }
            }

            existingByTitle.put(seed.title(), ad);
            created++;
            i++;
        }

        return new SeedResult(group, created, skipped,
                "Sample " + group + " ads processed.");
    }

    private String resolveCategorySlug(AdSeed seed) {
        return switch (seed.categoryKey()) {
            case "cars" -> "cars";
            case "motorcycles" -> "motorcycles";
            case "mobiles" -> "mobile-phones";
            case "tuition" -> "school-tuition";
            // The Tuition vertical spans several leaf categories under Education & Tuition, not
            // just School Tuition (see V9__education_tuition_category_restructure.sql) - these
            // slugs are used directly as both the seed's categoryKey and the real category slug.
            case "language-classes", "music", "dancing", "technology-coding", "other-education-tuition" ->
                    seed.categoryKey();
            case "houses" -> resolvePropertyCategory(seed);
            case "services" -> resolveServiceCategory(seed);
            default -> throw new IllegalStateException("Unknown sample category key: " + seed.categoryKey());
        };
    }

    private String resolvePropertyCategory(AdSeed seed) {
        String type = seed.attrs().get("propertyType");
        boolean rent = seed.title().toLowerCase().contains("rent");

        return switch (type) {
            case "HOUSE" -> rent ? "houses-for-rent" : "houses-for-sale";
            case "APARTMENT" -> rent ? "apartments-for-rent" : "apartments-for-sale";
            case "LAND" -> "land-for-sale";
            case "COMMERCIAL" -> "commercial-property";
            case "ROOM" -> "rooms-annexes";
            default -> throw new IllegalStateException(
                    "Unknown propertyType for sample ad '" + seed.title() + "': " + type);
        };
    }

    private String resolveServiceCategory(AdSeed seed) {
        String type = seed.attrs().getOrDefault("serviceType", "").toLowerCase();

        if (type.contains("clean")) {
            return "cleaning-services";
        }
        if (type.contains("computer") || type.contains("mobile phone") || type.contains("cctv")) {
            return "it-tech-services";
        }
        if (type.contains("moving")) {
            return "moving-delivery-services";
        }
        if (type.contains("electrical") || type.contains("plumb")
                || type.contains("painting") || type.contains("ac ")) {
            return "home-repair-services";
        }
        return "other-services";
    }

    private Map<String, String> attributesFor(AdSeed seed, String categorySlug) {
        Map<String, String> attrs = new LinkedHashMap<>(seed.attrs());

        // Property type is now represented by the leaf category itself.
        attrs.remove("propertyType");

        // New property leaves intentionally contain only relevant definitions.
        if ("land-for-sale".equals(categorySlug)) {
            attrs.keySet().retainAll(Set.of("landSize"));
        } else if ("commercial-property".equals(categorySlug)) {
            attrs.keySet().retainAll(Set.of("bathrooms", "floorArea"));
        }

        return attrs;
    }

    private Category requireCategory(String slug) {
        return categories.findBySlug(slug)
                .orElseThrow(() -> new IllegalStateException(
                        "Required master category is missing: " + slug
                                + ". Run the master-data SQL first."));
    }


    private Customer findCustomer(String username) {
        return customers.findAll().stream()
                .filter(customer -> username.equals(customer.getAccount().getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Sample customer '" + username + "' is missing. Seed people first."));
    }

    private void requireCustomers() {
        if (customers.count() == 0) {
            throw new IllegalStateException("No sample customers found. Seed people first.");
        }
    }

    private void requireMasterData(String type, String key, boolean present) {
        if (!present) {
            throw new IllegalStateException(
                    "Required master " + type + " is missing: " + key
                            + ". Run the master-data SQL first.");
        }
    }

    // --- Ad seed data -----------------------------------------------------------------------

    private record AdSeed(
            String title, String description, String price, String categoryKey, String locationKey,
            String sellerKey, Map<String, String> attrs, String imageGroup, AdStatus status, int daysAgo) {
    }

    private AdSeed ad(String title, String description, String price, String categoryKey, String locationKey,
            String sellerKey, Map<String, String> attrs, String imageGroup, AdStatus status, int daysAgo) {
        return new AdSeed(title, description, price, categoryKey, locationKey, sellerKey, attrs, imageGroup, status, daysAgo);
    }

    private static Map<String, String> kv(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private List<AdSeed> carAdSeeds() {
        return List.of(
                ad("Toyota Aqua 2019 - Hybrid", "Well maintained Toyota Aqua, automatic, hybrid, 72,000 km. Inspection welcome.",
                        "8950000", "cars", "maharagama", "kamal",
                        kv("make", "Toyota", "model", "Aqua", "year", "2019", "mileage", "72000", "fuelType", "HYBRID", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 2),
                ad("Toyota Prius 2017 - Full Option", "Genuine mileage, leather seats, reverse camera. Single owner.",
                        "9750000", "cars", "colombo", "nimal",
                        kv("make", "Toyota", "model", "Prius", "year", "2017", "mileage", "88000", "fuelType", "HYBRID", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 5),
                ad("Toyota Vitz 2018 - Excellent Condition", "Fuel efficient, well serviced, alloy wheels, new tyres.",
                        "6450000", "cars", "nugegoda", "sunil",
                        kv("make", "Toyota", "model", "Vitz", "year", "2018", "mileage", "65000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 1),
                ad("Suzuki Wagon R 2020", "Low mileage family car, first owner, full service history available.",
                        "5250000", "cars", "kottawa", "chamari",
                        kv("make", "Suzuki", "model", "Wagon R", "year", "2020", "mileage", "34000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 0),
                ad("Honda Fit GP5 2016 - Hybrid", "Imported brand new, hybrid battery in good health, well maintained.",
                        "6100000", "cars", "dehiwala", "dilani",
                        kv("make", "Honda", "model", "Fit", "year", "2016", "mileage", "95000", "fuelType", "HYBRID", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 10),
                ad("Honda Vezel 2018 - Hybrid RS", "RS grade, paddle shift, full leather interior, accident free.",
                        "11900000", "cars", "moratuwa", "ruwan",
                        kv("make", "Honda", "model", "Vezel", "year", "2018", "mileage", "58000", "fuelType", "HYBRID", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 3),
                ad("Nissan X-Trail 2017 - 7 Seater", "Spacious SUV, 4WD, sunroof, ideal for family trips.",
                        "13500000", "cars", "kandy", "priyanka",
                        kv("make", "Nissan", "model", "X-Trail", "year", "2017", "mileage", "72000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 14),
                ad("Toyota Axio 2015 - Hybrid", "Well kept sedan, new battery, service records available.",
                        "7200000", "cars", "gampaha", "ashan",
                        kv("make", "Toyota", "model", "Axio", "year", "2015", "mileage", "102000", "fuelType", "HYBRID", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 7),
                ad("Suzuki Alto 2021 - Like New", "Economical city car, low mileage, still under warranty.",
                        "4650000", "cars", "negombo", "malithi",
                        kv("make", "Suzuki", "model", "Alto", "year", "2021", "mileage", "18000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 0),
                ad("Toyota Premio 2014 - G Superior", "Well maintained, cool box, premium sound system.",
                        "8100000", "cars", "kurunegala", "nadeesha",
                        kv("make", "Toyota", "model", "Premio", "year", "2014", "mileage", "115000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 20),
                ad("Toyota Hiace 2013 - Dolphin Van", "15 seater, well maintained, ideal for school van or tours.",
                        "9800000", "cars", "colombo", "roshan",
                        kv("make", "Toyota", "model", "Hiace", "year", "2013", "mileage", "185000", "fuelType", "DIESEL", "transmission", "MANUAL", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 4),
                ad("Suzuki Every 2015 - Mini Van", "Good running condition, ideal for small business deliveries.",
                        "3950000", "cars", "kalutara", "kumari",
                        kv("make", "Suzuki", "model", "Every", "year", "2015", "mileage", "92000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 9),
                ad("Toyota Aqua 2016 - S Grade", "Fuel efficient hybrid, reverse camera, alloy wheels fitted.",
                        "7450000", "cars", "galle", "thilina",
                        kv("make", "Toyota", "model", "Aqua", "year", "2016", "mileage", "98000", "fuelType", "HYBRID", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 16),
                ad("Honda Fit GP1 2013", "Reliable hatchback, recently serviced, new battery fitted.",
                        "5300000", "cars", "matara", "anushka",
                        kv("make", "Honda", "model", "Fit", "year", "2013", "mileage", "128000", "fuelType", "HYBRID", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 22),
                ad("Toyota Vitz 2014 - KSP130", "Well maintained, good tyres, non accidental.",
                        "5750000", "cars", "jaffna", "sanjeewa",
                        kv("make", "Toyota", "model", "Vitz", "year", "2014", "mileage", "110000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 28),
                ad("Suzuki Wagon R Stingray 2019", "Special edition, LED headlamps, low mileage, one owner.",
                        "5900000", "cars", "ratnapura", "hasini",
                        kv("make", "Suzuki", "model", "Wagon R Stingray", "year", "2019", "mileage", "41000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 6),
                ad("Toyota Corolla Axio Hybrid 2016", "Well kept, comfortable ride, ideal for family use.",
                        "8700000", "cars", "nugegoda", "kamal",
                        kv("make", "Toyota", "model", "Axio", "year", "2016", "mileage", "89000", "fuelType", "HYBRID", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.ACTIVE, 11),
                ad("Nissan Leaf 2018 - Electric", "Zero emission, low running cost, good battery capacity remaining.",
                        "7900000", "cars", "colombo", "nimal",
                        kv("make", "Nissan", "model", "Leaf", "year", "2018", "mileage", "62000", "fuelType", "ELECTRIC", "transmission", "AUTOMATIC", "condition", "USED"),
                        "cars", AdStatus.PENDING_REVIEW, 0),
                ad("Toyota Premio 2011 - CVT", "Selling as is, minor scratches, engine and gearbox in good condition.",
                        "6300000", "cars", "maharagama", "sunil",
                        kv("make", "Toyota", "model", "Premio", "year", "2011", "mileage", "168000", "fuelType", "PETROL", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.REJECTED, 2),
                ad("Honda Vezel 2015 - Sold", "No longer available, listing kept for reference.",
                        "9600000", "cars", "dehiwala", "chamari",
                        kv("make", "Honda", "model", "Vezel", "year", "2015", "mileage", "101000", "fuelType", "HYBRID", "transmission", "CVT", "condition", "USED"),
                        "cars", AdStatus.DEACTIVATED, 18));
    }

    private List<AdSeed> motorcycleAdSeeds() {
        return List.of(
                ad("Bajaj Pulsar 150 2019", "Single owner, well maintained, good tyres, all documents clear.",
                        "375000", "motorcycles", "nugegoda", "roshan",
                        kv("make", "Bajaj", "model", "Pulsar 150", "year", "2019", "mileage", "24000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "motorcycles", AdStatus.ACTIVE, 3),
                ad("Honda Dio 2020 - Scooter", "Low mileage, ideal for daily commute, recently serviced.",
                        "285000", "motorcycles", "colombo", "kumari",
                        kv("make", "Honda", "model", "Dio", "year", "2020", "mileage", "12000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "motorcycles", AdStatus.ACTIVE, 1),
                ad("Yamaha FZ-S 2018", "Sporty look, powerful engine, well maintained, new chain sprocket set.",
                        "340000", "motorcycles", "kandy", "thilina",
                        kv("make", "Yamaha", "model", "FZ-S", "year", "2018", "mileage", "31000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "motorcycles", AdStatus.ACTIVE, 0),
                ad("TVS Apache RTR 160 2021", "Excellent condition, digital meter, disc brakes, single owner.",
                        "425000", "motorcycles", "kottawa", "anushka",
                        kv("make", "TVS", "model", "Apache RTR 160", "year", "2021", "mileage", "15000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "motorcycles", AdStatus.ACTIVE, 8),
                ad("Hero Honda CD 100 2005", "Reliable old classic, economical, good for daily use.",
                        "145000", "motorcycles", "kurunegala", "sanjeewa",
                        kv("make", "Hero", "model", "CD 100", "year", "2005", "mileage", "78000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "motorcycles", AdStatus.ACTIVE, 25),
                ad("Bajaj Pulsar 220F 2017", "Full fairing, twin disc brakes, well maintained, no leaks.",
                        "355000", "motorcycles", "galle", "hasini",
                        kv("make", "Bajaj", "model", "Pulsar 220F", "year", "2017", "mileage", "42000", "fuelType", "PETROL", "transmission", "MANUAL", "condition", "USED"),
                        "motorcycles", AdStatus.ACTIVE, 13),
                ad("Honda Dio 2016 - Good Condition", "Daily used, minor wear and tear, priced to sell fast.",
                        "195000", "motorcycles", "matara", "kamal",
                        kv("make", "Honda", "model", "Dio", "year", "2016", "mileage", "56000", "fuelType", "PETROL", "transmission", "AUTOMATIC", "condition", "USED"),
                        "motorcycles", AdStatus.PENDING_REVIEW, 0));
    }

    private List<AdSeed> propertyAdSeeds() {
        return List.of(
                ad("Modern House for Sale in Nugegoda", "Four bedroom house with three bathrooms and parking in a quiet residential area.",
                        "32500000", "houses", "nugegoda", "nimal",
                        kv("propertyType", "HOUSE", "bedrooms", "4", "bathrooms", "3", "landSize", "10", "floorArea", "2400", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 2),
                ad("Two Storey House for Sale - Kottawa", "Well built house close to main road, quiet neighbourhood, easy access to Colombo.",
                        "24500000", "houses", "kottawa", "ruwan",
                        kv("propertyType", "HOUSE", "bedrooms", "3", "bathrooms", "2", "landSize", "8", "floorArea", "1800", "furnished", "false"),
                        "property", AdStatus.ACTIVE, 4),
                ad("Annex for Rent - Maharagama", "Fully furnished annex with separate entrance, ideal for a small family.",
                        "28000", "houses", "maharagama", "priyanka",
                        kv("propertyType", "ROOM", "bedrooms", "1", "bathrooms", "1", "floorArea", "450", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 0),
                ad("Apartment for Sale - Colombo 5", "Spacious apartment with city views, secured parking, and 24-hour security.",
                        "45000000", "houses", "colombo", "ashan",
                        kv("propertyType", "APARTMENT", "bedrooms", "3", "bathrooms", "2", "floorArea", "1450", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 2),
                ad("Apartment for Rent - Dehiwala", "Fully furnished two bedroom apartment near the beach road.",
                        "65000", "houses", "dehiwala", "malithi",
                        kv("propertyType", "APARTMENT", "bedrooms", "2", "bathrooms", "1", "floorArea", "950", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 0),
                ad("Land for Sale - Kalutara", "Prime residential land, clear deeds, close to main town.",
                        "6000000", "houses", "kalutara", "nadeesha",
                        kv("propertyType", "LAND", "bedrooms", "0", "bathrooms", "0", "landSize", "20"),
                        "property", AdStatus.ACTIVE, 9),
                ad("Land for Sale - Kurunegala", "Flat land, road access, suitable for house construction.",
                        "9500000", "houses", "kurunegala", "kumari",
                        kv("propertyType", "LAND", "bedrooms", "0", "bathrooms", "0", "landSize", "40"),
                        "property", AdStatus.ACTIVE, 15),
                ad("Commercial Property for Rent - Colombo", "Ground floor commercial space suitable for retail or office use.",
                        "185000", "houses", "colombo", "thilina",
                        kv("propertyType", "COMMERCIAL", "bedrooms", "0", "bathrooms", "2", "floorArea", "3200"),
                        "property", AdStatus.ACTIVE, 6),
                ad("Commercial Shop for Sale - Kandy", "Well located shop space on a busy street, high foot traffic.",
                        "22000000", "houses", "kandy", "sanjeewa",
                        kv("propertyType", "COMMERCIAL", "bedrooms", "0", "bathrooms", "1", "floorArea", "850"),
                        "property", AdStatus.ACTIVE, 19),
                ad("House for Sale - Moratuwa", "Large family house with garden, quiet street, close to schools.",
                        "55000000", "houses", "moratuwa", "hasini",
                        kv("propertyType", "HOUSE", "bedrooms", "5", "bathrooms", "4", "landSize", "15", "floorArea", "3200", "furnished", "false"),
                        "property", AdStatus.ACTIVE, 3),
                ad("House for Rent - Nugegoda", "Three bedroom house, fully furnished, close to main town.",
                        "95000", "houses", "nugegoda", "kamal",
                        kv("propertyType", "HOUSE", "bedrooms", "3", "bathrooms", "2", "floorArea", "1600", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 1),
                ad("Annex for Rent - Kottawa", "Quiet annex with private entrance, water and electricity included.",
                        "20000", "houses", "kottawa", "nimal",
                        kv("propertyType", "ROOM", "bedrooms", "1", "bathrooms", "1", "floorArea", "400", "furnished", "false"),
                        "property", AdStatus.ACTIVE, 12),
                ad("House for Sale - Galle", "Traditional house with large garden, close to Galle Fort.",
                        "38000000", "houses", "galle", "sunil",
                        kv("propertyType", "HOUSE", "bedrooms", "4", "bathrooms", "3", "landSize", "12", "floorArea", "2600", "furnished", "false"),
                        "property", AdStatus.ACTIVE, 21),
                ad("Apartment for Sale - Colombo 6", "Modern two bedroom apartment with balcony and gym access.",
                        "32000000", "houses", "colombo", "chamari",
                        kv("propertyType", "APARTMENT", "bedrooms", "2", "bathrooms", "2", "floorArea", "1100", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 5),
                ad("House for Sale - Matara", "Comfortable family home close to the town centre and schools.",
                        "21500000", "houses", "matara", "dilani",
                        kv("propertyType", "HOUSE", "bedrooms", "3", "bathrooms", "2", "landSize", "9", "floorArea", "1750", "furnished", "false"),
                        "property", AdStatus.ACTIVE, 26),
                ad("Land for Sale - Ratnapura", "Scenic land with good road frontage, ideal for a holiday home.",
                        "12000000", "houses", "ratnapura", "ruwan",
                        kv("propertyType", "LAND", "bedrooms", "0", "bathrooms", "0", "landSize", "100"),
                        "property", AdStatus.ACTIVE, 17),
                ad("House for Rent - Kandy", "Spacious furnished house with mountain views, quiet residential area.",
                        "110000", "houses", "kandy", "priyanka",
                        kv("propertyType", "HOUSE", "bedrooms", "4", "bathrooms", "3", "floorArea", "2200", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 0),
                ad("Apartment for Rent - Colombo 3", "Compact one bedroom apartment, fully furnished, close to office areas.",
                        "55000", "houses", "colombo", "ashan",
                        kv("propertyType", "APARTMENT", "bedrooms", "1", "bathrooms", "1", "floorArea", "650", "furnished", "true"),
                        "property", AdStatus.ACTIVE, 7),
                ad("Commercial Building for Sale - Negombo", "Multi floor commercial building, suitable for offices or showroom.",
                        "65000000", "houses", "negombo", "malithi",
                        kv("propertyType", "COMMERCIAL", "bedrooms", "0", "bathrooms", "3", "floorArea", "4500"),
                        "property", AdStatus.ACTIVE, 23),
                ad("House for Sale - Jaffna", "Well maintained house in a peaceful neighbourhood, near main road.",
                        "19000000", "houses", "jaffna", "nadeesha",
                        kv("propertyType", "HOUSE", "bedrooms", "3", "bathrooms", "2", "landSize", "11", "floorArea", "1900", "furnished", "false"),
                        "property", AdStatus.ACTIVE, 29),
                ad("Apartment for Sale - Nugegoda", "Bright and airy apartment close to shopping and transport.",
                        "38500000", "houses", "nugegoda", "kumari",
                        kv("propertyType", "APARTMENT", "bedrooms", "3", "bathrooms", "2", "floorArea", "1350", "furnished", "false"),
                        "property", AdStatus.PENDING_REVIEW, 0),
                ad("House for Sale - Maharagama", "Large house with extra land, needs minor renovation.",
                        "41000000", "houses", "maharagama", "thilina",
                        kv("propertyType", "HOUSE", "bedrooms", "4", "bathrooms", "3", "landSize", "13", "floorArea", "2500", "furnished", "true"),
                        "property", AdStatus.REJECTED, 3),
                ad("Annex for Rent - Dehiwala", "Already rented out, keeping listing for records.",
                        "18000", "houses", "dehiwala", "sanjeewa",
                        kv("propertyType", "ROOM", "bedrooms", "1", "bathrooms", "1", "floorArea", "380", "furnished", "true"),
                        "property", AdStatus.DEACTIVATED, 14));
    }

    private List<AdSeed> mobileAdSeeds() {
        return List.of(
                ad("iPhone 15 Pro 256GB", "Excellent condition. Box and original cable included.",
                        "245000", "mobiles", "colombo", "kamal",
                        kv("brand", "Apple", "model", "15 Pro", "condition", "USED", "storage", "256GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 1),
                ad("iPhone 13 128GB", "Battery health 90%, no scratches, comes with charger.",
                        "145000", "mobiles", "nugegoda", "nimal",
                        kv("brand", "Apple", "model", "13", "condition", "USED", "storage", "128GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 2),
                ad("iPhone 12 64GB", "Good condition, minor scratches on back, screen is flawless.",
                        "98000", "mobiles", "maharagama", "sunil",
                        kv("brand", "Apple", "model", "12", "condition", "USED", "storage", "64GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 6),
                ad("Samsung Galaxy S23 Ultra 256GB", "Comes with S Pen, screen protector fitted since day one.",
                        "215000", "mobiles", "kottawa", "chamari",
                        kv("brand", "Samsung", "model", "Galaxy S23 Ultra", "condition", "USED", "storage", "256GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 0),
                ad("Samsung Galaxy A54 128GB", "Brand new, sealed box, local agent warranty.",
                        "89000", "mobiles", "dehiwala", "dilani",
                        kv("brand", "Samsung", "model", "Galaxy A54", "condition", "NEW", "storage", "128GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 3),
                ad("Samsung Galaxy Note 20 256GB", "S Pen included, great for note taking, minor wear on frame.",
                        "95000", "mobiles", "moratuwa", "ruwan",
                        kv("brand", "Samsung", "model", "Galaxy Note 20", "condition", "USED", "storage", "256GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 10),
                ad("Xiaomi Redmi Note 12 128GB", "Brand new, unopened box, 1 year warranty from agent.",
                        "52000", "mobiles", "kandy", "priyanka",
                        kv("brand", "Xiaomi", "model", "Redmi Note 12", "condition", "NEW", "storage", "128GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 1),
                ad("Xiaomi Poco X5 Pro 256GB", "Great gaming phone, fast charger included, minor scratches.",
                        "68000", "mobiles", "gampaha", "ashan",
                        kv("brand", "Xiaomi", "model", "Poco X5 Pro", "condition", "USED", "storage", "256GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 8),
                ad("Xiaomi Mi 11 128GB", "Well maintained, always used with a case and screen guard.",
                        "61000", "mobiles", "negombo", "malithi",
                        kv("brand", "Xiaomi", "model", "Mi 11", "condition", "USED", "storage", "128GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 15),
                ad("OnePlus 11 5G 256GB", "Flagship performance, Hasselblad camera, still under agent warranty.",
                        "165000", "mobiles", "kurunegala", "nadeesha",
                        kv("brand", "OnePlus", "model", "11 5G", "condition", "USED", "storage", "256GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 4),
                ad("OnePlus Nord CE 3 128GB", "Sealed brand new, purchased but not needed.",
                        "72000", "mobiles", "colombo", "roshan",
                        kv("brand", "OnePlus", "model", "Nord CE 3", "condition", "NEW", "storage", "128GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 0),
                ad("Google Pixel 7a 128GB", "Clean Android experience, excellent camera, minor edge wear.",
                        "135000", "mobiles", "kalutara", "kumari",
                        kv("brand", "Google", "model", "Pixel 7a", "condition", "USED", "storage", "128GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 12),
                ad("Google Pixel 6 128GB", "Factory refurbished, comes with new battery and screen.",
                        "89000", "mobiles", "galle", "thilina",
                        kv("brand", "Google", "model", "Pixel 6", "condition", "REFURBISHED", "storage", "128GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 18),
                ad("Huawei P30 Pro 128GB", "Excellent camera phone, minor scratches on the frame.",
                        "58000", "mobiles", "matara", "anushka",
                        kv("brand", "Huawei", "model", "P30 Pro", "condition", "USED", "storage", "128GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 22),
                ad("iPhone 11 64GB", "Good battery health, screen and body in great condition.",
                        "88000", "mobiles", "jaffna", "sanjeewa",
                        kv("brand", "Apple", "model", "11", "condition", "USED", "storage", "64GB", "warranty", "false"),
                        "phones", AdStatus.ACTIVE, 27),
                ad("Samsung Galaxy Z Flip 5 256GB", "Barely used, foldable flagship, comes with original accessories.",
                        "235000", "mobiles", "ratnapura", "hasini",
                        kv("brand", "Samsung", "model", "Galaxy Z Flip 5", "condition", "NEW", "storage", "256GB", "warranty", "true"),
                        "phones", AdStatus.ACTIVE, 5),
                ad("iPhone 14 Pro Max 512GB", "Top of the range storage, excellent condition, all accessories included.",
                        "385000", "mobiles", "colombo", "kamal",
                        kv("brand", "Apple", "model", "14 Pro Max", "condition", "USED", "storage", "512GB", "warranty", "true"),
                        "phones", AdStatus.PENDING_REVIEW, 0),
                ad("Xiaomi Redmi 9A 64GB", "Budget friendly phone, some wear on the back cover.",
                        "22000", "mobiles", "nugegoda", "nimal",
                        kv("brand", "Xiaomi", "model", "Redmi 9A", "condition", "USED", "storage", "64GB", "warranty", "false"),
                        "phones", AdStatus.REJECTED, 9));
    }

    // Small, curated, human-understandable Tuition dataset (~20 ads) covering realistic Sri
    // Lankan tuition-market scenarios - replaces the old ~1,000-row TuitionPerformanceSeeder
    // dataset (removed; see db/testdata). subject/grade/curriculum/medium/classMode/classType
    // values are the canonical SELECT/MULTI_SELECT option codes from
    // V3__category_attribute_master_data.sql and V10__tuition_filter_master_data.sql - only
    // school-tuition carries grade/curriculum/medium/classType, so those keys are omitted
    // entirely (never sent as blank/empty) for the other Education & Tuition leaf categories.
    private List<AdSeed> tuitionAdSeeds() {
        return List.of(
                ad("A/L Combined Mathematics - Theory & Revision", "Structured theory classes followed by weekly revision papers for the current A/L batch. Past paper discussions and model paper marking included. Small groups for individual attention.",
                        "4500", "tuition", "colombo", "sunil",
                        kv("subject", "COMBINED_MATHEMATICS", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 1),
                ad("A/L Physics - One-on-One Online Classes", "One-on-one online classes covering the full A/L Physics syllabus. Flexible scheduling, recorded sessions on request, and regular problem-solving practice.",
                        // Online class mode: zero physical locations (see AdLocationService).
                        "5000", "tuition", null, "ashan",
                        kv("subject", "PHYSICS", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "ONLINE", "classType", "INDIVIDUAL"),
                        "tuition", AdStatus.ACTIVE, 3),
                ad("A/L Chemistry - Paper Class (Kandy)", "Focused paper-class sessions for students who have completed the theory syllabus. We work through past papers, model papers, and common examiner traps for Organic, Physical and Inorganic Chemistry. Held every Saturday in Kandy town with a small batch size so every student gets their scripts marked and discussed individually. Suitable for repeat A/L students and first-timers doing final revision.",
                        "4200", "tuition", "kandy", "dilani",
                        kv("subject", "CHEMISTRY", "grade", "AL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 10),
                ad("A/L Biology - Theory & Revision (Hybrid)", "Combined theory and revision programme for A/L Biology. Join physically in Gampaha or follow the same class live online. Weekly short-answer question practice.",
                        "4000", "tuition", "gampaha", "priyanka",
                        kv("subject", "BIOLOGY", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "BOTH", "classType", "GROUP"),
                        "tuition", AdStatus.PENDING_REVIEW, 0),
                ad("O/L Mathematics - English Medium Group Classes", "Small group O/L Mathematics classes in Nugegoda. Concept-based teaching with weekly homework and past paper practice.",
                        "3000", "tuition", "nugegoda", "nimal",
                        kv("subject", "MATHEMATICS", "grade", "OL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 2),
                ad("O/L Science - Sinhala Medium Batch", "O/L Science classes covering Biology, Chemistry and Physics units. Past paper discussions and monthly progress tests.",
                        "2500", "tuition", "maharagama", "kamal",
                        kv("subject", "SCIENCE", "grade", "OL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 4),
                ad("O/L English - Individual & Group Options", "O/L English language classes with a choice of individual or small group sessions. Focus on grammar, comprehension and essay writing.",
                        // No curriculum value: plain local O/L English, not tied to any syllabus track.
                        "2200", "tuition", "galle", "kumari",
                        kv("subject", "ENGLISH", "grade", "OL", "medium", "ENGLISH", "classMode", "BOTH", "classType", "BOTH"),
                        "tuition", AdStatus.ACTIVE, 8),
                ad("Cambridge IGCSE Mathematics - Group & Online Batch", "IGCSE Cambridge syllabus Mathematics classes with weekly past paper practice and topical worksheets.",
                        "5200", "tuition", "nugegoda", "malithi",
                        kv("subject", "MATHEMATICS", "grade", "IGCSE", "curriculum", "CAMBRIDGE", "medium", "ENGLISH", "classMode", "BOTH", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 11),
                ad("Pearson Edexcel English Literature - AS Level (Online)", "Online AS Level English Literature classes following the Pearson Edexcel syllabus. Set text analysis, essay technique and past paper feedback, one-on-one.",
                        "6000", "tuition", null, "chamari",
                        kv("subject", "ENGLISH_LITERATURE", "grade", "AS_LEVEL", "curriculum", "EDEXCEL", "medium", "ENGLISH", "classMode", "ONLINE", "classType", "INDIVIDUAL"),
                        "tuition", AdStatus.ACTIVE, 6),
                ad("Primary English & Mathematics - Grade 3 to 5", "Fun, interactive foundation classes for primary school children covering English and Mathematics basics.",
                        // No curriculum value: not applicable at primary level.
                        "2000", "tuition", "negombo", "roshan",
                        kv("subject", "MATHEMATICS", "grade", "PRIMARY", "medium", "ENGLISH", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 5),
                ad("Spoken English Classes - Beginner to Advanced", "Practical, conversation-focused Spoken English classes for beginners to advanced learners. Both physical and online batches available.",
                        "3500", "language-classes", "kottawa", "ruwan",
                        kv("subject", "SPOKEN_ENGLISH", "classMode", "BOTH"),
                        "tuition", AdStatus.ACTIVE, 0),
                ad("IELTS Preparation - Band 7+ Target", "Focused IELTS preparation covering all four modules with weekly mock tests and one-on-one feedback sessions, delivered fully online.",
                        "7500", "language-classes", null, "thilina",
                        kv("subject", "IELTS", "classMode", "ONLINE"),
                        "tuition", AdStatus.ACTIVE, 2),
                ad("Japanese Language Classes - Beginner to Intermediate", "Beginner to intermediate Japanese language classes in Colombo, covering conversation, reading and writing (Hiragana/Katakana).",
                        "4000", "language-classes", "colombo", "anushka",
                        kv("subject", "JAPANESE", "classMode", "PHYSICAL"),
                        "tuition", AdStatus.ACTIVE, 14),
                ad("Piano Lessons for Kids & Adults", "Individual piano lessons at the student's home in and around Colombo. All ages and skill levels welcome, own keyboard/piano required.",
                        "3000", "music", "colombo", "sanjeewa",
                        kv("subject", "PIANO", "classMode", "HOME_VISIT"),
                        "tuition", AdStatus.ACTIVE, 9),
                ad("Guitar Classes - Acoustic & Electric", "Guitar classes for beginners and intermediate players, both physical classes in Kurunegala and online sessions available.",
                        "3200", "music", "kurunegala", "hasini",
                        kv("subject", "GUITAR", "classMode", "BOTH"),
                        "tuition", AdStatus.ACTIVE, 7),
                ad("Kandyan Dancing Classes", "Traditional Kandyan dancing classes for children and adults, held physically in Kandy. Costume guidance provided for exam/exhibition students.",
                        "2800", "dancing", "kandy", "nadeesha",
                        kv("subject", "KANDYAN_DANCING", "classMode", "PHYSICAL"),
                        "tuition", AdStatus.ACTIVE, 15),
                ad("Coding for Kids - Scratch & Python", "Introductory coding classes for school-age children using Scratch, moving on to Python fundamentals. Fully online with small group sizes.",
                        "5500", "technology-coding", null, "chamari",
                        kv("subject", "CODING", "grade", "GRADE_6_9", "classMode", "ONLINE"),
                        "tuition", AdStatus.ACTIVE, 3),
                ad("Chess Classes for Beginners", "Beginner-friendly chess classes for school children, held on weekends in Jaffna. Basic tactics, opening principles and friendly tournament practice.",
                        "1500", "other-education-tuition", "jaffna", "ruwan",
                        kv("subject", "CHESS", "classMode", "PHYSICAL"),
                        "tuition", AdStatus.PENDING_REVIEW, 0),
                ad("A/L ICT - O/L & A/L Combined Batch", "ICT classes covering both O/L and A/L syllabuses, with hands-on practical sessions alongside theory.",
                        "3200", "tuition", "matara", "thilina",
                        kv("subject", "ICT", "grade", "AL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.ACTIVE, 12),
                ad("Tamil Medium O/L Classes", "O/L classes conducted in Tamil medium, small group sessions with regular past paper practice.",
                        "2500", "tuition", "jaffna", "sanjeewa",
                        kv("subject", "TAMIL", "grade", "OL", "curriculum", "LOCAL", "medium", "TAMIL", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "tuition", AdStatus.REJECTED, 2));
    }

    private List<AdSeed> serviceAdSeeds() {
        return List.of(
                ad("Home Electrical Repair Services", "Domestic electrical installation and repair services in Colombo.",
                        "5000", "services", "colombo", "kamal",
                        kv("serviceType", "Electrical Repair", "experienceYears", "5", "serviceArea", "Colombo"),
                        "services", AdStatus.PENDING_REVIEW, 0),
                ad("AC Repair & Servicing", "Split unit repair, gas refill, and general servicing at your doorstep.",
                        "3500", "services", "nugegoda", "nimal",
                        kv("serviceType", "AC Repair & Servicing", "experienceYears", "8", "serviceArea", "Nugegoda & Colombo"),
                        "services", AdStatus.ACTIVE, 2),
                ad("Plumbing Services - Maharagama", "Leak repairs, pipe fitting, bathroom fittings installation.",
                        "2800", "services", "maharagama", "sunil",
                        kv("serviceType", "Plumbing", "experienceYears", "6", "serviceArea", "Maharagama"),
                        "services", AdStatus.ACTIVE, 0),
                ad("House Painting Services", "Interior and exterior painting, quality finishing guaranteed.",
                        "4500", "services", "kottawa", "chamari",
                        kv("serviceType", "House Painting", "experienceYears", "10", "serviceArea", "Colombo District"),
                        "services", AdStatus.ACTIVE, 5),
                ad("House Cleaning Services", "Deep cleaning for homes and apartments, own equipment provided.",
                        "2500", "services", "dehiwala", "dilani",
                        kv("serviceType", "House Cleaning", "experienceYears", "4", "serviceArea", "Dehiwala"),
                        "services", AdStatus.ACTIVE, 1),
                ad("Computer & Laptop Repair", "Hardware and software troubleshooting, data recovery available.",
                        "2000", "services", "moratuwa", "ruwan",
                        kv("serviceType", "Computer Repair", "experienceYears", "7", "serviceArea", "Moratuwa"),
                        "services", AdStatus.ACTIVE, 8),
                ad("Mobile Phone Screen Repair", "Screen replacement, battery replacement, and general phone repairs.",
                        "1500", "services", "kandy", "priyanka",
                        kv("serviceType", "Mobile Phone Repair", "experienceYears", "5", "serviceArea", "Kandy"),
                        "services", AdStatus.ACTIVE, 0),
                ad("CCTV Camera Installation", "Complete CCTV setup with remote viewing on your mobile phone.",
                        "6000", "services", "gampaha", "ashan",
                        kv("serviceType", "CCTV Installation", "experienceYears", "6", "serviceArea", "Gampaha District"),
                        "services", AdStatus.ACTIVE, 3),
                ad("Vehicle Detailing & Car Wash", "Interior and exterior detailing, polishing, and wax coating.",
                        "3000", "services", "negombo", "malithi",
                        kv("serviceType", "Vehicle Detailing", "experienceYears", "3", "serviceArea", "Negombo"),
                        "services", AdStatus.ACTIVE, 10),
                ad("House Moving & Packing Services", "Careful packing and safe transport, own lorry available.",
                        "8000", "services", "kurunegala", "nadeesha",
                        kv("serviceType", "Moving Services", "experienceYears", "9", "serviceArea", "Kurunegala & surrounding"),
                        "services", AdStatus.ACTIVE, 6),
                ad("Electrical Wiring & Repairs", "New wiring, fault finding, and safety inspections for homes.",
                        "4000", "services", "colombo", "roshan",
                        kv("serviceType", "Electrical Repair", "experienceYears", "12", "serviceArea", "Colombo & Suburbs"),
                        "services", AdStatus.ACTIVE, 14),
                ad("Plumbing & Pipe Fitting - Galle", "Bathroom and kitchen plumbing, water tank installation.",
                        "2600", "services", "galle", "kumari",
                        kv("serviceType", "Plumbing", "experienceYears", "5", "serviceArea", "Galle"),
                        "services", AdStatus.ACTIVE, 19),
                ad("Air Conditioner Installation & Repair", "New AC installation and repair for homes and offices.",
                        "3800", "services", "jaffna", "thilina",
                        kv("serviceType", "AC Installation & Repair", "experienceYears", "7", "serviceArea", "Jaffna"),
                        "services", AdStatus.ACTIVE, 25),
                ad("Home Deep Cleaning Services", "Complete home cleaning package, kitchen and bathroom included.",
                        "2200", "services", "ratnapura", "hasini",
                        kv("serviceType", "House Cleaning", "experienceYears", "3", "serviceArea", "Ratnapura"),
                        "services", AdStatus.REJECTED, 4),
                ad("CCTV & Security System Installation", "No longer taking new bookings, listing kept for reference.",
                        "5500", "services", "matara", "anushka",
                        kv("serviceType", "CCTV Installation", "experienceYears", "4", "serviceArea", "Matara"),
                        "services", AdStatus.DEACTIVATED, 21));
    }

    // Picks how many photos an ad gets from its group's pool: mostly 1, some with a small
    // gallery, so the Ad Details gallery view gets exercised without every ad needing 5 unique
    // images. The running index is shared across every ad ever seeded, not just its group, so
    // the pattern doesn't line up between neighbouring ads of the same category.
    private int imageCountFor(String group, int index) {
        int slot = index % 3;
        return switch (group) {
            case "cars", "motorcycles" -> slot == 0 ? 3 : slot == 1 ? 2 : 1;
            case "property" -> slot == 0 ? 4 : slot == 1 ? 2 : 1;
            case "phones" -> slot == 0 ? 2 : 1;
            default -> slot == 0 ? 2 : 1;
        };
    }

    private void attachImages(Ad ad, String group, int index) throws Exception {
        int count = imageCountFor(group, index);
        int poolSize = IMAGE_POOL_SIZE.get(group);
        int start = imageCursor.getOrDefault(group, 0);
        imageCursor.put(group, start + count);
        for (int order = 0; order < count; order++) {
            int n = (start + order) % poolSize + 1;
            String filename = String.format("%s_%02d.jpg", group, n);
            addSampleMedia(ad, "sample-media/" + group + "/" + filename, filename, order);
        }
    }

    // Tuition posters carry ad-specific subject text/layout baked into the image itself (unlike
    // the other groups' generic, unlabeled stock photos), so a shared rotating pool would
    // visibly mismatch posters to unrelated ads. Each ad instead gets its own dedicated,
    // correctly-labeled primary poster keyed by its fixed position in tuitionAdSeeds() (stable
    // regardless of how many earlier ads in the list were skipped vs. newly created this run);
    // only the 2nd/3rd gallery slot, where a subject mismatch isn't visually jarring, draws from
    // a small pool of generic (but still richly composed - schedule/course-info/classroom style)
    // secondary posters.
    private static final int TUITION_SECONDARY_IMAGE_POOL_SIZE = 4;

    // Explicit, deterministic media-density distribution across the 20 curated ads (0-based
    // index into tuitionAdSeeds()): 3 ads get a 3-image gallery, 5 get 2 images, the remaining 12
    // get just their primary poster - roughly the realistic "most ads have 1 photo, a few have a
    // small gallery" spread real tuition listings show, without every ad needing a unique set.
    private static final Set<Integer> TUITION_THREE_IMAGE_INDEXES = Set.of(0, 3, 13);
    private static final Set<Integer> TUITION_TWO_IMAGE_INDEXES = Set.of(2, 6, 10, 14, 18);

    private int tuitionAdIndex(String title) {
        List<AdSeed> seeds = tuitionAdSeeds();
        for (int idx = 0; idx < seeds.size(); idx++) {
            if (seeds.get(idx).title().equals(title)) {
                return idx;
            }
        }
        throw new IllegalStateException("Unknown tuition sample ad title: " + title);
    }

    private int tuitionImageCountFor(int adIndex) {
        if (TUITION_THREE_IMAGE_INDEXES.contains(adIndex)) {
            return 3;
        }
        if (TUITION_TWO_IMAGE_INDEXES.contains(adIndex)) {
            return 2;
        }
        return 1;
    }

    // Purely a function of adIndex (no shared mutable cursor): re-running the seeder must
    // reproduce the exact same media set for the exact same ad every time, regardless of call
    // order or how many other ads were created/skipped first.
    private void attachTuitionImages(Ad ad, int adIndex) throws Exception {
        int primaryNumber = adIndex + 1;
        int count = tuitionImageCountFor(adIndex);

        String primaryFilename = String.format("tuition_primary_%02d.jpg", primaryNumber);
        addSampleMedia(ad, "sample-media/tuition/" + primaryFilename, primaryFilename, 0);

        for (int order = 1; order < count; order++) {
            int n = (adIndex + order) % TUITION_SECONDARY_IMAGE_POOL_SIZE + 1;
            String filename = String.format("tuition_secondary_%02d.jpg", n);
            addSampleMedia(ad, "sample-media/tuition/" + filename, filename, order);
        }
    }

    // Reassociates an existing Tuition ad's media with the current expected poster(s) for its
    // position in tuitionAdSeeds(), without touching the ad row itself. Deletes whatever media it
    // currently has (including the backing storage object) first, so re-running the seeder always
    // converges on exactly the current poster set - safe to call whether the ad was just created
    // or already existed.
    private void reconcileTuitionMedia(Ad ad, int adIndex) {
        List<Media> existing = media.findByAdIdOrderByDisplayOrderAscIdAsc(ad.getId());
        for (Media m : existing) {
            try {
                storage.delete(m.getStorageKey());
            } catch (Exception e) {
                System.err.println("Failed to delete old tuition media file '" + m.getStorageKey() + "': " + e.getMessage());
            }
        }
        if (!existing.isEmpty()) {
            media.deleteAll(existing);
        }
        try {
            attachTuitionImages(ad, adIndex);
        } catch (Exception e) {
            System.err.println("Failed to attach tuition images for ad id " + ad.getId() + ": " + e.getMessage());
        }
    }

    private void addSampleMedia(Ad ad, String resourcePath, String filename, int order) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            StoredMedia stored = storage.store(input, filename, "image/jpeg");
            media.save(new Media(ad, stored.storageKey(), stored.contentType(), order));
        }
    }

    // Representative ACTIVE promotions so the feature is visible without any manual setup: six
    // ads in HOME_FEATURED (more than its visibleCount of 4, so the carousel has a second page to
    // page through), one ad in each other ad-linked placement (including all three
    // category-featured slots), two Tuition ads in TUITION_FEATURED plus one more in the generic
    // TOP_SEARCH placement, and three active banners so the homepage banner carousel has more than
    // one slide to rotate through. Most of the seeded ads stay unpromoted so ranking differences
    // show.
    private void seedSamplePromotionsIfMissing(Map<String, Ad> adsByTitle, Customer bannerAdvertiser) {
        if (promotions.count() > 0) {
            return;
        }

        activateSampleAdPromotion(adsByTitle.get("Toyota Aqua 2019 - Hybrid"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Samsung Galaxy S23 Ultra 256GB"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Two Storey House for Sale - Kottawa"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("A/L Combined Mathematics 2027"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("AC Repair & Servicing"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Honda Vezel 2018 - Hybrid RS"), "HOME_FEATURED_7D");

        activateSampleAdPromotion(adsByTitle.get("iPhone 15 Pro 256GB"), "TOP_SEARCH_7D");
        activateSampleAdPromotion(adsByTitle.get("Modern House for Sale in Nugegoda"), "PROPERTY_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Bajaj Pulsar 150 2019"), "VEHICLES_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("A/L Combined Mathematics - Theory & Revision"), "TUITION_HOME_FEATURED_30D");
        activateSampleAdPromotion(adsByTitle.get("Spoken English Classes - Beginner to Advanced"), "TUITION_HOME_FEATURED_30D");
        activateSampleAdPromotion(adsByTitle.get("O/L Mathematics - English Medium Group Classes"), "TUITION_SEARCH_TOP_30D");
        activateSampleAdPromotion(adsByTitle.get("A/L Physics - One-on-One Online Classes"), "TUITION_SEARCH_SIDEBAR_TOP_30D");

        promotionPlans.findByCode("HOME_BANNER_7D").ifPresent(plan -> {
            addSampleBannerPromotion(bannerAdvertiser, plan, "banner-promote-business.svg");
            addSampleBannerPromotion(bannerAdvertiser, plan, "banner-promote-reach.svg");
            addSampleBannerPromotion(bannerAdvertiser, plan, "banner-promote-grow.svg");
        });
    }

    // Seeded directly as ACTIVE without ever going through PromotionService, so no Payment is
    // ever created for these - paymentWaived=true keeps the Payment column ("Waived") consistent
    // with that instead of showing a stray, unactionable "Pending" next to an already-active promotion.
    private void activateSampleAdPromotion(Ad ad, String planCode) {
        if (ad == null) {
            return;
        }
        promotionPlans.findByCode(planCode).ifPresent(plan -> {
            Promotion promotion = promotions.save(new Promotion(
                    ad, ad.getSeller(), plan, plan.getPrice(), plan.getDurationDays(),
                    PromotionStatus.PENDING_PAYMENT, true));
            promotion.activate();
        });
    }

    private void addSampleBannerPromotion(Customer advertiser, PromotionPlan plan, String resourceFilename) {
        try {
            ClassPathResource resource = new ClassPathResource("sample-media/" + resourceFilename);
            try (InputStream input = resource.getInputStream()) {
                StoredMedia stored = storage.store(input, resourceFilename, "image/svg+xml");
                Media bannerMedia = media.save(Media.forPromotionBanner(
                        stored.storageKey(), stored.contentType()));
                Promotion promotion = promotions.save(Promotion.forBanner(
                        advertiser, plan, plan.getPrice(), plan.getDurationDays(), bannerMedia, null,
                        PromotionStatus.PENDING_PAYMENT, true));
                promotion.activate();
            }
        } catch (Exception e) {
            System.err.println("Failed to seed sample banner promotion: " + e.getMessage());
        }
    }
}
