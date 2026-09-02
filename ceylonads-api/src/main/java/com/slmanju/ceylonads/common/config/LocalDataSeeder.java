package com.slmanju.ceylonads.common.config;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.ad.service.AdAttributeService;
import com.slmanju.ceylonads.ad.service.AdLocationService;
import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.entity.Role;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.AttributeDefinitionRepository;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import com.slmanju.ceylonads.location.entity.Location;
import com.slmanju.ceylonads.location.entity.LocationType;
import com.slmanju.ceylonads.location.repository.LocationRepository;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.dto.StoredMedia;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionSlotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
// @Profile("local")
public class LocalDataSeeder {

// public class LocalDataSeeder implements CommandLineRunner {

    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final CategoryRepository categories;
    private final LocationRepository locations;
    private final AdRepository ads;
    private final MediaRepository media;
    private final MediaStorage storage;
    private final PasswordEncoder passwordEncoder;
    private final PromotionSlotRepository promotionSlots;
    private final PromotionPlanRepository promotionPlans;
    private final PromotionRepository promotions;
    private final AttributeDefinitionRepository attributeDefinitions;
    private final AttributeOptionRepository attributeOptions;
    private final AdAttributeService adAttributeService;
    private final AdLocationService adLocationService;

    // Bundled real photo pools under sample-media/<group>/<group>_NN.jpg, one pool per ad-image
    // "look" rather than per exact category, since several categories share a visual style.
    private static final Map<String, Integer> IMAGE_POOL_SIZE = Map.of(
            "cars", 19, "motorcycles", 6, "property", 17, "phones", 10, "education", 9, "services", 15);

    private final Map<String, Integer> imageCursor = new HashMap<>();

    public LocalDataSeeder(
            AccountRepository accounts,
            CustomerRepository customers,
            CategoryRepository categories,
            LocationRepository locations,
            AdRepository ads,
            MediaRepository media,
            MediaStorage storage,
            PasswordEncoder passwordEncoder,
            PromotionSlotRepository promotionSlots,
            PromotionPlanRepository promotionPlans,
            PromotionRepository promotions,
            AttributeDefinitionRepository attributeDefinitions,
            AttributeOptionRepository attributeOptions,
            AdAttributeService adAttributeService,
            AdLocationService adLocationService) {
        this.accounts = accounts;
        this.customers = customers;
        this.categories = categories;
        this.locations = locations;
        this.ads = ads;
        this.media = media;
        this.storage = storage;
        this.passwordEncoder = passwordEncoder;
        this.promotionSlots = promotionSlots;
        this.promotionPlans = promotionPlans;
        this.promotions = promotions;
        this.attributeDefinitions = attributeDefinitions;
        this.attributeOptions = attributeOptions;
        this.adAttributeService = adAttributeService;
        this.adLocationService = adLocationService;
    }

//     @Override
    @Transactional
    public void run() throws Exception {
// public void run(String... args) throws Exception {
        System.out.println("===============================");
        System.out.println("=== LocalDataSeeder STARTED ===");
        System.out.println("===============================");
        if (accounts.count() > 0) {
                System.out.println("=== ACCOUNTS > 0 ===");
            // Checked independently below so slots/plans (and now the moderator test account)
            // still get seeded into an existing local database that predates them; categories
            // already exist by this point in that case.
            seedPromotionSlotsAndPlansIfMissing();
            ensureModeratorAccountSeeded();

            System.out.println("=== LocalDataSeeder END 0 ===");
            return;
        }
        System.out.println("=== ACCOUNTS < 0 ===");

        accounts.save(new Account(
                "admin", passwordEncoder.encode("admin123"), "admin@ceylonads.local", Role.ADMIN));

        Map<String, Customer> people = seedCustomers();
        ensureModeratorAccountSeeded();

        Category vehicles = categories.save(new Category("Vehicles", "vehicles", null, 10));
        Category property = categories.save(new Category("Property", "property", null, 20));
        Category mobiles = categories.save(new Category("Mobile Phones", "mobile-phones", null, 30));
        Category tuition = categories.save(new Category("Education & Tuition", "education-tuition", null, 40));
        Category services = categories.save(new Category("Services", "services", null, 50));

        Category cars = categories.save(new Category("Cars", "cars", vehicles, 11));
        Category motorcycles = categories.save(new Category("Motorcycles", "motorcycles", vehicles, 12));
        Category houses = categories.save(new Category("Houses", "houses", property, 21));
        Category schoolTuition = categories.save(new Category("School Tuition", "school-tuition", tuition, 41));

        // Attributes attach to the leaf categories a user can actually select in the Post Ad
        // wizard (CategoryStep only lets you pick a leaf), not the parent groups above.
        seedCarAttributes(cars);
        seedMotorcycleAttributes(motorcycles);
        seedHouseAttributes(houses);
        seedSchoolTuitionAttributes(schoolTuition);
        seedMobilePhoneAttributes(mobiles);
        seedServiceAttributes(services);

        Map<String, Location> place = seedLocations();
        Map<String, Category> category = Map.of(
                "cars", cars, "motorcycles", motorcycles, "houses", houses,
                "mobiles", mobiles, "tuition", schoolTuition, "services", services);

        List<AdSeed> seeds = new ArrayList<>();
        seeds.addAll(carAdSeeds());
        seeds.addAll(motorcycleAdSeeds());
        seeds.addAll(propertyAdSeeds());
        seeds.addAll(mobileAdSeeds());
        seeds.addAll(tuitionAdSeeds());
        seeds.addAll(serviceAdSeeds());

        Map<String, Ad> createdByTitle = new HashMap<>();
        Instant now = Instant.now();
        int i = 0;
        for (AdSeed seed : seeds) {
            // created_at is updatable=false, so it must be backdated on the transient instance
            // before the initial INSERT (identity generation persists immediately on save) -
            // a later update would silently be dropped from the generated UPDATE statement.
            Ad ad = new Ad(
                    seed.title(), seed.description(), new BigDecimal(seed.price()),
                    category.get(seed.categoryKey()), people.get(seed.sellerKey()));
            switch (seed.status()) {
                case ACTIVE -> ad.approve(null);
                case REJECTED -> ad.reject(null);
                case DEACTIVATED -> { ad.approve(null); ad.deactivate(); }
                default -> { /* leave as PENDING_REVIEW, the constructor default */ }
            }
            Instant createdAt = now.minus(Duration.ofDays(seed.daysAgo()))
                    .minus(Duration.ofHours((i * 7) % 24))
                    .minus(Duration.ofMinutes((i * 13) % 60));
            ad.backdateCreatedAt(createdAt);

            ad = ads.save(ad);
            adAttributeService.replaceValues(ad, seed.attrs());
            List<String> locationSlugs = seed.locationKey() == null
                    ? List.of() : List.of(place.get(seed.locationKey()).getSlug());
            adLocationService.replaceLocations(ad, locationSlugs, seed.attrs());
            try {
                attachImages(ad, seed.imageGroup(), i);
            } catch (Exception e) {
                IO.println("Error while attaching images " + e.getMessage());
            }
            createdByTitle.put(seed.title(), ad);
            i++;
        }

        // Categories exist by this point in a fresh seed run, so category-bound slots can resolve.
        seedPromotionSlotsAndPlansIfMissing();
        // Ads exist by this point too, so representative promotions can be attached to them,
        // making the promotion feature visible without any manual setup in local development.
        seedSamplePromotionsIfMissing(createdByTitle, people.get("hasini"));

        System.out.println("=== LocalDataSeeder END 1 ===");
    }

    private Map<String, Customer> seedCustomers() {
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

        Map<String, Customer> byKey = new LinkedHashMap<>();
        for (Person p : people) {
            Account account = accounts.save(new Account(
                    p.username(), passwordEncoder.encode("customer123"), p.email(), Role.CUSTOMER));
            byKey.put(p.username(), customers.save(new Customer(account, p.displayName(), p.phone())));
        }
        return byKey;
    }

    // Idempotent: safe to call whether this run seeded a fresh database or found one already
    // populated. A Moderator needs a Customer profile just like any other ad poster (see
    // AdService.create), so it gets one here too - the account model itself stays the single
    // shared Account entity, just with role MODERATOR instead of CUSTOMER.
    private void ensureModeratorAccountSeeded() {
        if (accounts.existsByUsernameIgnoreCase("moderator1")) {
            return;
        }
        Account account = accounts.save(new Account(
                "moderator1", passwordEncoder.encode("moderator123"), "moderator1@ceylonads.local", Role.MODERATOR));
        customers.save(new Customer(account, "CeylonAds Moderator", "0770000000"));
    }

    private Map<String, Location> seedLocations() {
        Map<String, Location> byKey = new LinkedHashMap<>();
        Location western = locations.save(new Location("Western Province", "western-province", LocationType.PROVINCE, null));
        Location central = locations.save(new Location("Central Province", "central-province", LocationType.PROVINCE, null));
        Location northWestern = locations.save(new Location("North Western Province", "north-western-province", LocationType.PROVINCE, null));
        Location southern = locations.save(new Location("Southern Province", "southern-province", LocationType.PROVINCE, null));
        Location northern = locations.save(new Location("Northern Province", "northern-province", LocationType.PROVINCE, null));
        Location sabaragamuwa = locations.save(new Location("Sabaragamuwa Province", "sabaragamuwa-province", LocationType.PROVINCE, null));

        Location colomboDistrict = locations.save(new Location("Colombo District", "colombo-district", LocationType.DISTRICT, western));
        Location gampahaDistrict = locations.save(new Location("Gampaha District", "gampaha-district", LocationType.DISTRICT, western));
        Location kaluthaDistrict = locations.save(new Location("Kalutara District", "kalutara-district", LocationType.DISTRICT, western));
        Location kandyDistrict = locations.save(new Location("Kandy District", "kandy-district", LocationType.DISTRICT, central));
        Location kurunegalaDistrict = locations.save(new Location("Kurunegala District", "kurunegala-district", LocationType.DISTRICT, northWestern));
        Location galleDistrict = locations.save(new Location("Galle District", "galle-district", LocationType.DISTRICT, southern));
        Location mataraDistrict = locations.save(new Location("Matara District", "matara-district", LocationType.DISTRICT, southern));
        Location jaffnaDistrict = locations.save(new Location("Jaffna District", "jaffna-district", LocationType.DISTRICT, northern));
        Location ratnapuraDistrict = locations.save(new Location("Ratnapura District", "ratnapura-district", LocationType.DISTRICT, sabaragamuwa));

        byKey.put("colombo", locations.save(new Location("Colombo", "colombo", LocationType.CITY, colomboDistrict)));
        byKey.put("nugegoda", locations.save(new Location("Nugegoda", "nugegoda", LocationType.CITY, colomboDistrict)));
        byKey.put("maharagama", locations.save(new Location("Maharagama", "maharagama", LocationType.CITY, colomboDistrict)));
        byKey.put("kottawa", locations.save(new Location("Kottawa", "kottawa", LocationType.CITY, colomboDistrict)));
        byKey.put("dehiwala", locations.save(new Location("Dehiwala", "dehiwala", LocationType.CITY, colomboDistrict)));
        byKey.put("moratuwa", locations.save(new Location("Moratuwa", "moratuwa", LocationType.CITY, colomboDistrict)));
        byKey.put("gampaha", locations.save(new Location("Gampaha", "gampaha", LocationType.CITY, gampahaDistrict)));
        byKey.put("negombo", locations.save(new Location("Negombo", "negombo", LocationType.CITY, gampahaDistrict)));
        byKey.put("kalutara", locations.save(new Location("Kalutara", "kalutara", LocationType.CITY, kaluthaDistrict)));
        byKey.put("kandy", locations.save(new Location("Kandy", "kandy", LocationType.CITY, kandyDistrict)));
        byKey.put("kurunegala", locations.save(new Location("Kurunegala", "kurunegala", LocationType.CITY, kurunegalaDistrict)));
        byKey.put("galle", locations.save(new Location("Galle", "galle", LocationType.CITY, galleDistrict)));
        byKey.put("matara", locations.save(new Location("Matara", "matara", LocationType.CITY, mataraDistrict)));
        byKey.put("jaffna", locations.save(new Location("Jaffna", "jaffna", LocationType.CITY, jaffnaDistrict)));
        byKey.put("ratnapura", locations.save(new Location("Ratnapura", "ratnapura", LocationType.CITY, ratnapuraDistrict)));
        return byKey;
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

    // subject/grade values below use the canonical SELECT option values from
    // V10__tuition_filter_master_data.sql (see TuitionFilterMetadataService) - matchOption()
    // resolves case-insensitively but does not normalize whitespace/punctuation, so these must be
    // the exact canonical values (e.g. "A/L" would no longer match the "AL" option).
    private List<AdSeed> tuitionAdSeeds() {
        return List.of(
                ad("O/L Mathematics Tuition - English Medium", "Individual and small group classes. Physical classes in Nugegoda and online classes available.",
                        "3000", "tuition", "nugegoda", "nimal",
                        kv("subject", "Mathematics", "grade", "OL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "BOTH", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 0),
                ad("O/L Science - Sinhala Medium", "Experienced teacher, past paper discussions, weekly revision tests.",
                        "2500", "tuition", "maharagama", "kamal",
                        kv("subject", "Science", "grade", "OL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 2),
                ad("A/L Combined Mathematics 2027", "Theory and revision classes, model paper discussions included.",
                        "4000", "tuition", "colombo", "sunil",
                        kv("subject", "COMBINED_MATHEMATICS", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "BOTH", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 0),
                ad("A/L Physics 2027 - Colombo", "Concept based teaching with practical examples and past papers.",
                        "3800", "tuition", "colombo", "chamari",
                        kv("subject", "Physics", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 1),
                ad("A/L Chemistry Classes - Kandy", "Small class sizes, individual attention, weekly assignments.",
                        "3800", "tuition", "kandy", "dilani",
                        kv("subject", "Chemistry", "grade", "AL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 5),
                ad("Spoken English Classes - All Levels", "Practical conversation focused classes for beginners to advanced.",
                        "3500", "tuition", "kottawa", "ruwan",
                        kv("subject", "SPOKEN_ENGLISH", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "BOTH", "classType", "INDIVIDUAL"),
                        "education", AdStatus.ACTIVE, 3),
                ad("IELTS Classes - Colombo", "Focused preparation for all four modules, mock tests included.",
                        "6000", "tuition", "colombo", "priyanka",
                        kv("subject", "IELTS", "curriculum", "PROFESSIONAL", "medium", "ENGLISH", "classMode", "BOTH", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 0),
                ad("Edexcel Mathematics - Online Classes", "IGCSE and AS level Mathematics, recorded sessions provided.",
                        // Online class mode: zero physical locations (see AdLocationService),
                        // so locationKey is null rather than a picked-arbitrarily city.
                        "5000", "tuition", null, "ashan",
                        kv("subject", "Mathematics", "grade", "IGCSE", "curriculum", "EDEXCEL", "medium", "ENGLISH", "classMode", "ONLINE", "classType", "INDIVIDUAL"),
                        "education", AdStatus.ACTIVE, 7),
                ad("Cambridge Mathematics - Nugegoda", "IGCSE Cambridge syllabus, past paper practice every week.",
                        "5200", "tuition", "nugegoda", "malithi",
                        kv("subject", "Mathematics", "grade", "IGCSE", "curriculum", "CAMBRIDGE", "medium", "ENGLISH", "classMode", "BOTH", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 11),
                ad("ICT Classes - O/L & A/L", "Covers theory and practical sessions, project guidance included.",
                        "3200", "tuition", "kurunegala", "nadeesha",
                        kv("subject", "ICT", "grade", "OL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 9),
                ad("Primary Mathematics - Grade 3 to 5", "Fun and interactive classes to build a strong foundation.",
                        "2000", "tuition", "negombo", "roshan",
                        kv("subject", "Mathematics", "grade", "PRIMARY", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 4),
                ad("Sinhala Language Classes - Galle", "O/L Sinhala classes, essay writing and grammar practice.",
                        "2500", "tuition", "galle", "kumari",
                        kv("subject", "Sinhala", "grade", "OL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 16),
                ad("Tamil Language Classes - Jaffna", "O/L Tamil classes with experienced teacher, small groups.",
                        "2500", "tuition", "jaffna", "thilina",
                        kv("subject", "Tamil", "grade", "OL", "curriculum", "LOCAL", "medium", "TAMIL", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 20),
                ad("A/L Combined Maths - Online Classes", "Individual online sessions, flexible scheduling available.",
                        "4200", "tuition", null, "anushka",
                        kv("subject", "COMBINED_MATHEMATICS", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "ONLINE", "classType", "INDIVIDUAL"),
                        "education", AdStatus.ACTIVE, 6),
                ad("O/L Mathematics - Individual Classes", "One on one classes tailored to the student's pace.",
                        "3500", "tuition", "kalutara", "sanjeewa",
                        kv("subject", "Mathematics", "grade", "OL", "curriculum", "LOCAL", "medium", "SINHALA", "classMode", "PHYSICAL", "classType", "INDIVIDUAL"),
                        "education", AdStatus.ACTIVE, 13),
                ad("English Classes for Kids - Ratnapura", "Beginner friendly classes for primary school children.",
                        "2200", "tuition", "ratnapura", "hasini",
                        kv("subject", "English", "grade", "PRIMARY", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "PHYSICAL", "classType", "GROUP"),
                        "education", AdStatus.ACTIVE, 24),
                ad("A/L Physics - Individual Online Classes", "Flexible timing, recorded sessions available on request.",
                        "4500", "tuition", null, "kamal",
                        kv("subject", "Physics", "grade", "AL", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "ONLINE", "classType", "INDIVIDUAL"),
                        "education", AdStatus.PENDING_REVIEW, 0));
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

    private void seedCarAttributes(Category cars) {
        AttributeDefinition make = attr(cars, "make", "Make", AttributeDataType.SELECT, true, true, null, 10);
        options(make, "Toyota", "Honda", "Nissan", "Suzuki", "Mitsubishi", "BMW", "Mercedes-Benz", "Other");

        attr(cars, "model", "Model", AttributeDataType.TEXT, true, false, null, 20);
        attr(cars, "year", "Year", AttributeDataType.NUMBER, true, true, null, 30);
        attr(cars, "mileage", "Mileage", AttributeDataType.NUMBER, true, true, "km", 40);

        AttributeDefinition fuelType = attr(cars, "fuelType", "Fuel Type", AttributeDataType.SELECT, true, true, null, 50);
        option(fuelType, "PETROL", "Petrol", 1);
        option(fuelType, "DIESEL", "Diesel", 2);
        option(fuelType, "HYBRID", "Hybrid", 3);
        option(fuelType, "ELECTRIC", "Electric", 4);

        AttributeDefinition transmission = attr(cars, "transmission", "Transmission", AttributeDataType.SELECT, true, true, null, 60);
        option(transmission, "AUTOMATIC", "Automatic", 1);
        option(transmission, "MANUAL", "Manual", 2);
        option(transmission, "CVT", "CVT", 3);

        AttributeDefinition condition = attr(cars, "condition", "Condition", AttributeDataType.SELECT, false, true, null, 70);
        option(condition, "NEW", "New", 1);
        option(condition, "USED", "Used", 2);
        option(condition, "RECONDITIONED", "Reconditioned", 3);
    }

    private void seedMotorcycleAttributes(Category motorcycles) {
        AttributeDefinition make = attr(motorcycles, "make", "Make", AttributeDataType.SELECT, true, true, null, 10);
        options(make, "Bajaj", "Honda", "Yamaha", "TVS", "Hero", "Suzuki", "Other");

        attr(motorcycles, "model", "Model", AttributeDataType.TEXT, true, false, null, 20);
        attr(motorcycles, "year", "Year", AttributeDataType.NUMBER, true, true, null, 30);
        attr(motorcycles, "mileage", "Mileage", AttributeDataType.NUMBER, true, true, "km", 40);

        AttributeDefinition fuelType = attr(motorcycles, "fuelType", "Fuel Type", AttributeDataType.SELECT, true, true, null, 50);
        option(fuelType, "PETROL", "Petrol", 1);
        option(fuelType, "ELECTRIC", "Electric", 2);

        AttributeDefinition transmission = attr(motorcycles, "transmission", "Transmission", AttributeDataType.SELECT, true, true, null, 60);
        option(transmission, "MANUAL", "Manual", 1);
        option(transmission, "AUTOMATIC", "Automatic", 2);

        AttributeDefinition condition = attr(motorcycles, "condition", "Condition", AttributeDataType.SELECT, false, true, null, 70);
        option(condition, "NEW", "New", 1);
        option(condition, "USED", "Used", 2);
        option(condition, "RECONDITIONED", "Reconditioned", 3);
    }

    private void seedHouseAttributes(Category houses) {
        AttributeDefinition propertyType = attr(houses, "propertyType", "Property Type", AttributeDataType.SELECT, true, true, null, 10);
        option(propertyType, "HOUSE", "House", 1);
        option(propertyType, "APARTMENT", "Apartment", 2);
        option(propertyType, "LAND", "Land", 3);
        option(propertyType, "COMMERCIAL", "Commercial", 4);
        option(propertyType, "ROOM", "Room", 5);

        attr(houses, "bedrooms", "Bedrooms", AttributeDataType.NUMBER, true, true, null, 20);
        attr(houses, "bathrooms", "Bathrooms", AttributeDataType.NUMBER, true, true, null, 30);
        attr(houses, "landSize", "Land Size", AttributeDataType.DECIMAL, false, true, "perches", 40);
        attr(houses, "floorArea", "Floor Area", AttributeDataType.DECIMAL, false, true, "sq ft", 50);
        attr(houses, "furnished", "Furnished", AttributeDataType.BOOLEAN, false, true, null, 60);
    }

    private void seedSchoolTuitionAttributes(Category schoolTuition) {
        attr(schoolTuition, "subject", "Subject", AttributeDataType.TEXT, true, false, null, 10);
        attr(schoolTuition, "grade", "Grade / Level", AttributeDataType.TEXT, false, false, null, 20);

        AttributeDefinition curriculum = attr(schoolTuition, "curriculum", "Curriculum", AttributeDataType.SELECT, true, true, null, 30);
        option(curriculum, "LOCAL", "Local", 1);
        option(curriculum, "EDEXCEL", "Edexcel", 2);
        option(curriculum, "CAMBRIDGE", "Cambridge", 3);
        option(curriculum, "IB", "IB", 4);
        option(curriculum, "PROFESSIONAL", "Professional", 5);

        // A tutor commonly teaches in more than one medium at once (e.g. English + Sinhala), so
        // this is the confirmed multi-select audit case - see AdLocationService/AdAttributeService
        // for how MULTI_SELECT is handled generically once a definition is marked as such.
        AttributeDefinition medium = attr(schoolTuition, "medium", "Medium", AttributeDataType.MULTI_SELECT, true, true, null, 40);
        option(medium, "SINHALA", "Sinhala", 1);
        option(medium, "ENGLISH", "English", 2);
        option(medium, "TAMIL", "Tamil", 3);

        AttributeDefinition classMode = attr(schoolTuition, "classMode", "Class Mode", AttributeDataType.SELECT, true, true, null, 50);
        option(classMode, "PHYSICAL", "Physical", 1);
        option(classMode, "ONLINE", "Online", 2);
        option(classMode, "BOTH", "Online & Physical", 3);

        AttributeDefinition classType = attr(schoolTuition, "classType", "Class Type", AttributeDataType.SELECT, false, true, null, 60);
        option(classType, "INDIVIDUAL", "Individual", 1);
        option(classType, "GROUP", "Group", 2);
        option(classType, "BOTH", "Individual & Group", 3);
    }

    private void seedMobilePhoneAttributes(Category mobiles) {
        AttributeDefinition brand = attr(mobiles, "brand", "Brand", AttributeDataType.SELECT, true, true, null, 10);
        options(brand, "Apple", "Samsung", "Xiaomi", "Huawei", "OnePlus", "Google", "Other");

        attr(mobiles, "model", "Model", AttributeDataType.TEXT, true, false, null, 20);

        AttributeDefinition condition = attr(mobiles, "condition", "Condition", AttributeDataType.SELECT, true, true, null, 30);
        option(condition, "NEW", "New", 1);
        option(condition, "USED", "Used", 2);
        option(condition, "REFURBISHED", "Refurbished", 3);

        AttributeDefinition storage = attr(mobiles, "storage", "Storage", AttributeDataType.SELECT, true, true, null, 40);
        option(storage, "64GB", "64 GB", 1);
        option(storage, "128GB", "128 GB", 2);
        option(storage, "256GB", "256 GB", 3);
        option(storage, "512GB", "512 GB", 4);
        option(storage, "1TB", "1 TB", 5);

        attr(mobiles, "warranty", "Warranty", AttributeDataType.BOOLEAN, false, true, null, 50);
    }

    private void seedServiceAttributes(Category services) {
        attr(services, "serviceType", "Service Type", AttributeDataType.TEXT, true, false, null, 10);
        attr(services, "experienceYears", "Experience", AttributeDataType.NUMBER, false, true, "years", 20);
        attr(services, "serviceArea", "Service Area", AttributeDataType.TEXT, false, false, null, 30);
    }

    private AttributeDefinition attr(
            Category category, String key, String name, AttributeDataType dataType,
            boolean required, boolean filterable, String unit, int displayOrder) {
        return attributeDefinitions.save(new AttributeDefinition(
                category, key, name, dataType, required, filterable, false, unit, displayOrder));
    }

    private void option(AttributeDefinition definition, String value, String label, int displayOrder) {
        attributeOptions.save(new AttributeOption(definition, value, label, displayOrder));
    }

    private void options(AttributeDefinition definition, String... valuesAndLabels) {
        int order = 1;
        for (String value : valuesAndLabels) {
            option(definition, value, value, order++);
        }
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

    private void addSampleMedia(Ad ad, String resourcePath, String filename, int order) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            StoredMedia stored = storage.store(input, filename, "image/jpeg");
            media.save(new Media(ad, stored.storageKey(), stored.contentType(), order));
        }
    }

    private void seedPromotionSlotsAndPlansIfMissing() {
        if (promotionSlots.count() > 0) {
                System.out.println("=== PROMOTION SLOTS COUNT > 0 ===");
            return;
        }

        PromotionSlot homeFeatured = promotionSlots.save(new PromotionSlot(
                "HOME_FEATURED", "Homepage Featured", "The homepage Featured Ads section.",
                PlacementType.HOME_FEATURED, null, SourceChannel.MAIN_SITE, 20, 4, 10));
        promotionPlans.save(new PromotionPlan(
                "HOME_FEATURED_7D", "Homepage Featured",
                "Highlight your ad on the CeylonAds homepage Featured Ads section.",
                homeFeatured, 7, new BigDecimal("750.00"), true, true, 10));
        promotionPlans.save(new PromotionPlan(
                "HOME_FEATURED_30D", "Homepage Featured — 30 Days",
                "Highlight your ad on the CeylonAds homepage Featured Ads section for a full month.",
                homeFeatured, 30, new BigDecimal("2500.00"), true, true, 11));

        // Local/demo plans that exercise the optional-payment paths without a real bank transfer.
        promotionPlans.save(new PromotionPlan(
                "HOME_FEATURED_LAUNCH", "Launch Special — Homepage Featured",
                "A short complimentary homepage feature for new sellers trying CeylonAds promotions. "
                        + "Subject to admin approval, no payment required.",
                homeFeatured, 3, BigDecimal.ZERO, false, true, 12));
        promotionPlans.save(new PromotionPlan(
                "HOME_FEATURED_DEMO", "Demo Instant Promotion (local/dev only)",
                "Activates immediately with no payment and no approval - for exercising the "
                        + "auto-activation path in local development. Not intended for production.",
                homeFeatured, 1, BigDecimal.ZERO, false, false, 13));

        PromotionSlot homeBanner = promotionSlots.save(new PromotionSlot(
                "HOME_BANNER", "Homepage Banner", "A rotating carousel of banners on the homepage.",
                PlacementType.HOME_BANNER, null, SourceChannel.MAIN_SITE, 6, 1, 20));
        promotionPlans.save(new PromotionPlan(
                "HOME_BANNER_7D", "Homepage Banner",
                "A rotating banner placement on the CeylonAds homepage.",
                homeBanner, 7, new BigDecimal("5000.00"), true, true, 20));

        // Capacity is kept comfortably above the concurrent demand the shared test fixtures
        // generate across the suite (several tests activate a TOP_SEARCH promotion and never
        // expire it within the run). visibleCount stays small since only a few promoted results
        // should be boosted to the top of any one search/browse request at a time.
        PromotionSlot searchTop = promotionSlots.save(new PromotionSlot(
                "SEARCH_TOP", "Top Search", "Top placement in general search and browse results.",
                PlacementType.TOP_SEARCH, null, SourceChannel.MAIN_SITE, 20, 3, 90));
        promotionPlans.save(new PromotionPlan(
                "TOP_SEARCH_7D", "Top Search",
                "Rank higher in general search and browse results.",
                searchTop, 7, new BigDecimal("400.00"), true, true, 90));

        seedCategoryFeaturedSlot("VEHICLES_FEATURED", "Vehicles Featured",
                "the Vehicles category page", "vehicles", SourceChannel.MAIN_SITE, 12, 4, 30);
        seedCategoryFeaturedSlot("PROPERTY_FEATURED", "Property Featured",
                "the Property category page", "property", SourceChannel.MAIN_SITE, 12, 4, 40);
        seedCategoryFeaturedSlot("TUITION_FEATURED", "Education & Tuition Featured",
                "the Education & Tuition category page", "education-tuition", SourceChannel.TUITION, 12, 4, 50);
    }

    private void seedCategoryFeaturedSlot(
            String code, String name, String pageDescription, String categorySlug, SourceChannel sourceChannel,
            int capacity, int visibleCount, int displayOrder) {
        categories.findBySlug(categorySlug).ifPresent(category -> {
            PromotionSlot slot = promotionSlots.save(new PromotionSlot(
                    code, name, "Top placement on " + pageDescription + ".",
                    PlacementType.CATEGORY_FEATURED, category, sourceChannel, capacity, visibleCount, displayOrder));
            promotionPlans.save(new PromotionPlan(
                    code + "_7D", name,
                    "Appear above regular ads on " + pageDescription + ".",
                    slot, 7, new BigDecimal("1000.00"), true, true, displayOrder));
        });
    }

    // Representative ACTIVE promotions so the feature is visible without any manual setup: six
    // ads in HOME_FEATURED (more than its visibleCount of 4, so the carousel has a second page to
    // page through), one ad in each other ad-linked placement (including all three
    // category-featured slots), plus three active banners so the homepage banner carousel has
    // more than one slide to rotate through. Most of the ~100 seeded ads stay unpromoted so
    // ranking differences show.
    private void seedSamplePromotionsIfMissing(Map<String, Ad> adsByTitle, Customer bannerAdvertiser) {
        if (promotions.count() > 0) {
            return;
        }

        activateSampleAdPromotion(adsByTitle.get("Toyota Aqua 2019 - Hybrid"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Samsung Galaxy S23 Ultra 256GB"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Two Storey House for Sale - Kottawa"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("A/L Combined Mathematics 2027"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Home Electrical Repair Services"), "HOME_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Honda Vezel 2018 - Hybrid RS"), "HOME_FEATURED_7D");

        activateSampleAdPromotion(adsByTitle.get("iPhone 15 Pro 256GB"), "TOP_SEARCH_7D");
        activateSampleAdPromotion(adsByTitle.get("Modern House for Sale in Nugegoda"), "PROPERTY_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("Bajaj Pulsar 150 2019"), "VEHICLES_FEATURED_7D");
        activateSampleAdPromotion(adsByTitle.get("O/L Mathematics Tuition - English Medium"), "TUITION_FEATURED_7D");

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
            IO.println("Failed to seed sample banner promotion" + e.getMessage());
        }
    }
}
