package com.egs.rjsf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_catalog")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (isActive == null) isActive = true;
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
