package com.egs.rjsf.transformer.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "transformer_template_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"form_id", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private String formId;

    @Column(nullable = false)
    private Integer version;

    @Type(JsonType.class)
    @Column(name = "template_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> templateJson;

    @Column(name = "loaded_at")
    private OffsetDateTime loadedAt;

    @PrePersist
    void prePersist() {
        loadedAt = OffsetDateTime.now();
    }
}
