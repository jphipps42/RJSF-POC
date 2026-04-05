package com.egs.rjsf.service;

import com.egs.rjsf.dto.AwardDetailDto;
import com.egs.rjsf.dto.SubmissionWithSchemaDto;
import com.egs.rjsf.entity.*;
import com.egs.rjsf.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AwardService {

    private final AwardRepository awardRepository;
    private final FormConfigurationRepository formConfigurationRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final FormSchemaVersionRepository formSchemaVersionRepository;
    private final ProjectPersonnelRepository projectPersonnelRepository;
    private final AwardLinkedFileRepository awardLinkedFileRepository;

    public AwardService(AwardRepository awardRepository,
                        FormConfigurationRepository formConfigurationRepository,
                        FormSubmissionRepository formSubmissionRepository,
                        FormSchemaVersionRepository formSchemaVersionRepository,
                        ProjectPersonnelRepository projectPersonnelRepository,
                        AwardLinkedFileRepository awardLinkedFileRepository) {
        this.awardRepository = awardRepository;
        this.formConfigurationRepository = formConfigurationRepository;
        this.formSubmissionRepository = formSubmissionRepository;
        this.formSchemaVersionRepository = formSchemaVersionRepository;
        this.projectPersonnelRepository = projectPersonnelRepository;
        this.awardLinkedFileRepository = awardLinkedFileRepository;
    }

    public List<Award> findAll() {
        return awardRepository.findAllByOrderByCreatedAtDesc();
    }

    public Award findById(UUID id) {
        return awardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Award not found: " + id));
    }

    public Award findByLogNumber(String logNumber) {
        return awardRepository.findByLogNumber(logNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Award not found for logNumber: " + logNumber));
    }

    public AwardDetailDto findByIdWithDetails(UUID id) {
        Award award = findById(id);
        return buildDetailDto(award);
    }

    public AwardDetailDto findByLogNumberWithDetails(String logNumber) {
        Award award = findByLogNumber(logNumber);
        return buildDetailDto(award);
    }

    private AwardDetailDto buildDetailDto(Award award) {
        List<FormSubmission> submissions = formSubmissionRepository
                .findByAwardIdOrderByFormKey(award.getId());
        List<SubmissionWithSchemaDto> submissionDtos = buildSubmissionDtos(submissions);
        List<ProjectPersonnel> personnel = projectPersonnelRepository
                .findByAwardIdOrderByName(award.getId());
        List<AwardLinkedFile> linkedFiles = awardLinkedFileRepository
                .findByAwardIdOrderBySectionAscCreatedAtDesc(award.getId());

        AwardDetailDto dto = new AwardDetailDto();
        dto.setId(award.getId());
        dto.setLogNumber(award.getLogNumber());
        dto.setAwardNumber(award.getAwardNumber());
        dto.setAwardMechanism(award.getAwardMechanism());
        dto.setFundingOpportunity(award.getFundingOpportunity());
        dto.setPrincipalInvestigator(award.getPrincipalInvestigator());
        dto.setPerformingOrganization(award.getPerformingOrganization());
        dto.setContractingOrganization(award.getContractingOrganization());
        dto.setPeriodOfPerformance(award.getPeriodOfPerformance());
        dto.setAwardAmount(award.getAwardAmount());
        dto.setProgramOffice(award.getProgramOffice());
        dto.setProgram(award.getProgram());
        dto.setScienceOfficer(award.getScienceOfficer());
        dto.setGorCor(award.getGorCor());
        dto.setPiBudget(award.getPiBudget());
        dto.setFinalRecommendedBudget(award.getFinalRecommendedBudget());
        dto.setProgramManager(award.getProgramManager());
        dto.setContractGrantsSpecialist(award.getContractGrantsSpecialist());
        dto.setBranchChief(award.getBranchChief());
        dto.setPrimeAwardType(award.getPrimeAwardType());
        dto.setStatus(award.getStatus());
        dto.setCreatedAt(award.getCreatedAt());
        dto.setUpdatedAt(award.getUpdatedAt());
        dto.setSubmissions(submissionDtos);
        dto.setPersonnel(personnel);
        dto.setLinked_files(linkedFiles);

        return dto;
    }

    List<SubmissionWithSchemaDto> buildSubmissionDtos(List<FormSubmission> submissions) {
        List<SubmissionWithSchemaDto> dtos = new ArrayList<>();

        for (FormSubmission sub : submissions) {
            // Pinned schema version (if the submission was pinned to a specific version)
            FormSchemaVersion pinnedVersion = null;
            if (sub.getSchemaVersionId() != null) {
                pinnedVersion = formSchemaVersionRepository.findById(sub.getSchemaVersionId())
                        .orElse(null);
            }

            // Current schema version for this form config
            FormSchemaVersion currentVersion = formSchemaVersionRepository
                    .findByFormIdAndIsCurrentTrue(sub.getFormConfigId())
                    .orElse(null);

            // Form title from the configuration
            String formTitle = formConfigurationRepository.findById(sub.getFormConfigId())
                    .map(FormConfiguration::getTitle)
                    .orElse(null);

            // COALESCE logic: use pinned if available, otherwise current
            FormSchemaVersion effectiveVersion = pinnedVersion != null ? pinnedVersion : currentVersion;

            Map<String, Object> jsonSchema = effectiveVersion != null ? effectiveVersion.getJsonSchema() : Map.of();
            Map<String, Object> uiSchema = effectiveVersion != null ? effectiveVersion.getUiSchema() : Map.of();
            Integer schemaVersion = effectiveVersion != null ? effectiveVersion.getVersion() : null;
            Integer currentVersionNumber = currentVersion != null ? currentVersion.getVersion() : null;

            dtos.add(new SubmissionWithSchemaDto(
                    sub.getId(),
                    sub.getAwardId(),
                    sub.getFormConfigId(),
                    sub.getFormKey(),
                    sub.getFormData(),
                    sub.getStatus(),
                    sub.getSubmittedAt(),
                    sub.getCompletionDate(),
                    sub.getIsLocked(),
                    sub.getSchemaVersionId(),
                    schemaVersion,
                    sub.getNotes(),
                    sub.getCreatedAt(),
                    sub.getUpdatedAt(),
                    formTitle,
                    jsonSchema,
                    uiSchema,
                    currentVersionNumber
            ));
        }

        return dtos;
    }

    @Transactional
    public Award create(Award award) {
        Award saved = awardRepository.save(award);

        // For each active form config, create a submission pinned to the current schema version
        List<FormConfiguration> activeConfigs = formConfigurationRepository
                .findByIsActiveTrueOrderByFormKey();

        for (FormConfiguration config : activeConfigs) {
            FormSchemaVersion currentVersion = formSchemaVersionRepository
                    .findByFormIdAndIsCurrentTrue(config.getId())
                    .orElse(null);

            FormSubmission submission = FormSubmission.builder()
                    .awardId(saved.getId())
                    .formConfigId(config.getId())
                    .formKey(config.getFormKey())
                    .formData(Map.of())
                    .status("not_started")
                    .isLocked(false)
                    .schemaVersionId(currentVersion != null ? currentVersion.getId() : null)
                    .schemaVersion(currentVersion != null ? currentVersion.getVersion() : null)
                    .build();
            formSubmissionRepository.save(submission);
        }

        return saved;
    }

    @Transactional
    public Award update(UUID id, Map<String, Object> fields) {
        Award award = findById(id);

        if (fields.containsKey("log_number")) {
            award.setLogNumber((String) fields.get("log_number"));
        }
        if (fields.containsKey("award_number")) {
            award.setAwardNumber((String) fields.get("award_number"));
        }
        if (fields.containsKey("award_mechanism")) {
            award.setAwardMechanism((String) fields.get("award_mechanism"));
        }
        if (fields.containsKey("funding_opportunity")) {
            award.setFundingOpportunity((String) fields.get("funding_opportunity"));
        }
        if (fields.containsKey("principal_investigator")) {
            award.setPrincipalInvestigator((String) fields.get("principal_investigator"));
        }
        if (fields.containsKey("performing_organization")) {
            award.setPerformingOrganization((String) fields.get("performing_organization"));
        }
        if (fields.containsKey("contracting_organization")) {
            award.setContractingOrganization((String) fields.get("contracting_organization"));
        }
        if (fields.containsKey("period_of_performance")) {
            award.setPeriodOfPerformance((String) fields.get("period_of_performance"));
        }
        if (fields.containsKey("award_amount")) {
            Object val = fields.get("award_amount");
            if (val instanceof Number num) {
                award.setAwardAmount(BigDecimal.valueOf(num.doubleValue()));
            } else if (val instanceof String s) {
                award.setAwardAmount(new BigDecimal(s));
            }
        }
        if (fields.containsKey("program_office")) {
            award.setProgramOffice((String) fields.get("program_office"));
        }
        if (fields.containsKey("program")) {
            award.setProgram((String) fields.get("program"));
        }
        if (fields.containsKey("science_officer")) {
            award.setScienceOfficer((String) fields.get("science_officer"));
        }
        if (fields.containsKey("gor_cor")) {
            award.setGorCor((String) fields.get("gor_cor"));
        }
        if (fields.containsKey("pi_budget")) {
            Object val = fields.get("pi_budget");
            if (val instanceof Number num) {
                award.setPiBudget(BigDecimal.valueOf(num.doubleValue()));
            } else if (val instanceof String s) {
                award.setPiBudget(new BigDecimal(s));
            }
        }
        if (fields.containsKey("final_recommended_budget")) {
            Object val = fields.get("final_recommended_budget");
            if (val instanceof Number num) {
                award.setFinalRecommendedBudget(BigDecimal.valueOf(num.doubleValue()));
            } else if (val instanceof String s) {
                award.setFinalRecommendedBudget(new BigDecimal(s));
            }
        }
        if (fields.containsKey("program_manager")) {
            award.setProgramManager((String) fields.get("program_manager"));
        }
        if (fields.containsKey("contract_grants_specialist")) {
            award.setContractGrantsSpecialist((String) fields.get("contract_grants_specialist"));
        }
        if (fields.containsKey("branch_chief")) {
            award.setBranchChief((String) fields.get("branch_chief"));
        }
        if (fields.containsKey("prime_award_type")) {
            award.setPrimeAwardType((String) fields.get("prime_award_type"));
        }
        if (fields.containsKey("status")) {
            award.setStatus((String) fields.get("status"));
        }

        return awardRepository.save(award);
    }
}
