package com.egs.rjsf.service;

import com.egs.rjsf.entity.FormConfiguration;
import com.egs.rjsf.entity.FormSchemaVersion;
import com.egs.rjsf.entity.SchemaMigration;
import com.egs.rjsf.repository.FormConfigurationRepository;
import com.egs.rjsf.repository.FormSchemaVersionRepository;
import com.egs.rjsf.repository.SchemaMigrationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SchemaVersionService {

    private final FormSchemaVersionRepository formSchemaVersionRepository;
    private final SchemaMigrationRepository schemaMigrationRepository;
    private final FormConfigurationRepository formConfigurationRepository;

    public SchemaVersionService(FormSchemaVersionRepository formSchemaVersionRepository,
                                SchemaMigrationRepository schemaMigrationRepository,
                                FormConfigurationRepository formConfigurationRepository) {
        this.formSchemaVersionRepository = formSchemaVersionRepository;
        this.schemaMigrationRepository = schemaMigrationRepository;
        this.formConfigurationRepository = formConfigurationRepository;
    }

    public List<FormSchemaVersion> findAllByFormId(UUID formId) {
        return formSchemaVersionRepository.findByFormIdOrderByVersionDesc(formId);
    }

    public FormSchemaVersion findCurrentByFormId(UUID formId) {
        return formSchemaVersionRepository.findByFormIdAndIsCurrentTrue(formId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No current schema version found for form: " + formId));
    }

    public FormSchemaVersion findByFormIdAndVersion(UUID formId, Integer version) {
        return formSchemaVersionRepository.findByFormIdAndVersion(formId, version)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Schema version " + version + " not found for form: " + formId));
    }

    @Transactional
    public FormSchemaVersion publish(UUID formId,
                                     Map<String, Object> jsonSchema,
                                     Map<String, Object> uiSchema,
                                     Map<String, Object> defaultData,
                                     String changeNotes,
                                     List<Map<String, Object>> migrationRules) {
        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(formId)
                .orElse(null);

        // Compute next version from the max existing version (not just current)
        List<FormSchemaVersion> allVersions = formSchemaVersionRepository.findByFormIdOrderByVersionDesc(formId);
        int maxVersion = allVersions.isEmpty() ? 0 : allVersions.get(0).getVersion();
        int nextVersion = maxVersion + 1;

        if (currentVersion != null) {
            currentVersion.setIsCurrent(false);
            formSchemaVersionRepository.saveAndFlush(currentVersion);
        }

        FormSchemaVersion newVersion = FormSchemaVersion.builder()
                .formId(formId)
                .version(nextVersion)
                .jsonSchema(jsonSchema)
                .uiSchema(uiSchema != null ? uiSchema : Map.of())
                .defaultData(defaultData != null ? defaultData : Map.of())
                .changeNotes(changeNotes)
                .isCurrent(true)
                .build();
        FormSchemaVersion saved = formSchemaVersionRepository.save(newVersion);

        if (migrationRules != null && !migrationRules.isEmpty() && currentVersion != null) {
            SchemaMigration migration = SchemaMigration.builder()
                    .formId(formId)
                    .fromVersion(currentVersion.getVersion())
                    .toVersion(nextVersion)
                    .migrationScript(migrationRules)
                    .build();
            schemaMigrationRepository.save(migration);
        }

        // Touch the parent form_configurations.updated_at
        FormConfiguration config = formConfigurationRepository.findById(formId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormConfiguration not found: " + formId));
        formConfigurationRepository.save(config); // triggers @PreUpdate

        return saved;
    }

    @Transactional
    public FormSchemaVersion setCurrent(UUID formId, Integer version) {
        // Demote all current versions for this form
        List<FormSchemaVersion> allVersions = formSchemaVersionRepository
                .findByFormIdOrderByVersionDesc(formId);
        for (FormSchemaVersion v : allVersions) {
            if (Boolean.TRUE.equals(v.getIsCurrent())) {
                v.setIsCurrent(false);
                formSchemaVersionRepository.saveAndFlush(v);
            }
        }

        // Promote the specified version
        FormSchemaVersion target = formSchemaVersionRepository
                .findByFormIdAndVersion(formId, version)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Schema version " + version + " not found for form: " + formId));
        target.setIsCurrent(true);
        return formSchemaVersionRepository.save(target);
    }
}
