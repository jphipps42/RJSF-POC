package com.egs.rjsf.transformer.validation;

import com.egs.rjsf.transformer.engine.PathResolver;
import com.egs.rjsf.transformer.exception.TransformerValidationException;
import com.egs.rjsf.transformer.model.FieldMapping;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ValidationGate {

    private final PathResolver pathResolver;

    public ValidationGate(PathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    public void validate(Map<String, Object> formData, List<FieldMapping> fieldMappings) {
        List<Violation> violations = new ArrayList<>();

        for (FieldMapping field : fieldMappings) {
            Optional<Object> value = pathResolver.get(field.jsonPath(), formData);

            // Check required fields (nullable = false)
            if (!field.nullable()) {
                if (value.isEmpty() || value.get() == null) {
                    violations.add(new Violation(field.jsonPath(), "Required field is missing"));
                    continue;
                }
            }

            // Check string type fields that have a value
            if (value.isPresent() && value.get() != null && field.validation() != null) {
                Object val = value.get();
                Map<String, Object> validation = field.validation();

                // Check enum constraint
                if (validation.containsKey("enum") && validation.get("enum") instanceof List<?> allowedValues) {
                    if (!allowedValues.contains(val)) {
                        violations.add(new Violation(field.jsonPath(),
                                "Value '" + val + "' is not in allowed values: " + allowedValues));
                    }
                }

                // Check string format (basic checks)
                if ("string".equals(validation.get("type")) && val instanceof String strVal) {
                    String format = (String) validation.get("format");
                    if ("email".equals(format) && !strVal.contains("@")) {
                        violations.add(new Violation(field.jsonPath(), "must match format 'email'"));
                    }
                    if ("date".equals(format)) {
                        try {
                            java.time.LocalDate.parse(strVal);
                        } catch (Exception e) {
                            violations.add(new Violation(field.jsonPath(), "must be a valid date string"));
                        }
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new TransformerValidationException(violations);
        }
    }
}
