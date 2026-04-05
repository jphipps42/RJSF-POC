package com.egs.rjsf.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "form_configurations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "form_key", unique = true, nullable = false, length = 100)
    private String formKey;

    @Column(nullable = false)
    private String title;

    private String description;

    @Type(JsonType.class)
    @Column(name = "json_schema", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> jsonSchema;

    @Type(JsonType.class)
    @Column(name = "ui_schema", columnDefinition = "jsonb")
    private Map<String, Object> uiSchema;

    @Type(JsonType.class)
    @Column(name = "default_data", columnDefinition = "jsonb")
    private Map<String, Object> defaultData;

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
