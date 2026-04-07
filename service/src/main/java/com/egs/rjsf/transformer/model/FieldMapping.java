package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FieldMapping(
        String jsonPath,
        String column,
        String sqlType,
        boolean nullable,
        Object defaultValue,
        TransformConfig transform,
        Map<String, Object> validation,
        List<String> tags
) {
    @JsonCreator
    public FieldMapping(
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("column") String column,
            @JsonProperty("sqlType") String sqlType,
            @JsonProperty("nullable") Boolean nullable,
            @JsonProperty("defaultValue") Object defaultValue,
            @JsonProperty("transform") TransformConfig transform,
            @JsonProperty("validation") Map<String, Object> validation,
            @JsonProperty("tags") List<String> tags
    ) {
        this(
                jsonPath,
                column,
                sqlType,
                nullable != null ? nullable : true,
                defaultValue,
                transform,
                validation,
                tags != null ? tags : List.of()
        );
    }
}
