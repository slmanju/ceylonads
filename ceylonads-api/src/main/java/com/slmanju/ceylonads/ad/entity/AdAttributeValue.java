package com.slmanju.ceylonads.ad.entity;

import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ad_attribute_values",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ad_attribute_value", columnNames = {"ad_id", "attribute_definition_id", "value_text"}),
        indexes = {
                @Index(name = "idx_ad_attribute_values_ad", columnList = "ad_id"),
                @Index(name = "idx_ad_attribute_values_def_text", columnList = "attribute_definition_id,value_text"),
                @Index(name = "idx_ad_attribute_values_def_number", columnList = "attribute_definition_id,value_number")
        })
public class AdAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    // TEXT/SELECT/MULTI_SELECT: the stable option value (or free text). Never a display label.
    @Column(name = "value_text", length = 255)
    private String valueText;

    // NUMBER/DECIMAL: kept as BigDecimal so range filters (year >= 2018, mileage <= 50000) are a
    // plain indexed column comparison rather than parsing valueText on every query.
    @Column(name = "value_number", precision = 19, scale = 4)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    protected AdAttributeValue() {
    }

    public AdAttributeValue(Ad ad, AttributeDefinition attributeDefinition, String valueText, BigDecimal valueNumber, Boolean valueBoolean) {
        this.ad = ad;
        this.attributeDefinition = attributeDefinition;
        this.valueText = valueText;
        this.valueNumber = valueNumber;
        this.valueBoolean = valueBoolean;
    }

    public Long getId() { return id; }
    public Ad getAd() { return ad; }
    public AttributeDefinition getAttributeDefinition() { return attributeDefinition; }
    public String getValueText() { return valueText; }
    public BigDecimal getValueNumber() { return valueNumber; }
    public Boolean getValueBoolean() { return valueBoolean; }
}
