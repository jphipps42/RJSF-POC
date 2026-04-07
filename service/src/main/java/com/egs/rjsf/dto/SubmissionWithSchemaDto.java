package com.egs.rjsf.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SubmissionWithSchemaDto(
        UUID id,
        UUID awardId,
        UUID formConfigId,
        String formKey,
        Map<String, Object> formData,
        String status,
        Map<String, Object> sectionStatus,
        OffsetDateTime submittedAt,
        OffsetDateTime completionDate,
        Boolean isLocked,
        UUID schemaVersionId,
        Integer schemaVersion,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String formTitle,
        Map<String, Object> jsonSchema,
        Map<String, Object> uiSchema,
        Integer currentVersion
) {}
