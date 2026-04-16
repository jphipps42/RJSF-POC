package com.egs.rjsf.entity.formdata;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "form_pre_award_overview")
@Getter @Setter @NoArgsConstructor
public class FormPreAwardOverview extends FormDataBase {

    @Column(name = "pi_budget")
    private BigDecimal piBudget;

    @Column(name = "final_recommended_budget")
    private BigDecimal finalRecommendedBudget;

    @Column(name = "funding_source")
    private String fundingSource;

    @Column(name = "negotiation_status")
    private String negotiationStatus;

    @Column(name = "program_manager")
    private String programManager;

    @Column(name = "co_principal_investigator")
    private String coPrincipalInvestigator;

    @Column(name = "contract_grants_specialist")
    private String contractGrantsSpecialist;

    @Column(name = "branch_chief")
    private String branchChief;

    @Column(name = "prime_award_type")
    private String primeAwardType;

    @Column(name = "pi_notification_date")
    private LocalDate piNotificationDate;

    @Type(JsonType.class)
    @Column(name = "personnel", columnDefinition = "jsonb")
    private Object personnel;

    @Column(name = "overview_notes")
    private String overviewNotes;
}
