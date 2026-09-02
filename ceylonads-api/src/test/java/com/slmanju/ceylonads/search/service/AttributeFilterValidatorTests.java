package com.slmanju.ceylonads.search.service;

import com.slmanju.ceylonads.category.entity.AttributeDataType;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.category.repository.AttributeDefinitionRepository;
import com.slmanju.ceylonads.category.repository.AttributeOptionRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.search.dto.AttributeFilterCriterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Plain unit test (no Spring context) covering the exact regression this class exists to prevent:
// "subject" (and any other key) is defined once per category, so validate() must union options
// across every same-key definition rather than only checking the first one a repository query
// happens to return - see AttributeFilterValidator.validate() for the fix and full rationale.
@ExtendWith(MockitoExtension.class)
class AttributeFilterValidatorTests {

    @Mock
    private AttributeDefinitionRepository definitions;

    @Mock
    private AttributeOptionRepository options;

    private AttributeFilterValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AttributeFilterValidator(definitions, options);
    }

    @Test
    void acceptsAValueOnlyPresentUnderASiblingCategorysDefinitionForTheSameKey() {
        AttributeDefinition schoolTuitionSubject = definition(1L, "subject", "Subject");
        AttributeDefinition otherEducationSubject = definition(2L, "subject", "Subject");

        when(definitions.findByKeyInAndActiveTrueAndFilterableTrue(any()))
                .thenReturn(List.of(schoolTuitionSubject, otherEducationSubject));
        when(options.findByAttributeDefinitionIdInAndActiveTrue(any())).thenReturn(List.of(
                option(schoolTuitionSubject, "MATHEMATICS"),
                option(schoolTuitionSubject, "PHYSICS"),
                option(otherEducationSubject, "CHESS")));

        assertDoesNotThrow(() -> validator.validate(
                List.of(new AttributeFilterCriterion("subject", "CHESS", null, null))));
    }

    @Test
    void stillRejectsAValueNotPresentUnderAnySameKeyDefinition() {
        AttributeDefinition schoolTuitionSubject = definition(1L, "subject", "Subject");
        AttributeDefinition otherEducationSubject = definition(2L, "subject", "Subject");

        when(definitions.findByKeyInAndActiveTrueAndFilterableTrue(any()))
                .thenReturn(List.of(schoolTuitionSubject, otherEducationSubject));
        when(options.findByAttributeDefinitionIdInAndActiveTrue(any())).thenReturn(List.of(
                option(schoolTuitionSubject, "MATHEMATICS"),
                option(otherEducationSubject, "CHESS")));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(
                List.of(new AttributeFilterCriterion("subject", "NOT_A_REAL_SUBJECT", null, null))));
        assertEquals("Invalid subject value: NOT_A_REAL_SUBJECT", ex.getMessage());
    }

    @Test
    void rejectsAnAttributeKeyWithNoMatchingDefinitionAtAll() {
        when(definitions.findByKeyInAndActiveTrueAndFilterableTrue(any())).thenReturn(List.of());
        when(options.findByAttributeDefinitionIdInAndActiveTrue(any())).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> validator.validate(
                List.of(new AttributeFilterCriterion("notARealAttribute", "x", null, null))));
    }

    private AttributeDefinition definition(long id, String key, String name) {
        AttributeDefinition definition = new AttributeDefinition(
                null, key, name, AttributeDataType.SELECT, false, true, false, null, 0);
        setId(definition, id);
        return definition;
    }

    private AttributeOption option(AttributeDefinition definition, String value) {
        return new AttributeOption(definition, value, value, 0);
    }

    private void setId(AttributeDefinition definition, long id) {
        try {
            var field = AttributeDefinition.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(definition, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
