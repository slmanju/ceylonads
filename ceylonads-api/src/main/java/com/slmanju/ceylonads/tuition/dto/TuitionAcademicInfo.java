package com.slmanju.ceylonads.tuition.dto;

import java.util.List;

public record TuitionAcademicInfo(
        String subject,
        String level,
        AttributeValueLabel curriculum,
        List<AttributeValueLabel> medium) {
}
