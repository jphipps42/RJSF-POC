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

@Entity
@Table(name = "form_pre_award_acquisition")
@Getter @Setter @NoArgsConstructor
public class FormPreAwardAcquisition extends FormDataBase {

    // D.1a Personnel
    @Column(name = "acq_personnel_qualifications")
    private String acqPersonnelQualifications;

    @Column(name = "acq_personnel_effort")
    private String acqPersonnelEffort;

    @Column(name = "acq_personnel_salary_cap")
    private String acqPersonnelSalaryCap;

    @Column(name = "acq_personnel_fringe_rate")
    private String acqPersonnelFringeRate;

    @Column(name = "acq_personnel_notes")
    private String acqPersonnelNotes;

    // D.1b Equipment
    @Column(name = "acq_equip_included")
    private String acqEquipIncluded;

    @Column(name = "acq_equip_necessary")
    private String acqEquipNecessary;

    @Column(name = "acq_equip_cost_appropriate")
    private String acqEquipCostAppropriate;

    @Column(name = "acq_equip_notes")
    private String acqEquipNotes;

    // D.1c Travel
    @Column(name = "acq_travel_included")
    private String acqTravelIncluded;

    @Column(name = "acq_travel_appropriate")
    private String acqTravelAppropriate;

    @Column(name = "acq_travel_notes")
    private String acqTravelNotes;

    // D.1d Materials
    @Column(name = "acq_materials_included")
    private String acqMaterialsIncluded;

    @Column(name = "acq_materials_appropriate")
    private String acqMaterialsAppropriate;

    @Column(name = "acq_materials_cost_appropriate")
    private String acqMaterialsCostAppropriate;

    @Column(name = "acq_materials_notes")
    private String acqMaterialsNotes;

    // D.1e Consultant
    @Column(name = "acq_consultant_included")
    private String acqConsultantIncluded;

    @Column(name = "acq_consultant_necessary")
    private String acqConsultantNecessary;

    @Column(name = "acq_consultant_duties_described")
    private String acqConsultantDutiesDescribed;

    @Column(name = "acq_consultant_costs_appropriate")
    private String acqConsultantCostsAppropriate;

    @Column(name = "acq_consultant_notes")
    private String acqConsultantNotes;

    // D.1f Third Party
    @Column(name = "acq_third_party_included")
    private String acqThirdPartyIncluded;

    @Column(name = "acq_third_party_value_added")
    private String acqThirdPartyValueAdded;

    @Column(name = "acq_third_party_work_described")
    private String acqThirdPartyWorkDescribed;

    @Column(name = "acq_third_party_budget_concerns")
    private String acqThirdPartyBudgetConcerns;

    @Column(name = "acq_third_party_notes")
    private String acqThirdPartyNotes;

    // D.1g Other Direct Costs
    @Column(name = "acq_other_direct_included")
    private String acqOtherDirectIncluded;

    @Column(name = "acq_other_direct_justified")
    private String acqOtherDirectJustified;

    @Column(name = "acq_other_direct_breakdown")
    private String acqOtherDirectBreakdown;

    @Column(name = "acq_other_direct_notes")
    private String acqOtherDirectNotes;

    // D.1h Additional Concerns
    @Column(name = "acq_additional_has_concerns")
    private String acqAdditionalHasConcerns;

    @Column(name = "acq_additional_notes")
    private String acqAdditionalNotes;

    // D.2 Peer Review
    @Column(name = "acq_peer_review_score")
    private BigDecimal acqPeerReviewScore;

    @Column(name = "acq_peer_review_outcome")
    private String acqPeerReviewOutcome;

    @Column(name = "acq_peer_comments")
    private String acqPeerComments;

    // D.3 SOW Concerns
    @Column(name = "acq_sow_comments")
    private String acqSowComments;

    // D.4 CPS
    @Column(name = "acq_cps_received")
    private String acqCpsReceived;

    @Column(name = "acq_cps_foreign_influence")
    private String acqCpsForeignInfluence;

    @Column(name = "acq_cps_overlap_identified")
    private String acqCpsOverlapIdentified;

    @Column(name = "acq_cps_comments")
    private String acqCpsComments;

    // D.5 IER
    @Column(name = "acq_ier_applicable")
    private String acqIerApplicable;

    @Column(name = "acq_ier_comment")
    private String acqIerComment;

    @Column(name = "acq_ier_plan_included")
    private String acqIerPlanIncluded;

    @Column(name = "acq_ier_plan_notes")
    private String acqIerPlanNotes;

    // D.6 Data Management
    @Column(name = "acq_dmp_received")
    private String acqDmpReceived;

    @Column(name = "acq_dmp_repository_identified")
    private String acqDmpRepositoryIdentified;

    @Column(name = "acq_dmp_sharing_timeline")
    private String acqDmpSharingTimeline;

    @Column(name = "acq_dmp_notes")
    private String acqDmpNotes;

    // D.7 Special Requirements
    @Type(JsonType.class)
    @Column(name = "acq_special_requirements", columnDefinition = "jsonb")
    private Object acqSpecialRequirements;

    @Column(name = "acq_special_notes")
    private String acqSpecialNotes;
}
