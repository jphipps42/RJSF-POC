package com.egs.rjsf.service;

import com.egs.rjsf.dto.MigrationResultDto;
import com.egs.rjsf.dto.SubmissionWithSchemaDto;
import com.egs.rjsf.entity.FormConfiguration;
import com.egs.rjsf.entity.FormSchemaVersion;
import com.egs.rjsf.entity.FormSubmission;
import com.egs.rjsf.repository.FormConfigurationRepository;
import com.egs.rjsf.repository.FormSchemaVersionRepository;
import com.egs.rjsf.repository.FormSubmissionRepository;
import com.egs.rjsf.transformer.service.SubmissionWriteService;
import com.egs.rjsf.util.MigrationEngine;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class FormSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(FormSubmissionService.class);

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormSchemaVersionRepository formSchemaVersionRepository;
    private final FormConfigurationRepository formConfigurationRepository;
    private final MigrationEngine migrationEngine;
    private final SubmissionWriteService transformerWriteService;

    public FormSubmissionService(FormSubmissionRepository formSubmissionRepository,
                                 FormSchemaVersionRepository formSchemaVersionRepository,
                                 FormConfigurationRepository formConfigurationRepository,
                                 MigrationEngine migrationEngine,
                                 SubmissionWriteService transformerWriteService) {
        this.formSubmissionRepository = formSubmissionRepository;
        this.formSchemaVersionRepository = formSchemaVersionRepository;
        this.formConfigurationRepository = formConfigurationRepository;
        this.migrationEngine = migrationEngine;
        this.transformerWriteService = transformerWriteService;
    }

    public List<SubmissionWithSchemaDto> findByAwardId(UUID awardId) {
        List<FormSubmission> submissions = formSubmissionRepository
                .findByAwardIdOrderByFormKey(awardId);
        return buildSubmissionDtos(submissions);
    }

    public SubmissionWithSchemaDto findById(UUID id) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));
        return buildSubmissionDto(submission);
    }

    public SubmissionWithSchemaDto findByAwardIdAndFormKey(UUID awardId, String formKey) {
        FormSubmission submission = formSubmissionRepository
                .findByAwardIdAndFormKey(awardId, formKey)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found for awardId=" + awardId + ", formKey=" + formKey));
        return buildSubmissionDto(submission);
    }

    public MigrationResultDto getForEdit(UUID id) {
        return migrationEngine.migrateSubmissionToCurrentVersion(id);
    }

    public MigrationResultDto getForAudit(UUID id) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        FormSchemaVersion pinnedVersion = formSchemaVersionRepository
                .findById(submission.getSchemaVersionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pinned schema version not found: " + submission.getSchemaVersionId()));

        return new MigrationResultDto(
                submission.getFormData(),
                pinnedVersion.getJsonSchema(),
                pinnedVersion.getUiSchema(),
                pinnedVersion.getVersion(),
                false
        );
    }

    @Transactional
    public FormSubmission saveDraft(UUID id, Map<String, Object> formData, String sectionId) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        // Check if the specific section is locked
        if (sectionId != null && isSectionLocked(submission, sectionId)) {
            throw new IllegalStateException("Section '" + sectionId + "' is submitted and cannot be edited.");
        }
        if (sectionId == null && Boolean.TRUE.equals(submission.getIsLocked())) {
            throw new IllegalStateException("Submission is locked and cannot be edited: " + id);
        }

        // Pin to current schema version
        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(submission.getFormConfigId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No current schema version for form: " + submission.getFormConfigId()));
        submission.setSchemaVersionId(currentVersion.getId());
        submission.setSchemaVersion(currentVersion.getVersion());

        submission.setFormData(formData);

        // Update section status
        if (sectionId != null) {
            Map<String, Object> sectionStatus = new HashMap<>(
                    submission.getSectionStatus() != null ? submission.getSectionStatus() : Map.of());
            sectionStatus.put(sectionId, "in_progress");
            submission.setSectionStatus(sectionStatus);
        }

        submission.setStatus("in_progress");
        FormSubmission saved = formSubmissionRepository.save(submission);
        syncToRelationalTable(saved, sectionId);
        return saved;
    }

    @Transactional
    public FormSubmission submit(UUID id, Map<String, Object> formData, String sectionId) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        if (sectionId != null && isSectionLocked(submission, sectionId)) {
            throw new IllegalStateException("Section '" + sectionId + "' is already submitted.");
        }
        if (sectionId == null && Boolean.TRUE.equals(submission.getIsLocked())) {
            throw new IllegalStateException("Submission is locked and cannot be submitted: " + id);
        }

        // Pin to current version
        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(submission.getFormConfigId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No current schema version for form: " + submission.getFormConfigId()));
        submission.setSchemaVersionId(currentVersion.getId());
        submission.setSchemaVersion(currentVersion.getVersion());

        submission.setFormData(formData);

        // Update section status
        Map<String, Object> sectionStatus = new HashMap<>(
                submission.getSectionStatus() != null ? submission.getSectionStatus() : Map.of());
        if (sectionId != null) {
            sectionStatus.put(sectionId, "submitted");
        } else {
            // Submit all sections
            for (String key : sectionStatus.keySet()) {
                sectionStatus.put(key, "submitted");
            }
        }
        submission.setSectionStatus(sectionStatus);

        // Check if ALL sections are now submitted
        boolean allSubmitted = sectionStatus.values().stream()
                .allMatch(v -> "submitted".equals(v));

        if (allSubmitted) {
            submission.setStatus("submitted");
            submission.setIsLocked(true);
            submission.setSubmittedAt(OffsetDateTime.now());
            submission.setCompletionDate(OffsetDateTime.now());
        } else {
            submission.setStatus("in_progress");
        }

        FormSubmission saved = formSubmissionRepository.save(submission);
        syncToRelationalTable(saved, sectionId);
        return saved;
    }

    @Transactional
    public FormSubmission reset(UUID id, String sectionId) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        if (sectionId != null) {
            // Unlock one section — set back to in_progress so user can edit
            Map<String, Object> sectionStatus = new HashMap<>(
                    submission.getSectionStatus() != null ? submission.getSectionStatus() : Map.of());
            sectionStatus.put(sectionId, "in_progress");
            submission.setSectionStatus(sectionStatus);
            submission.setStatus("in_progress");
            submission.setIsLocked(false);
        } else {
            // Unlock all sections — set back to in_progress, preserve data
            submission.setStatus("in_progress");
            submission.setIsLocked(false);
            submission.setSubmittedAt(null);
            submission.setCompletionDate(null);

            Map<String, Object> sectionStatus = new HashMap<>(
                    submission.getSectionStatus() != null ? submission.getSectionStatus() : Map.of());
            for (String key : sectionStatus.keySet()) {
                if ("submitted".equals(sectionStatus.get(key))) {
                    sectionStatus.put(key, "in_progress");
                }
            }
            submission.setSectionStatus(sectionStatus);
        }

        return formSubmissionRepository.save(submission);
    }

    // ---- Helper methods ----

    // Maps each leaf section ID to its transformer template formId
    private static final Map<String, String> SECTION_TO_TEMPLATE = Map.ofEntries(
            Map.entry("overview", "pre-award-overview"),
            Map.entry("safety_review", "pre-award-safety"),
            Map.entry("animal_review", "pre-award-animal"),
            Map.entry("human_no_regulatory", "pre-award-human"),
            Map.entry("human_anatomical", "pre-award-human"),
            Map.entry("human_data_secondary", "pre-award-human"),
            Map.entry("human_subjects", "pre-award-human"),
            Map.entry("human_special_topics", "pre-award-human"),
            Map.entry("human_estimated_start", "pre-award-human"),
            Map.entry("acq_br_personnel", "pre-award-acquisition"),
            Map.entry("acq_br_equipment", "pre-award-acquisition"),
            Map.entry("acq_br_travel", "pre-award-acquisition"),
            Map.entry("acq_br_materials", "pre-award-acquisition"),
            Map.entry("acq_br_consultant", "pre-award-acquisition"),
            Map.entry("acq_br_third_party", "pre-award-acquisition"),
            Map.entry("acq_br_other_direct", "pre-award-acquisition"),
            Map.entry("acq_br_additional", "pre-award-acquisition"),
            Map.entry("acq_peer_review", "pre-award-acquisition"),
            Map.entry("acq_sow_concerns", "pre-award-acquisition"),
            Map.entry("acq_cps", "pre-award-acquisition"),
            Map.entry("acq_ier", "pre-award-acquisition"),
            Map.entry("acq_data_management", "pre-award-acquisition"),
            Map.entry("acq_special_requirements", "pre-award-acquisition"),
            Map.entry("final_recommendation", "pre-award-final")
    );

    private void syncToRelationalTable(FormSubmission submission, String sectionId) {
        try {
            if (!"pre_award_composite".equals(submission.getFormKey())) {
                return;
            }
            String formId = sectionId != null ? SECTION_TO_TEMPLATE.get(sectionId) : null;
            if (formId == null) {
                log.debug("No transformer template mapped for section '{}', skipping", sectionId);
                return;
            }
            transformerWriteService.writeSection(
                    formId,
                    submission.getAwardId(),
                    submission.getFormData(),
                    sectionId,
                    null
            );
        } catch (Exception e) {
            log.warn("Transformer sync failed for submission {} section '{}': {}",
                    submission.getId(), sectionId, e.getMessage(), e);
        }
    }

    private boolean isSectionLocked(FormSubmission submission, String sectionId) {
        if (submission.getSectionStatus() == null) return false;
        return "submitted".equals(submission.getSectionStatus().get(sectionId));
    }

    private List<SubmissionWithSchemaDto> buildSubmissionDtos(List<FormSubmission> submissions) {
        List<SubmissionWithSchemaDto> dtos = new ArrayList<>();
        for (FormSubmission sub : submissions) {
            dtos.add(buildSubmissionDto(sub));
        }
        return dtos;
    }

    private SubmissionWithSchemaDto buildSubmissionDto(FormSubmission sub) {
        FormSchemaVersion pinnedVersion = null;
        if (sub.getSchemaVersionId() != null) {
            pinnedVersion = formSchemaVersionRepository.findById(sub.getSchemaVersionId())
                    .orElse(null);
        }

        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(sub.getFormConfigId())
                .orElse(null);

        String formTitle = formConfigurationRepository.findById(sub.getFormConfigId())
                .map(FormConfiguration::getTitle)
                .orElse(null);

        FormSchemaVersion effectiveVersion = pinnedVersion != null ? pinnedVersion : currentVersion;

        Map<String, Object> jsonSchema = effectiveVersion != null ? effectiveVersion.getJsonSchema() : Map.of();
        Map<String, Object> uiSchema = effectiveVersion != null ? effectiveVersion.getUiSchema() : Map.of();
        Integer schemaVersion = effectiveVersion != null ? effectiveVersion.getVersion() : null;
        Integer currentVersionNumber = currentVersion != null ? currentVersion.getVersion() : null;

        return new SubmissionWithSchemaDto(
                sub.getId(),
                sub.getAwardId(),
                sub.getFormConfigId(),
                sub.getFormKey(),
                sub.getFormData(),
                sub.getStatus(),
                sub.getSectionStatus(),
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
        );
    }
}
