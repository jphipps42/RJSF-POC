package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SchemaVersionConfig(
        @JsonProperty("column") String column,
        @JsonProperty("sqlType") String sqlType
) {}
