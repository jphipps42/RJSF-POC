package com.egs.rjsf.transformer.exception;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String formId) {
        super("Transformer template not found for formId: " + formId);
    }
}
