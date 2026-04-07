package com.egs.rjsf.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "form_submissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"award_id", "form_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "award_id", nullable = false)
    private UUID awardId;

    @Column(name = "form_config_id", nullable = false)
    private UUID formConfigId;

    @Column(name = "form_key", nullable = false, length = 100)
    private String formKey;

    @Type(JsonType.class)
    @Column(name = "form_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> formData = Map.of();

    @Builder.Default
    @Column(length = 50, nullable = false)
    private String status = "not_started";

    @Type(JsonType.class)
    @Column(name = "section_status", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> sectionStatus = Map.of();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "completion_date")
    private OffsetDateTime completionDate;

    @Builder.Default
    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "schema_version_id")
    private UUID schemaVersionId;

    @Column(name = "schema_version")
    private Integer schemaVersion;

    @Column(columnDefinition = "text")
    private String notes;

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
