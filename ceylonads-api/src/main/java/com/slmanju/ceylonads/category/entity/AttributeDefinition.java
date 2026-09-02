package com.slmanju.ceylonads.category.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attribute_definitions", uniqueConstraints = @UniqueConstraint(
        name = "uk_attribute_definition_category_key", columnNames = {"category_id", "attribute_key"}))
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // "key" is a reserved word in several SQL dialects, so the column is named attribute_key.
    @Column(name = "attribute_key", nullable = false, length = 60)
    private String key;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttributeDataType dataType;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean filterable;

    @Column(nullable = false)
    private boolean searchable;

    @Column(length = 30)
    private String unit;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Read-only navigational collection (AttributeOption owns the relationship via its own
    // repository) so a single query can join-fetch a definition's options for the ad-detail read
    // path instead of a separate IN-clause round trip.
    @OneToMany(mappedBy = "attributeDefinition", fetch = FetchType.LAZY)
    @OrderBy("displayOrder asc, id asc")
    private List<AttributeOption> options = new ArrayList<>();

    protected AttributeDefinition() {
    }

    public AttributeDefinition(
            Category category,
            String key,
            String name,
            AttributeDataType dataType,
            boolean required,
            boolean filterable,
            boolean searchable,
            String unit,
            int displayOrder) {
        this.category = category;
        this.key = key;
        this.name = name;
        this.dataType = dataType;
        this.required = required;
        this.filterable = filterable;
        this.searchable = searchable;
        this.unit = unit;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public AttributeDataType getDataType() { return dataType; }
    public boolean isRequired() { return required; }
    public boolean isFilterable() { return filterable; }
    public boolean isSearchable() { return searchable; }
    public String getUnit() { return unit; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AttributeOption> getOptions() { return options; }

    // key and dataType are immutable once created: changing dataType after values exist would
    // corrupt the typed AdAttributeValue columns, and key is what clients/URLs address it by.
    public void update(String name, boolean required, boolean filterable, boolean searchable, String unit, int displayOrder, boolean active) {
        this.name = name;
        this.required = required;
        this.filterable = filterable;
        this.searchable = searchable;
        this.unit = unit;
        this.displayOrder = displayOrder;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }
}
