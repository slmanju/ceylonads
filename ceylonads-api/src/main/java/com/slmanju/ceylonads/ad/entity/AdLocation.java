package com.slmanju.ceylonads.ad.entity;

import com.slmanju.ceylonads.location.entity.Location;
import jakarta.persistence.*;

// Normalized ad<->location association (an ad has 0..N locations - e.g. online tuition has zero,
// a teacher covering two towns has two). Deliberately a plain join-row entity rather than a
// @ManyToMany on Ad, so batch read paths can fetch/group it exactly like AdAttributeValue instead
// of relying on collection-fetch joins that would multiply/duplicate Ad rows under pagination.
@Entity
@Table(name = "ad_locations", uniqueConstraints = @UniqueConstraint(
        name = "uk_ad_location", columnNames = {"ad_id", "location_id"}),
        indexes = {
                @Index(name = "idx_ad_locations_ad", columnList = "ad_id"),
                @Index(name = "idx_ad_locations_location", columnList = "location_id")
        })
public class AdLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    protected AdLocation() {
    }

    public AdLocation(Ad ad, Location location) {
        this.ad = ad;
        this.location = location;
    }

    public Long getId() { return id; }
    public Ad getAd() { return ad; }
    public Location getLocation() { return location; }
}
