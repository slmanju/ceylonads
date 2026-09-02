package com.slmanju.ceylonads.tuition.dto;

import java.util.List;

// classPurposes is always empty today: no attribute for it exists in the current tuition category
// schema. Kept as a field (rather than omitted) so the frontend contract is stable if/when that
// data is introduced.
public record TuitionClassInfo(
        List<AttributeValueLabel> deliveryModes,
        List<AttributeValueLabel> classFormats,
        List<AttributeValueLabel> classPurposes) {
}
