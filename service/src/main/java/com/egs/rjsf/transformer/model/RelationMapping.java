package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RelationMapping(
        @JsonProperty("jsonPath") String jsonPath,
        @JsonProperty("childTable") String childTable,
        @JsonProperty("parentKey") String parentKey,
        @JsonProperty("orderColumn") String orderColumn,
        @JsonProperty("fields") List<FieldMapping> fields
) {}
