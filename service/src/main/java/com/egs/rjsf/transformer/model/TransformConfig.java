package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransformConfig(
        @JsonProperty("toDb") String toDb,
        @JsonProperty("fromDb") String fromDb
) {}
