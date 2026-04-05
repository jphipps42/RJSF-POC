package com.egs.rjsf.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "schema_migrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"form_id", "from_version", "to_version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemaMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "from_version", nullable = false)
    private Integer fromVersion;

    @Column(name = "to_version", nullable = false)
    private Integer toVersion;

    @Type(JsonType.class)
    @Column(name = "migration_script", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> migrationScript = List.of();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
