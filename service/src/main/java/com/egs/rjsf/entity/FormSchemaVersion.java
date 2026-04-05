package com.egs.rjsf.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "form_schema_versions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"form_id", "version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormSchemaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(nullable = false)
    private Integer version;

    @Type(JsonType.class)
    @Column(name = "json_schema", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> jsonSchema;

    @Type(JsonType.class)
    @Column(name = "ui_schema", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> uiSchema = Map.of();

    @Type(JsonType.class)
    @Column(name = "default_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> defaultData = Map.of();

    @Column(name = "change_notes", columnDefinition = "text")
    private String changeNotes;

    @Builder.Default
    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
