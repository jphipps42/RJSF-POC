package com.egs.rjsf.transformer.registry;

import com.egs.rjsf.transformer.model.FieldMapping;
import com.egs.rjsf.transformer.model.RelationMapping;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TemplateValidator {

    private static final Logger log = LoggerFactory.getLogger(TemplateValidator.class);

    public void validate(TransformerTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("TransformerTemplate must not be null");
        }

        if (isBlank(template.formId())) {
            throw new IllegalArgumentException("TransformerTemplate formId must not be blank");
        }

        if (template.version() <= 0) {
            throw new IllegalArgumentException(
                    "TransformerTemplate version must be > 0, got: " + template.version()
                    + " (formId: " + template.formId() + ")");
        }

        if (isBlank(template.tableName())) {
            throw new IllegalArgumentException(
                    "TransformerTemplate tableName must not be blank (formId: " + template.formId() + ")");
        }

        if (template.fields() == null || template.fields().isEmpty()) {
            throw new IllegalArgumentException(
                    "TransformerTemplate must have at least one field mapping (formId: " + template.formId() + ")");
        }

        for (int i = 0; i < template.fields().size(); i++) {
            FieldMapping field = template.fields().get(i);
            validateField(template.formId(), field, i);
        }

        if (template.relations() != null) {
            for (int i = 0; i < template.relations().size(); i++) {
                RelationMapping relation = template.relations().get(i);
                validateRelation(template.formId(), relation, i);
            }
        }

        log.debug("Validation passed for template: {}", template.formId());
    }

    private void validateField(String formId, FieldMapping field, int index) {
        if (isBlank(field.jsonPath())) {
            throw new IllegalArgumentException(
                    "Field[" + index + "].jsonPath must not be blank (formId: " + formId + ")");
        }
        if (isBlank(field.column())) {
            throw new IllegalArgumentException(
                    "Field[" + index + "].column must not be blank (formId: " + formId + ")");
        }
        if (isBlank(field.sqlType())) {
            throw new IllegalArgumentException(
                    "Field[" + index + "].sqlType must not be blank (formId: " + formId + ")");
        }
    }

    private void validateRelation(String formId, RelationMapping relation, int index) {
        if (isBlank(relation.childTable())) {
            log.warn("Relation[{}].childTable is blank (formId: {})", index, formId);
            throw new IllegalArgumentException(
                    "Relation[" + index + "].childTable must not be blank (formId: " + formId + ")");
        }
        if (isBlank(relation.parentKey())) {
            log.warn("Relation[{}].parentKey is blank (formId: {})", index, formId);
            throw new IllegalArgumentException(
                    "Relation[" + index + "].parentKey must not be blank (formId: " + formId + ")");
        }
        if (relation.fields() == null || relation.fields().isEmpty()) {
            log.warn("Relation[{}].fields is null or empty (formId: {})", index, formId);
            throw new IllegalArgumentException(
                    "Relation[" + index + "].fields must not be empty (formId: " + formId + ")");
        }

        for (int i = 0; i < relation.fields().size(); i++) {
            validateField(formId, relation.fields().get(i), i);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
