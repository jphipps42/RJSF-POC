package com.egs.rjsf.transformer.service;

import com.egs.rjsf.transformer.db.SqlExecutor;
import com.egs.rjsf.transformer.dto.TransformerSubmissionResponse;
import com.egs.rjsf.transformer.engine.PathResolver;
import com.egs.rjsf.transformer.exception.SubmissionNotFoundException;
import com.egs.rjsf.transformer.hook.HookRegistry;
import com.egs.rjsf.transformer.hook.ReadHook;
import com.egs.rjsf.transformer.model.FieldMapping;
import com.egs.rjsf.transformer.model.RelationMapping;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import com.egs.rjsf.transformer.registry.TemplateRegistry;
import com.egs.rjsf.transformer.transform.FromDbFunction;
import com.egs.rjsf.transformer.transform.TransformRegistry;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubmissionReadService {

    private final TemplateRegistry templateRegistry;
    private final HookRegistry hookRegistry;
    private final PathResolver pathResolver;
    private final TransformRegistry transformRegistry;
    private final SqlExecutor sqlExecutor;

    public SubmissionReadService(TemplateRegistry templateRegistry,
                                 HookRegistry hookRegistry,
                                 PathResolver pathResolver,
                                 TransformRegistry transformRegistry,
                                 SqlExecutor sqlExecutor) {
        this.templateRegistry = templateRegistry;
        this.hookRegistry = hookRegistry;
        this.pathResolver = pathResolver;
        this.transformRegistry = transformRegistry;
        this.sqlExecutor = sqlExecutor;
    }

    public TransformerSubmissionResponse read(long submissionId, String formId, Integer templateVersion) {
        // Stage 1: Template Resolution
        TransformerTemplate template = templateRegistry.getTemplate(formId);

        // Stage 2: Primary Row Query
        List<String> columns = new ArrayList<>();
        columns.add("submission_id");
        columns.add(template.schemaVersion().column());
        columns.add(template.auditColumns().createdAt());
        columns.add(template.auditColumns().submittedBy());
        for (FieldMapping field : template.fields()) {
            columns.add(field.column());
        }

        Map<String, Object> row;
        try {
            row = sqlExecutor.selectById(template.tableName(), columns, submissionId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new SubmissionNotFoundException(submissionId);
        }

        // Stage 3: Child Table Queries
        Map<String, List<Map<String, Object>>> childData = new LinkedHashMap<>();
        for (RelationMapping relation : template.relations()) {
            List<String> childColumns = new ArrayList<>();
            for (FieldMapping childField : relation.fields()) {
                childColumns.add(childField.column());
            }
            childColumns.add(relation.orderColumn());
            List<Map<String, Object>> children = sqlExecutor.selectChildren(
                    relation.childTable(), childColumns,
                    relation.parentKey(), submissionId, relation.orderColumn());
            childData.put(relation.jsonPath(), children);
        }

        // Stage 4: Post-Query Hooks
        for (ReadHook hook : hookRegistry.resolveReadHooks(template.readHooks().postQuery())) {
            row = hook.apply(row);
        }

        // Stage 5: Field Assembly (column values -> formData)
        Map<String, Object> formData = new LinkedHashMap<>();
        for (FieldMapping field : template.fields()) {
            Object value = row.get(field.column());

            // Apply fromDb transform
            if (value != null && field.transform() != null && field.transform().fromDb() != null) {
                Optional<FromDbFunction> fn = transformRegistry.getFromDb(field.transform().fromDb());
                if (fn.isPresent()) {
                    value = fn.get().apply(value, field);
                }
            }

            if (value != null) {
                pathResolver.set(field.jsonPath(), value, formData);
            }
        }

        // Assemble child data into formData
        for (RelationMapping relation : template.relations()) {
            List<Map<String, Object>> children = childData.get(relation.jsonPath());
            if (children != null && !children.isEmpty()) {
                List<Map<String, Object>> assembledChildren = new ArrayList<>();
                for (Map<String, Object> childRow : children) {
                    Map<String, Object> childObj = new LinkedHashMap<>();
                    for (FieldMapping childField : relation.fields()) {
                        Object value = childRow.get(childField.column());
                        if (value != null && childField.transform() != null && childField.transform().fromDb() != null) {
                            Optional<FromDbFunction> fn = transformRegistry.getFromDb(childField.transform().fromDb());
                            if (fn.isPresent()) {
                                value = fn.get().apply(value, childField);
                            }
                        }
                        if (value != null) {
                            pathResolver.set(childField.jsonPath(), value, childObj);
                        }
                    }
                    assembledChildren.add(childObj);
                }
                pathResolver.set(relation.jsonPath(), assembledChildren, formData);
            }
        }

        // Stage 6: Post-Assembly Hooks
        for (ReadHook hook : hookRegistry.resolveReadHooks(template.readHooks().postAssemble())) {
            formData = hook.apply(formData);
        }

        // Stage 7: Response Envelope
        Object createdAtRaw = row.get(template.auditColumns().createdAt());
        OffsetDateTime createdAt = null;
        if (createdAtRaw instanceof OffsetDateTime odt) {
            createdAt = odt;
        } else if (createdAtRaw instanceof java.sql.Timestamp ts) {
            createdAt = ts.toInstant().atOffset(ZoneOffset.UTC);
        }

        Integer schemaVersion = null;
        Object svRaw = row.get(template.schemaVersion().column());
        if (svRaw instanceof Number num) {
            schemaVersion = num.intValue();
        }

        return new TransformerSubmissionResponse(
                submissionId,
                template.formId(),
                schemaVersion,
                createdAt,
                formData
        );
    }
}
