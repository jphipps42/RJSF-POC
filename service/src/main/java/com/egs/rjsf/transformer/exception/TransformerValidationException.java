package com.egs.rjsf.transformer.exception;

import com.egs.rjsf.transformer.validation.Violation;

import java.util.List;

public class TransformerValidationException extends RuntimeException {
    private final List<Violation> violations;

    public TransformerValidationException(List<Violation> violations) {
        super("Validation failed with " + violations.size() + " violation(s)");
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
