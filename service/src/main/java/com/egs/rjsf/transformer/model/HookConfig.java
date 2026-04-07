package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record HookConfig(
        List<String> preValidation,
        List<String> preInsert,
        List<String> postInsert,
        List<String> postQuery,
        List<String> postAssemble
) {
    @JsonCreator
    public HookConfig(
            @JsonProperty("preValidation") List<String> preValidation,
            @JsonProperty("preInsert") List<String> preInsert,
            @JsonProperty("postInsert") List<String> postInsert,
            @JsonProperty("postQuery") List<String> postQuery,
            @JsonProperty("postAssemble") List<String> postAssemble
    ) {
        this.preValidation = preValidation != null ? preValidation : List.of();
        this.preInsert = preInsert != null ? preInsert : List.of();
        this.postInsert = postInsert != null ? postInsert : List.of();
        this.postQuery = postQuery != null ? postQuery : List.of();
        this.postAssemble = postAssemble != null ? postAssemble : List.of();
    }
}
