package com.egs.rjsf.service.sync;

import com.egs.rjsf.entity.formdata.*;
import com.egs.rjsf.repository.formdata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.egs.rjsf.service.sync.FormDataExtractor.*;

/**
 * JPA/POJO-based strategy for syncing form data to relational tables.
 * Uses typed entity classes and Spring Data repositories instead of
 * the dynamic Map-based transformer pipeline.
 */
@Component("pojoSyncStrategy")
public class PojoSyncStrategy implements RelationalSyncStrategy {

    private static final Logger log = LoggerFactory.getLogger(PojoSyncStrategy.class);
    private static final int SCHEMA_VERSION = 1;

    private final FormPreAwardOverviewRepository overviewRepo;
    private final FormPreAwardSafetyRepository safetyRepo;
    private final FormPreAwardAnimalRepository animalRepo;
    private final FormPreAwardHumanRepository humanRepo;
    private final FormPreAwardAcquisitionRepository acquisitionRepo;
    private final FormPreAwardFinalRepository finalRepo;

    public PojoSyncStrategy(FormPreAwardOverviewRepository overviewRepo,
                            FormPreAwardSafetyRepository safetyRepo,
                            FormPreAwardAnimalRepository animalRepo,
                            FormPreAwardHumanRepository humanRepo,
                            FormPreAwardAcquisitionRepository acquisitionRepo,
                            FormPreAwardFinalRepository finalRepo) {
        this.overviewRepo = overviewRepo;
        this.safetyRepo = safetyRepo;
        this.animalRepo = animalRepo;
        this.humanRepo = humanRepo;
        this.acquisitionRepo = acquisitionRepo;
        this.finalRepo = finalRepo;
    }

    @Override
    public String getName() {
        return "POJO";
    }

    @Override
    @Transactional
    public void writeSection(String formId, UUID awardId, Map<String, Object> formData,
                             String sectionId, String submittedBy) {
        switch (formId) {
            case "pre-award-overview" -> upsertOverview(awardId, formData, submittedBy);
            case "pre-award-safety" -> upsertSafety(awardId, formData, submittedBy);
            case "pre-award-animal" -> upsertAnimal(awardId, formData, submittedBy);
            case "pre-award-human" -> upsertHuman(awardId, formData, sectionId, submittedBy);
            case "pre-award-acquisition" -> upsertAcquisition(awardId, formData, sectionId, submittedBy);
            case "pre-award-final" -> upsertFinal(awardId, formData, submittedBy);
            default -> log.warn("POJO strategy: unknown formId '{}'", formId);
        }
    }

    // ---- Overview (single section, all fields) ----

    private void upsertOverview(UUID awardId, Map<String, Object> formData, String submittedBy) {
        FormPreAwardOverview entity = overviewRepo.findByAwardId(awardId)
                .orElseGet(FormPreAwardOverview::new);
        initBase(entity, awardId, submittedBy);

        entity.setPiBudget(getBigDecimal(formData, "pi_budget"));
        entity.setFinalRecommendedBudget(getBigDecimal(formData, "final_recommended_budget"));
        entity.setFundingSource(getString(formData, "funding_source"));
        entity.setNegotiationStatus(getString(formData, "negotiation_status"));
        entity.setProgramManager(getString(formData, "program_manager"));
        entity.setCoPrincipalInvestigator(getString(formData, "co_principal_investigator"));
        entity.setContractGrantsSpecialist(getString(formData, "contract_grants_specialist"));
        entity.setBranchChief(getString(formData, "branch_chief"));
        entity.setPrimeAwardType(getString(formData, "prime_award_type"));
        entity.setPiNotificationDate(getLocalDate(formData, "pi_notification_date"));
        entity.setPersonnel(getJsonb(formData, "personnel"));
        entity.setOverviewNotes(getString(formData, "overview_notes"));

        overviewRepo.save(entity);
        log.debug("POJO upserted overview for award {}", awardId);
    }

    // ---- Safety (single section, all fields) ----

    private void upsertSafety(UUID awardId, Map<String, Object> formData, String submittedBy) {
        FormPreAwardSafety entity = safetyRepo.findByAwardId(awardId)
                .orElseGet(FormPreAwardSafety::new);
        initBase(entity, awardId, submittedBy);

        entity.setSafetyQ1(getString(formData, "safety_q1"));
        entity.setProgrammaticRec(getString(formData, "programmatic_rec"));
        entity.setSafetyQ2(getString(formData, "safety_q2"));
        entity.setSafetyQ3(getString(formData, "safety_q3"));
        entity.setSafetyQ4(getString(formData, "safety_q4"));
        entity.setSafetyQ5(getString(formData, "safety_q5"));
        entity.setSafetyQ6(getString(formData, "safety_q6"));
        entity.setSafetyQ7(getString(formData, "safety_q7"));
        entity.setSafetyQ8(getString(formData, "safety_q8"));
        entity.setSafetyNotes(getString(formData, "safety_notes"));

        safetyRepo.save(entity);
        log.debug("POJO upserted safety for award {}", awardId);
    }

    // ---- Animal (single section, all fields) ----

    private void upsertAnimal(UUID awardId, Map<String, Object> formData, String submittedBy) {
        FormPreAwardAnimal entity = animalRepo.findByAwardId(awardId)
                .orElseGet(FormPreAwardAnimal::new);
        initBase(entity, awardId, submittedBy);

        entity.setAnimalQ1(getString(formData, "animal_q1"));
        entity.setAnimalSpecies(getJsonb(formData, "animal_species"));
        entity.setAnimalQ2(getString(formData, "animal_q2"));
        entity.setAnimalQ3(getString(formData, "animal_q3"));
        entity.setAnimalQ4(getString(formData, "animal_q4"));
        entity.setIacucProtocolNumber(getString(formData, "iacuc_protocol_number"));
        entity.setAnimalQ5(getString(formData, "animal_q5"));
        entity.setAnimalStartDate(getString(formData, "animal_start_date"));
        entity.setAnimalNotes(getString(formData, "animal_notes"));

        animalRepo.save(entity);
        log.debug("POJO upserted animal for award {}", awardId);
    }

    // ---- Human (multi-section: only set fields for the active sectionId) ----

    private void upsertHuman(UUID awardId, Map<String, Object> formData,
                             String sectionId, String submittedBy) {
        FormPreAwardHuman entity = humanRepo.findByAwardId(awardId)
                .orElseGet(FormPreAwardHuman::new);
        initBase(entity, awardId, submittedBy);

        switch (sectionId) {
            case "human_no_regulatory" -> setHumanNoRegulatoryFields(entity, formData);
            case "human_anatomical" -> setHumanAnatomicalFields(entity, formData);
            case "human_data_secondary" -> setHumanDataSecondaryFields(entity, formData);
            case "human_subjects" -> setHumanSubjectsFields(entity, formData);
            case "human_special_topics" -> setHumanSpecialTopicsFields(entity, formData);
            case "human_estimated_start" -> setHumanEstimatedStartFields(entity, formData);
        }

        humanRepo.save(entity);
        log.debug("POJO upserted human/{} for award {}", sectionId, awardId);
    }

    private void setHumanNoRegulatoryFields(FormPreAwardHuman e, Map<String, Object> d) {
        e.setNoReviewDefaultNo(getBoolean(d, "no_review_default_no"));
        e.setHumanS1Q1(getString(d, "human_s1_q1"));
        e.setHumanS1Q2(getString(d, "human_s1_q2"));
        e.setHumanS1Q3(getString(d, "human_s1_q3"));
        e.setHumanS1Q4(getString(d, "human_s1_q4"));
        e.setHumanS1Q5(getString(d, "human_s1_q5"));
        e.setHumanS1Notes(getString(d, "human_s1_notes"));
    }

    private void setHumanAnatomicalFields(FormPreAwardHuman e, Map<String, Object> d) {
        e.setHasDefaultNo(getBoolean(d, "has_default_no"));
        e.setHumanHasQ1(getString(d, "human_has_q1"));
        e.setHumanHasQ2(getString(d, "human_has_q2"));
        e.setHumanHasQ3(getString(d, "human_has_q3"));
        e.setHumanHasQ4(getString(d, "human_has_q4"));
        e.setHumanHasQ5(getString(d, "human_has_q5"));
        e.setHumanHasQ6(getString(d, "human_has_q6"));
        e.setHumanHasQ7(getString(d, "human_has_q7"));
        e.setHumanHasNotes(getString(d, "human_has_notes"));
    }

    private void setHumanDataSecondaryFields(FormPreAwardHuman e, Map<String, Object> d) {
        e.setHumanDsQ1(getString(d, "human_ds_q1"));
        e.setHumanDsNotes(getString(d, "human_ds_notes"));
    }

    private void setHumanSubjectsFields(FormPreAwardHuman e, Map<String, Object> d) {
        e.setHumanHsQ1(getString(d, "human_hs_q1"));
        e.setHumanHsQ2(getString(d, "human_hs_q2"));
        e.setCtFdaQ1(getString(d, "ct_fda_q1"));
        e.setCtNonusQ1(getString(d, "ct_nonus_q1"));
        e.setHumanHsNotes(getString(d, "human_hs_notes"));
    }

    private void setHumanSpecialTopicsFields(FormPreAwardHuman e, Map<String, Object> d) {
        e.setHumanOstQ1(getString(d, "human_ost_q1"));
        e.setHumanOstNotes(getString(d, "human_ost_notes"));
    }

    private void setHumanEstimatedStartFields(FormPreAwardHuman e, Map<String, Object> d) {
        e.setEstimatedStartDate(getString(d, "estimated_start_date"));
    }

    // ---- Acquisition (multi-section: only set fields for the active sectionId) ----

    private void upsertAcquisition(UUID awardId, Map<String, Object> formData,
                                   String sectionId, String submittedBy) {
        FormPreAwardAcquisition entity = acquisitionRepo.findByAwardId(awardId)
                .orElseGet(FormPreAwardAcquisition::new);
        initBase(entity, awardId, submittedBy);

        switch (sectionId) {
            case "acq_br_personnel" -> setAcqPersonnelFields(entity, formData);
            case "acq_br_equipment" -> setAcqEquipmentFields(entity, formData);
            case "acq_br_travel" -> setAcqTravelFields(entity, formData);
            case "acq_br_materials" -> setAcqMaterialsFields(entity, formData);
            case "acq_br_consultant" -> setAcqConsultantFields(entity, formData);
            case "acq_br_third_party" -> setAcqThirdPartyFields(entity, formData);
            case "acq_br_other_direct" -> setAcqOtherDirectFields(entity, formData);
            case "acq_br_additional" -> setAcqAdditionalFields(entity, formData);
            case "acq_peer_review" -> setAcqPeerReviewFields(entity, formData);
            case "acq_sow_concerns" -> setAcqSowFields(entity, formData);
            case "acq_cps" -> setAcqCpsFields(entity, formData);
            case "acq_ier" -> setAcqIerFields(entity, formData);
            case "acq_data_management" -> setAcqDataManagementFields(entity, formData);
            case "acq_special_requirements" -> setAcqSpecialRequirementsFields(entity, formData);
        }

        acquisitionRepo.save(entity);
        log.debug("POJO upserted acquisition/{} for award {}", sectionId, awardId);
    }

    private void setAcqPersonnelFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqPersonnelQualifications(getString(d, "acq_personnel_qualifications"));
        e.setAcqPersonnelEffort(getString(d, "acq_personnel_effort"));
        e.setAcqPersonnelSalaryCap(getString(d, "acq_personnel_salary_cap"));
        e.setAcqPersonnelFringeRate(getString(d, "acq_personnel_fringe_rate"));
        e.setAcqPersonnelNotes(getString(d, "acq_personnel_notes"));
    }

    private void setAcqEquipmentFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqEquipIncluded(getString(d, "acq_equip_included"));
        e.setAcqEquipNecessary(getString(d, "acq_equip_necessary"));
        e.setAcqEquipCostAppropriate(getString(d, "acq_equip_cost_appropriate"));
        e.setAcqEquipNotes(getString(d, "acq_equip_notes"));
    }

    private void setAcqTravelFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqTravelIncluded(getString(d, "acq_travel_included"));
        e.setAcqTravelAppropriate(getString(d, "acq_travel_appropriate"));
        e.setAcqTravelNotes(getString(d, "acq_travel_notes"));
    }

    private void setAcqMaterialsFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqMaterialsIncluded(getString(d, "acq_materials_included"));
        e.setAcqMaterialsAppropriate(getString(d, "acq_materials_appropriate"));
        e.setAcqMaterialsCostAppropriate(getString(d, "acq_materials_cost_appropriate"));
        e.setAcqMaterialsNotes(getString(d, "acq_materials_notes"));
    }

    private void setAcqConsultantFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqConsultantIncluded(getString(d, "acq_consultant_included"));
        e.setAcqConsultantNecessary(getString(d, "acq_consultant_necessary"));
        e.setAcqConsultantDutiesDescribed(getString(d, "acq_consultant_duties_described"));
        e.setAcqConsultantCostsAppropriate(getString(d, "acq_consultant_costs_appropriate"));
        e.setAcqConsultantNotes(getString(d, "acq_consultant_notes"));
    }

    private void setAcqThirdPartyFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqThirdPartyIncluded(getString(d, "acq_third_party_included"));
        e.setAcqThirdPartyValueAdded(getString(d, "acq_third_party_value_added"));
        e.setAcqThirdPartyWorkDescribed(getString(d, "acq_third_party_work_described"));
        e.setAcqThirdPartyBudgetConcerns(getString(d, "acq_third_party_budget_concerns"));
        e.setAcqThirdPartyNotes(getString(d, "acq_third_party_notes"));
    }

    private void setAcqOtherDirectFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqOtherDirectIncluded(getString(d, "acq_other_direct_included"));
        e.setAcqOtherDirectJustified(getString(d, "acq_other_direct_justified"));
        e.setAcqOtherDirectBreakdown(getString(d, "acq_other_direct_breakdown"));
        e.setAcqOtherDirectNotes(getString(d, "acq_other_direct_notes"));
    }

    private void setAcqAdditionalFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqAdditionalHasConcerns(getString(d, "acq_additional_has_concerns"));
        e.setAcqAdditionalNotes(getString(d, "acq_additional_notes"));
    }

    private void setAcqPeerReviewFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqPeerReviewScore(getBigDecimal(d, "acq_peer_review_score"));
        e.setAcqPeerReviewOutcome(getString(d, "acq_peer_review_outcome"));
        e.setAcqPeerComments(getString(d, "acq_peer_comments"));
    }

    private void setAcqSowFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqSowComments(getString(d, "acq_sow_comments"));
    }

    private void setAcqCpsFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqCpsReceived(getString(d, "acq_cps_received"));
        e.setAcqCpsForeignInfluence(getString(d, "acq_cps_foreign_influence"));
        e.setAcqCpsOverlapIdentified(getString(d, "acq_cps_overlap_identified"));
        e.setAcqCpsComments(getString(d, "acq_cps_comments"));
    }

    private void setAcqIerFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqIerApplicable(getString(d, "acq_ier_applicable"));
        e.setAcqIerComment(getString(d, "acq_ier_comment"));
        e.setAcqIerPlanIncluded(getString(d, "acq_ier_plan_included"));
        e.setAcqIerPlanNotes(getString(d, "acq_ier_plan_notes"));
    }

    private void setAcqDataManagementFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqDmpReceived(getString(d, "acq_dmp_received"));
        e.setAcqDmpRepositoryIdentified(getString(d, "acq_dmp_repository_identified"));
        e.setAcqDmpSharingTimeline(getString(d, "acq_dmp_sharing_timeline"));
        e.setAcqDmpNotes(getString(d, "acq_dmp_notes"));
    }

    private void setAcqSpecialRequirementsFields(FormPreAwardAcquisition e, Map<String, Object> d) {
        e.setAcqSpecialRequirements(getJsonb(d, "acq_special_requirements"));
        e.setAcqSpecialNotes(getString(d, "acq_special_notes"));
    }

    // ---- Final Recommendation (single section, all fields) ----

    private void upsertFinal(UUID awardId, Map<String, Object> formData, String submittedBy) {
        FormPreAwardFinal entity = finalRepo.findByAwardId(awardId)
                .orElseGet(FormPreAwardFinal::new);
        initBase(entity, awardId, submittedBy);

        entity.setScientificOverlap(getString(formData, "scientific_overlap"));
        entity.setForeignInvolvement(getString(formData, "foreign_involvement"));
        entity.setRisgApproval(getString(formData, "risg_approval"));
        entity.setSoRecommendation(getString(formData, "so_recommendation"));
        entity.setSoComments(getString(formData, "so_comments"));
        entity.setGorRecommendation(getString(formData, "gor_recommendation"));
        entity.setGorComments(getString(formData, "gor_comments"));

        finalRepo.save(entity);
        log.debug("POJO upserted final for award {}", awardId);
    }

    // ---- Shared ----

    private void initBase(FormDataBase entity, UUID awardId, String submittedBy) {
        entity.setAwardId(awardId);
        entity.setSchemaVersion(SCHEMA_VERSION);
        if (submittedBy != null) {
            entity.setSubmittedBy(submittedBy);
        }
    }
}
