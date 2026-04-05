package com.egs.rjsf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "award_linked_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AwardLinkedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "award_id", nullable = false)
    private UUID awardId;

    @Column(nullable = false, length = 100)
    private String section;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
        lastUpdated = OffsetDateTime.now();
    }
}
