package com.egs.rjsf.transformer.service;

import com.egs.rjsf.transformer.db.SqlExecutor;
import com.egs.rjsf.transformer.dto.TransformerSubmissionRequest;
import com.egs.rjsf.transformer.dto.TransformerSubmissionResponse;
import com.egs.rjsf.transformer.engine.PathResolver;
import com.egs.rjsf.transformer.engine.TypeCoercionService;
import com.egs.rjsf.transformer.entity.TemplateHistoryEntity;
import com.egs.rjsf.transformer.exception.DuplicateSubmissionException;
import com.egs.rjsf.transformer.exception.VersionMismatchException;
import com.egs.rjsf.transformer.hook.HookRegistry;
import com.egs.rjsf.transformer.hook.WriteHook;
import com.egs.rjsf.transformer.model.FieldMapping;
import com.egs.rjsf.transformer.model.RelationMapping;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import com.egs.rjsf.transformer.registry.TemplateRegistry;
import com.egs.rjsf.transformer.repository.TemplateHistoryRepository;
import com.egs.rjsf.transformer.transform.ToDbFunction;
import com.egs.rjsf.transformer.transform.TransformRegistry;
import com.egs.rjsf.transformer.validation.ValidationGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubmissionWriteService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionWriteService.class);

    private final TemplateRegistry templateRegistry;
    private final ValidationGate validationGate;
    private final HookRegistry hookRegistry;
    private final PathResolver pathResolver;
    private final TransformRegistry transformRegistry;
    private final TypeCoercionService typeCoercionService;
    private final SqlExecutor sqlExecutor;
    private final TemplateHistoryRepository templateHistoryRepository;

    public SubmissionWriteService(TemplateRegistry templateRegistry,
                                  ValidationGate validationGate,
                                  HookRegistry hookRegistry,
                                  PathResolver pathResolver,
                                  TransformRegistry transformRegistry,
                                  TypeCoercionService typeCoercionService,
                                  SqlExecutor sqlExecutor,
                                  TemplateHistoryRepository templateHistoryRepository) {
        this.templateRegistry = templateRegistry;
        this.validationGate = validationGate;
        this.hookRegistry = hookRegistry;
        this.pathResolver = pathResolver;
        this.transformRegistry = transformRegistry;
        this.typeCoercionService = typeCoercionService;
        this.sqlExecutor = sqlExecutor;
        this.templateHistoryRepository = templateHistoryRepository;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransformerSubmissionResponse write(TransformerSubmissionRequest request) {
        // Stage 1: Template Resolution
        TransformerTemplate template = templateRegistry.getTemplate(request.formId());

        // Check version mismatch: if request specifies a version newer than template
        if (request.schemaVersion() != null && request.schemaVersion() > template.version()) {
            throw new VersionMismatchException(
                    "Requested schema version " + request.schemaVersion()
                            + " exceeds template version " + template.version());
        }

        Map<String, Object> formData = new LinkedHashMap<>(request.formData());

        // Stage 2: Pre-Validation Hooks
        for (WriteHook hook : hookRegistry.resolveWriteHooks(template.writeHooks().preValidation())) {
            formData = hook.apply(formData);
        }

        // Stage 3: Validation Gate
        validationGate.validate(formData, template.fields());

        // Stage 4: Field Mapping (formData -> column values)
        Map<String, Object> columnValues = new LinkedHashMap<>();
        for (FieldMapping field : template.fields()) {
            Optional<Object> rawValue = pathResolver.get(field.jsonPath(), formData);
            Object value = rawValue.orElse(field.defaultValue());

            // Apply toDb transform if configured
            if (value != null && field.transform() != null && field.transform().toDb() != null) {
                Optional<ToDbFunction> fn = transformRegistry.getToDb(field.transform().toDb());
                if (fn.isPresent()) {
                    value = fn.get().apply(value, field);
                }
            }

            // Type coercion
            if (value != null) {
                value = typeCoercionService.coerce(value, field.sqlType());
            }

            columnValues.put(field.column(), value);
        }

        // Add schema version and audit columns
        columnValues.put(template.schemaVersion().column(), template.version());
        columnValues.put(template.auditColumns().submittedBy(), request.submittedBy());

        // Stage 5: Pre-Insert Hooks
        for (WriteHook hook : hookRegistry.resolveWriteHooks(template.writeHooks().preInsert())) {
            columnValues = hook.apply(columnValues);
        }

        // Stage 6: SQL Insert (parent + children)
        long submissionId;
        try {
            submissionId = sqlExecutor.insert(template.tableName(), columnValues);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new DuplicateSubmissionException("Duplicate submission for form: " + request.formId());
        }

        // Insert child tables for relations
        for (RelationMapping relation : template.relations()) {
            Optional<Object> arrayValue = pathResolver.get(relation.jsonPath(), formData);
            if (arrayValue.isPresent() && arrayValue.get() instanceof List<?> items) {
                List<Map<String, Object>> childRows = new ArrayList<>();
                for (Object item : items) {
                    if (item instanceof Map<?, ?> itemMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemData = (Map<String, Object>) itemMap;
                        Map<String, Object> childRow = new LinkedHashMap<>();
                        for (FieldMapping childField : relation.fields()) {
                            Optional<Object> childVal = pathResolver.get(childField.jsonPath(), itemData);
                            Object val = childVal.orElse(childField.defaultValue());
                            if (val != null && childField.transform() != null && childField.transform().toDb() != null) {
                                Optional<ToDbFunction> fn = transformRegistry.getToDb(childField.transform().toDb());
                                if (fn.isPresent()) {
                                    val = fn.get().apply(val, childField);
                                }
                            }
                            if (val != null) {
                                val = typeCoercionService.coerce(val, childField.sqlType());
                            }
                            childRow.put(childField.column(), val);
                        }
                        childRows.add(childRow);
                    }
                }
                if (!childRows.isEmpty()) {
                    sqlExecutor.batchInsertChildren(
                            relation.childTable(), relation.parentKey(), submissionId,
                            relation.orderColumn(), childRows);
                }
            }
        }

        log.info("Wrote submission {} for form '{}'", submissionId, request.formId());

        // Stage 7: Post-Insert Hooks (side-effects only, after commit)
        // Note: these run inside the same transaction; for truly post-commit hooks,
        // a @TransactionalEventListener approach would be needed
        for (WriteHook hook : hookRegistry.resolveWriteHooks(template.writeHooks().postInsert())) {
            try {
                hook.apply(columnValues);
            } catch (Exception e) {
                log.warn("Post-insert hook failed for submission {}: {}", submissionId, e.getMessage());
            }
        }

        // Save template history
        saveTemplateHistory(template);

        return new TransformerSubmissionResponse(
                submissionId,
                template.formId(),
                template.version(),
                OffsetDateTime.now(),
                null  // write response does not echo formData per spec
        );
    }

    /**
     * Section-aware upsert for composite form integration.
     * Filters fields by section tag, maps and transforms them, then upserts into the
     * relational table keyed by award_id. Only the section's columns are touched.
     */
    public void writeSection(String formId, UUID awardId, Map<String, Object> formData,
                             String sectionId, String submittedBy) {
        TransformerTemplate template = templateRegistry.getTemplate(formId);

        List<FieldMapping> sectionFields = template.fieldsBySection(sectionId);
        if (sectionFields.isEmpty()) {
            log.debug("No transformer fields mapped for section '{}', skipping", sectionId);
            return;
        }

        Map<String, Object> columnValues = new LinkedHashMap<>();
        for (FieldMapping field : sectionFields) {
            Optional<Object> rawValue = pathResolver.get(field.jsonPath(), formData);
            Object value = rawValue.orElse(field.defaultValue());

            if (value != null && field.transform() != null && field.transform().toDb() != null) {
                Optional<ToDbFunction> fn = transformRegistry.getToDb(field.transform().toDb());
                if (fn.isPresent()) {
                    value = fn.get().apply(value, field);
                }
            }

            if (value != null) {
                value = typeCoercionService.coerce(value, field.sqlType());
            }

            columnValues.put(field.column(), value);
        }

        columnValues.put(template.schemaVersion().column(), template.version());
        if (submittedBy != null) {
            columnValues.put(template.auditColumns().submittedBy(), submittedBy);
        }

        sqlExecutor.upsertByAwardId(template.tableName(), awardId, columnValues);

        log.info("Upserted section '{}' for award {} into '{}'", sectionId, awardId, template.tableName());
    }

    private void saveTemplateHistory(TransformerTemplate template) {
        if (templateHistoryRepository.findByFormIdAndVersion(template.formId(), template.version()).isEmpty()) {
            TemplateHistoryEntity history = TemplateHistoryEntity.builder()
                    .formId(template.formId())
                    .version(template.version())
                    .templateJson(Map.of(
                            "formId", template.formId(),
                            "version", template.version(),
                            "tableName", template.tableName()
                    ))
                    .build();
            templateHistoryRepository.save(history);
        }
    }
}
