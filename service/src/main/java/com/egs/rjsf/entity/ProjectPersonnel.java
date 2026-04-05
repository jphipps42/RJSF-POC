package com.egs.rjsf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_personnel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectPersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "award_id", nullable = false)
    private UUID awardId;

    @Column(nullable = false)
    private String organization;

    @Builder.Default
    @Column(length = 100)
    private String country = "USA";

    @Column(name = "project_role", nullable = false, length = 100)
    private String projectRole;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "is_subcontract")
    private Boolean isSubcontract = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
