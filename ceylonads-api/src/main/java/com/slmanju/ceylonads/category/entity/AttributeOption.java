package com.slmanju.ceylonads.category.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "attribute_options", uniqueConstraints = @UniqueConstraint(
        name = "uk_attribute_option_definition_value", columnNames = {"attribute_definition_id", "option_value"}))
public class AttributeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    // "value" is a reserved word in several SQL dialects (H2 included), same reasoning as
    // AttributeDefinition.key -> attribute_key.
    @Column(name = "option_value", nullable = false, length = 60)
    private String value;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected AttributeOption() {
    }

    public AttributeOption(AttributeDefinition attributeDefinition, String value, String label, int displayOrder) {
        this.attributeDefinition = attributeDefinition;
        this.value = value;
        this.label = label;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public AttributeDefinition getAttributeDefinition() { return attributeDefinition; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }

    public void update(String label, int displayOrder, boolean active) {
        this.label = label;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
