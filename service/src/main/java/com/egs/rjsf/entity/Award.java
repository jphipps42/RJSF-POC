package com.egs.rjsf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "awards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Award {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "log_number", unique = true, nullable = false, length = 50)
    private String logNumber;

    @Column(name = "award_number", length = 50)
    private String awardNumber;

    @Column(name = "award_mechanism", columnDefinition = "text")
    private String awardMechanism;

    @Column(name = "funding_opportunity", columnDefinition = "text")
    private String fundingOpportunity;

    @Column(name = "principal_investigator")
    private String principalInvestigator;

    @Column(name = "performing_organization", columnDefinition = "text")
    private String performingOrganization;

    @Column(name = "contracting_organization", columnDefinition = "text")
    private String contractingOrganization;

    @Column(name = "period_of_performance", length = 100)
    private String periodOfPerformance;

    @Column(name = "award_amount", precision = 15, scale = 2)
    private BigDecimal awardAmount;

    @Column(name = "program_office", columnDefinition = "text")
    private String programOffice;

    private String program;

    @Column(name = "science_officer")
    private String scienceOfficer;

    @Column(name = "gor_cor")
    private String gorCor;

    @Column(name = "pi_budget", precision = 15, scale = 2)
    private BigDecimal piBudget;

    @Column(name = "final_recommended_budget", precision = 15, scale = 2)
    private BigDecimal finalRecommendedBudget;

    @Column(name = "program_manager")
    private String programManager;

    @Column(name = "contract_grants_specialist")
    private String contractGrantsSpecialist;

    @Column(name = "branch_chief")
    private String branchChief;

    @Builder.Default
    @Column(name = "prime_award_type", length = 50)
    private String primeAwardType = "extramural";

    @Builder.Default
    @Column(length = 50)
    private String status = "under_negotiation";

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
