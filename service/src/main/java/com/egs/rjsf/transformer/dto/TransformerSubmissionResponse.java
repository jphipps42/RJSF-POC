package com.egs.rjsf.transformer.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record TransformerSubmissionResponse(
        Long submissionId,
        String formId,
        Integer version,
        OffsetDateTime createdAt,
        Map<String, Object> formData
) {}
