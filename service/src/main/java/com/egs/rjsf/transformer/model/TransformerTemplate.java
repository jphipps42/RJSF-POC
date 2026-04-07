package com.egs.rjsf.transformer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TransformerTemplate(
        String formId,
        int version,
        String tableName,
        String description,
        SchemaVersionConfig schemaVersion,
        AuditColumnConfig auditColumns,
        List<FieldMapping> fields,
        List<RelationMapping> relations,
        HookConfig writeHooks,
        HookConfig readHooks
) {
    @JsonCreator
    public TransformerTemplate(
            @JsonProperty("formId") String formId,
            @JsonProperty("version") int version,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("description") String description,
            @JsonProperty("schemaVersion") SchemaVersionConfig schemaVersion,
            @JsonProperty("auditColumns") AuditColumnConfig auditColumns,
            @JsonProperty("fields") List<FieldMapping> fields,
            @JsonProperty("relations") List<RelationMapping> relations,
            @JsonProperty("writeHooks") HookConfig writeHooks,
            @JsonProperty("readHooks") HookConfig readHooks
    ) {
        this.formId = formId;
        this.version = version;
        this.tableName = tableName;
        this.description = description;
        this.schemaVersion = schemaVersion;
        this.auditColumns = auditColumns;
        this.fields = fields != null ? fields : List.of();
        this.relations = relations != null ? relations : List.of();
        this.writeHooks = writeHooks != null ? writeHooks : new HookConfig(null, null, null, null, null);
        this.readHooks = readHooks != null ? readHooks : new HookConfig(null, null, null, null, null);
    }
}
