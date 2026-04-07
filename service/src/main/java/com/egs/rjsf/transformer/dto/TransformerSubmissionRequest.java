package com.egs.rjsf.transformer.dto;

import java.util.Map;

public record TransformerSubmissionRequest(
        String formId,
        Integer schemaVersion,
        String submittedBy,
        Map<String, Object> formData
) {}
