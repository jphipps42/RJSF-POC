package com.egs.rjsf.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record PublishVersionRequest(
        @NotNull Map<String, Object> json_schema,
        Map<String, Object> ui_schema,
        Map<String, Object> default_data,
        String change_notes,
        List<Map<String, Object>> migration_rules
) {}
