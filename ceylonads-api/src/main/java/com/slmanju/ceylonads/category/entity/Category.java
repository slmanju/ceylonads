package com.slmanju.ceylonads.category.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_category_slug", columnNames = "slug"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected Category() {
    }

    public Category(String name, String slug, Category parent, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Category getParent() { return parent; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }

    public void update(String name, int displayOrder, boolean active) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.active = active;
    }
}
