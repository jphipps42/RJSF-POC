package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditColumnConfig(
        @JsonProperty("createdAt") String createdAt,
        @JsonProperty("updatedAt") String updatedAt,
        @JsonProperty("submittedBy") String submittedBy
) {}
