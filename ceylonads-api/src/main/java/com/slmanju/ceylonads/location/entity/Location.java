package com.slmanju.ceylonads.location.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "locations", uniqueConstraints = @UniqueConstraint(name = "uk_location_slug", columnNames = "slug"))
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 140)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    @Column(nullable = false)
    private boolean active = true;

    protected Location() {
    }

    public Location(String name, String slug, LocationType type, Location parent) {
        this.name = name;
        this.slug = slug;
        this.type = type;
        this.parent = parent;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public LocationType getType() { return type; }
    public Location getParent() { return parent; }
    public boolean isActive() { return active; }
}
