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
public class FormConfigurationService {

    private final FormConfigurationRepository formConfigurationRepository;
    private final FormSchemaVersionRepository formSchemaVersionRepository;
    private final SchemaMigrationRepository schemaMigrationRepository;

    public FormConfigurationService(FormConfigurationRepository formConfigurationRepository,
                                    FormSchemaVersionRepository formSchemaVersionRepository,
                                    SchemaMigrationRepository schemaMigrationRepository) {
        this.formConfigurationRepository = formConfigurationRepository;
        this.formSchemaVersionRepository = formSchemaVersionRepository;
        this.schemaMigrationRepository = schemaMigrationRepository;
    }

    public List<FormConfiguration> findAll() {
        return formConfigurationRepository.findByIsActiveTrueOrderByFormKey();
    }

    public FormConfiguration findByFormKey(String formKey) {
        return formConfigurationRepository.findByFormKeyAndIsActiveTrue(formKey)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormConfiguration not found for formKey: " + formKey));
    }

    @Transactional
    public FormConfiguration create(FormConfiguration config) {
        FormConfiguration saved = formConfigurationRepository.save(config);

        FormSchemaVersion version = FormSchemaVersion.builder()
                .formId(saved.getId())
                .version(1)
                .jsonSchema(saved.getJsonSchema())
                .uiSchema(saved.getUiSchema() != null ? saved.getUiSchema() : Map.of())
                .defaultData(saved.getDefaultData() != null ? saved.getDefaultData() : Map.of())
                .isCurrent(true)
                .build();
        formSchemaVersionRepository.save(version);

        return saved;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public FormConfiguration update(UUID id, Map<String, Object> updates) {
        FormConfiguration config = formConfigurationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormConfiguration not found: " + id));

        if (updates.containsKey("title")) {
            config.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("description")) {
            config.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("ui_schema")) {
            config.setUiSchema((Map<String, Object>) updates.get("ui_schema"));
        }
        if (updates.containsKey("default_data")) {
            config.setDefaultData((Map<String, Object>) updates.get("default_data"));
        }

        if (updates.containsKey("json_schema")) {
            Map<String, Object> newJsonSchema = (Map<String, Object>) updates.get("json_schema");
            config.setJsonSchema(newJsonSchema);

            FormSchemaVersion currentVersion = formSchemaVersionRepository
                    .findByFormIdAndIsCurrentTrue(config.getId())
                    .orElse(null);

            int nextVersion = currentVersion != null ? currentVersion.getVersion() + 1 : 1;

            if (currentVersion != null) {
                currentVersion.setIsCurrent(false);
                formSchemaVersionRepository.saveAndFlush(currentVersion);
            }

            FormSchemaVersion newVersion = FormSchemaVersion.builder()
                    .formId(config.getId())
                    .version(nextVersion)
                    .jsonSchema(newJsonSchema)
                    .uiSchema(config.getUiSchema() != null ? config.getUiSchema() : Map.of())
                    .defaultData(config.getDefaultData() != null ? config.getDefaultData() : Map.of())
                    .isCurrent(true)
                    .build();
            formSchemaVersionRepository.save(newVersion);

            if (updates.containsKey("migration_rules")
                    && updates.get("migration_rules") instanceof List<?> rules
                    && !rules.isEmpty()
                    && currentVersion != null) {
                SchemaMigration migration = SchemaMigration.builder()
                        .formId(config.getId())
                        .fromVersion(currentVersion.getVersion())
                        .toVersion(nextVersion)
                        .migrationScript((List<Map<String, Object>>) updates.get("migration_rules"))
                        .build();
                schemaMigrationRepository.save(migration);
            }
        }

        config.setVersion(config.getVersion() + 1);
        return formConfigurationRepository.save(config);
    }

    public FormConfiguration softDelete(UUID id) {
        FormConfiguration config = formConfigurationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormConfiguration not found: " + id));
        config.setIsActive(false);
        return formConfigurationRepository.save(config);
    }
}
