package com.egs.rjsf.dto;

import java.util.Map;

public record MigrationResultDto(
    Map<String, Object> formData,
    Map<String, Object> json_schema,
    Map<String, Object> ui_schema,
    Integer schemaVersion,
    boolean migrated
) {}
