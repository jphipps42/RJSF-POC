package com.egs.rjsf.service;

import com.egs.rjsf.dto.MigrationResultDto;
import com.egs.rjsf.dto.SubmissionWithSchemaDto;
import com.egs.rjsf.entity.FormConfiguration;
import com.egs.rjsf.entity.FormSchemaVersion;
import com.egs.rjsf.entity.FormSubmission;
import com.egs.rjsf.repository.FormConfigurationRepository;
import com.egs.rjsf.repository.FormSchemaVersionRepository;
import com.egs.rjsf.repository.FormSubmissionRepository;
import com.egs.rjsf.util.MigrationEngine;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FormSubmissionService {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormSchemaVersionRepository formSchemaVersionRepository;
    private final FormConfigurationRepository formConfigurationRepository;
    private final MigrationEngine migrationEngine;

    public FormSubmissionService(FormSubmissionRepository formSubmissionRepository,
                                 FormSchemaVersionRepository formSchemaVersionRepository,
                                 FormConfigurationRepository formConfigurationRepository,
                                 MigrationEngine migrationEngine) {
        this.formSubmissionRepository = formSubmissionRepository;
        this.formSchemaVersionRepository = formSchemaVersionRepository;
        this.formConfigurationRepository = formConfigurationRepository;
        this.migrationEngine = migrationEngine;
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
    public FormSubmission saveDraft(UUID id, Map<String, Object> formData) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        if (Boolean.TRUE.equals(submission.getIsLocked())) {
            throw new IllegalStateException("Submission is locked and cannot be edited: " + id);
        }

        // If not yet pinned to a schema version, pin to the current version
        if (submission.getSchemaVersionId() == null) {
            FormSchemaVersion currentVersion = formSchemaVersionRepository
                    .findByFormIdAndIsCurrentTrue(submission.getFormConfigId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No current schema version for form: " + submission.getFormConfigId()));
            submission.setSchemaVersionId(currentVersion.getId());
            submission.setSchemaVersion(currentVersion.getVersion());
        }

        submission.setFormData(formData);
        submission.setStatus("in_progress");
        return formSubmissionRepository.save(submission);
    }

    @Transactional
    public FormSubmission submit(UUID id, Map<String, Object> formData) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        if (Boolean.TRUE.equals(submission.getIsLocked())) {
            throw new IllegalStateException("Submission is locked and cannot be submitted: " + id);
        }

        // Always pin to current version on submit
        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(submission.getFormConfigId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No current schema version for form: " + submission.getFormConfigId()));
        submission.setSchemaVersionId(currentVersion.getId());
        submission.setSchemaVersion(currentVersion.getVersion());

        submission.setFormData(formData);
        submission.setStatus("submitted");
        submission.setIsLocked(true);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission.setCompletionDate(OffsetDateTime.now());

        return formSubmissionRepository.save(submission);
    }

    @Transactional
    public FormSubmission reset(UUID id) {
        FormSubmission submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + id));

        submission.setFormData(Map.of());
        submission.setStatus("not_started");
        submission.setIsLocked(false);
        submission.setSubmittedAt(null);
        submission.setCompletionDate(null);
        submission.setSchemaVersionId(null);
        submission.setSchemaVersion(null);

        return formSubmissionRepository.save(submission);
    }

    // ---- Helper methods for building DTOs ----

    private List<SubmissionWithSchemaDto> buildSubmissionDtos(List<FormSubmission> submissions) {
        List<SubmissionWithSchemaDto> dtos = new ArrayList<>();
        for (FormSubmission sub : submissions) {
            dtos.add(buildSubmissionDto(sub));
        }
        return dtos;
    }

    private SubmissionWithSchemaDto buildSubmissionDto(FormSubmission sub) {
        // Pinned schema version
        FormSchemaVersion pinnedVersion = null;
        if (sub.getSchemaVersionId() != null) {
            pinnedVersion = formSchemaVersionRepository.findById(sub.getSchemaVersionId())
                    .orElse(null);
        }

        // Current schema version for this form config
        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(sub.getFormConfigId())
                .orElse(null);

        // Form title
        String formTitle = formConfigurationRepository.findById(sub.getFormConfigId())
                .map(FormConfiguration::getTitle)
                .orElse(null);

        // COALESCE: pinned if available, else current
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
