package com.egs.rjsf.util;

import com.egs.rjsf.dto.MigrationResultDto;
import com.egs.rjsf.entity.FormSchemaVersion;
import com.egs.rjsf.entity.FormSubmission;
import com.egs.rjsf.entity.SchemaMigration;
import com.egs.rjsf.repository.FormSchemaVersionRepository;
import com.egs.rjsf.repository.FormSubmissionRepository;
import com.egs.rjsf.repository.SchemaMigrationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MigrationEngine {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormSchemaVersionRepository formSchemaVersionRepository;
    private final SchemaMigrationRepository schemaMigrationRepository;

    private final Map<String, Function<Object, Object>> transforms;

    public MigrationEngine(FormSubmissionRepository formSubmissionRepository,
                           FormSchemaVersionRepository formSchemaVersionRepository,
                           SchemaMigrationRepository schemaMigrationRepository) {
        this.formSubmissionRepository = formSubmissionRepository;
        this.formSchemaVersionRepository = formSchemaVersionRepository;
        this.schemaMigrationRepository = schemaMigrationRepository;

        this.transforms = Map.of(
                "toString", value -> value == null ? null : String.valueOf(value),
                "toNumber", value -> {
                    if (value == null) return null;
                    if (value instanceof Number n) return n.doubleValue();
                    return Double.parseDouble(String.valueOf(value));
                },
                "toBoolean", value -> {
                    if (value == null) return null;
                    if (value instanceof Boolean b) return b;
                    return Boolean.parseBoolean(String.valueOf(value));
                },
                "splitComma", value -> {
                    if (value == null) return null;
                    return Arrays.asList(String.valueOf(value).split(",", -1))
                            .stream()
                            .map(String::trim)
                            .collect(Collectors.toList());
                },
                "joinComma", value -> {
                    if (value == null) return null;
                    if (value instanceof List<?> list) {
                        return list.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(","));
                    }
                    return String.valueOf(value);
                }
        );
    }

    /**
     * Applies a list of migration rules to form data and returns a new Map with
     * the rules applied. This is a pure function with no database access.
     */
    public Map<String, Object> migrateFormData(Map<String, Object> data,
                                                List<Map<String, Object>> rules) {
        Map<String, Object> result = new LinkedHashMap<>(data);

        for (Map<String, Object> rule : rules) {
            String op = (String) rule.get("op");
            if (op == null) continue;

            switch (op) {
                case "rename" -> {
                    String from = (String) rule.get("from");
                    String to = (String) rule.get("to");
                    if (from != null && to != null && result.containsKey(from)) {
                        result.put(to, result.remove(from));
                    }
                }
                case "set_default" -> {
                    String field = (String) rule.get("field");
                    Object value = rule.get("value");
                    if (field != null && !result.containsKey(field)) {
                        result.put(field, value);
                    }
                }
                case "drop" -> {
                    String field = (String) rule.get("field");
                    if (field != null) {
                        result.remove(field);
                    }
                }
                case "transform" -> {
                    String field = (String) rule.get("field");
                    String fn = (String) rule.get("fn");
                    if (field != null && fn != null && result.containsKey(field)) {
                        Function<Object, Object> transform = transforms.get(fn);
                        if (transform != null) {
                            result.put(field, transform.apply(result.get(field)));
                        }
                    }
                }
                default -> { /* unknown op, skip */ }
            }
        }

        return result;
    }

    /**
     * Loads a submission by ID, determines whether its pinned schema version is
     * behind the current version, and if so chains all intermediate migration
     * scripts to produce up-to-date form data.
     */
    public MigrationResultDto migrateSubmissionToCurrentVersion(UUID submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FormSubmission not found: " + submissionId));

        FormSchemaVersion pinnedVersion = formSchemaVersionRepository.findById(submission.getSchemaVersionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pinned FormSchemaVersion not found: " + submission.getSchemaVersionId()));

        FormSchemaVersion currentVersion = formSchemaVersionRepository
                .findByFormIdAndIsCurrentTrue(pinnedVersion.getFormId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No current FormSchemaVersion found for form: " + pinnedVersion.getFormId()));

        // If already at or ahead of current version, return data as-is
        if (pinnedVersion.getVersion() >= currentVersion.getVersion()) {
            return new MigrationResultDto(
                    submission.getFormData(),
                    currentVersion.getJsonSchema(),
                    currentVersion.getUiSchema(),
                    currentVersion.getVersion(),
                    false
            );
        }

        // Load all migration scripts between pinned and current versions
        List<SchemaMigration> migrations = schemaMigrationRepository
                .findByFormIdAndFromVersionGreaterThanEqualAndFromVersionLessThanOrderByFromVersionAsc(
                        pinnedVersion.getFormId(),
                        pinnedVersion.getVersion(),
                        currentVersion.getVersion()
                );

        // Chain migration rules
        Map<String, Object> migratedData = new LinkedHashMap<>(submission.getFormData());
        for (SchemaMigration migration : migrations) {
            migratedData = migrateFormData(migratedData, migration.getMigrationScript());
        }

        return new MigrationResultDto(
                migratedData,
                currentVersion.getJsonSchema(),
                currentVersion.getUiSchema(),
                currentVersion.getVersion(),
                true
        );
    }
}
