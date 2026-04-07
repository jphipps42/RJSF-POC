package com.egs.rjsf.transformer.ddl;

import com.egs.rjsf.transformer.model.FieldMapping;
import com.egs.rjsf.transformer.model.RelationMapping;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import com.egs.rjsf.transformer.util.IdentifierValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DdlManager {

    private static final Logger log = LoggerFactory.getLogger(DdlManager.class);

    private static final Set<String> ALLOWED_SQL_TYPES = Set.of(
            "TEXT", "INTEGER", "BIGINT", "SMALLINT", "BOOLEAN", "NUMERIC",
            "DECIMAL", "REAL", "DOUBLE PRECISION", "DATE", "TIME",
            "TIMESTAMP", "TIMESTAMPTZ", "UUID", "JSONB", "JSON",
            "VARCHAR", "CHAR", "BYTEA", "SERIAL", "BIGSERIAL"
    );

    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "submission_id", "id"
    );

    private final JdbcTemplate jdbcTemplate;

    public DdlManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void reconcileAll(List<TransformerTemplate> templates) {
        for (TransformerTemplate template : templates) {
            try {
                reconcile(template);
            } catch (Exception e) {
                log.error("Failed to reconcile template '{}': {}", template.formId(), e.getMessage(), e);
            }
        }
    }

    public void reconcile(TransformerTemplate template) {
        String tableName = IdentifierValidator.validate(template.tableName());
        String schemaVersionCol = IdentifierValidator.validate(template.schemaVersion().column());
        validateSqlType(template.schemaVersion().sqlType());

        String createdAtCol = IdentifierValidator.validate(template.auditColumns().createdAt());
        String updatedAtCol = IdentifierValidator.validate(template.auditColumns().updatedAt());
        String submittedByCol = IdentifierValidator.validate(template.auditColumns().submittedBy());

        Set<String> auditAndSystemColumns = new HashSet<>(SYSTEM_COLUMNS);
        auditAndSystemColumns.add(createdAtCol);
        auditAndSystemColumns.add(updatedAtCol);
        auditAndSystemColumns.add(submittedByCol);
        auditAndSystemColumns.add(schemaVersionCol);

        // Validate all field identifiers up front
        for (FieldMapping field : template.fields()) {
            IdentifierValidator.validate(field.column());
            validateSqlType(field.sqlType());
        }

        boolean tableExists = checkTableExists(tableName);

        if (!tableExists) {
            createTable(tableName, schemaVersionCol, template.schemaVersion().sqlType(),
                    template.fields(), createdAtCol, updatedAtCol, submittedByCol);
        } else {
            alterTable(tableName, template.fields(), auditAndSystemColumns);
        }

        // Reconcile child tables for relations
        for (RelationMapping relation : template.relations()) {
            reconcileChildTable(tableName, relation, auditAndSystemColumns);
        }
    }

    private boolean checkTableExists(String tableName) {
        Object result = jdbcTemplate.queryForObject(
                "SELECT to_regclass('" + tableName + "')", Object.class);
        return result != null;
    }

    private void createTable(String tableName, String schemaVersionCol, String schemaVersionType,
                             List<FieldMapping> fields, String createdAtCol,
                             String updatedAtCol, String submittedByCol) {

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (\n");
        sql.append("    submission_id BIGSERIAL PRIMARY KEY,\n");
        sql.append("    ").append(schemaVersionCol).append(" ").append(schemaVersionType).append(" NOT NULL");

        for (FieldMapping field : fields) {
            sql.append(",\n    ").append(field.column()).append(" ").append(field.sqlType());
            if (!field.nullable()) {
                sql.append(" NOT NULL");
            }
        }

        sql.append(",\n    ").append(createdAtCol).append(" TIMESTAMPTZ NOT NULL DEFAULT NOW()");
        sql.append(",\n    ").append(updatedAtCol).append(" TIMESTAMPTZ NOT NULL DEFAULT NOW()");
        sql.append(",\n    ").append(submittedByCol).append(" TEXT");
        sql.append("\n)");

        log.info("Creating table '{}': {}", tableName, sql);
        jdbcTemplate.execute(sql.toString());
        log.info("Table '{}' created successfully", tableName);
    }

    private void alterTable(String tableName, List<FieldMapping> fields,
                            Set<String> auditAndSystemColumns) {

        Set<String> existingColumns = getExistingColumns(tableName);

        // Add missing columns
        for (FieldMapping field : fields) {
            if (!existingColumns.contains(field.column())) {
                StringBuilder sql = new StringBuilder();
                sql.append("ALTER TABLE ").append(tableName)
                        .append(" ADD COLUMN ").append(field.column())
                        .append(" ").append(field.sqlType());

                log.info("Adding column '{}' to table '{}': {}", field.column(), tableName, sql);
                jdbcTemplate.execute(sql.toString());
            }
        }

        // Detect deprecated columns
        Set<String> templateColumns = new HashSet<>();
        for (FieldMapping field : fields) {
            templateColumns.add(field.column());
        }

        for (String existingCol : existingColumns) {
            if (!templateColumns.contains(existingCol) && !auditAndSystemColumns.contains(existingCol)) {
                log.warn("Deprecated column detected: '{}' in table '{}' is not defined in the template",
                        existingCol, tableName);
            }
        }
    }

    private void reconcileChildTable(String parentTable, RelationMapping relation,
                                     Set<String> parentAuditAndSystemColumns) {
        String childTable = IdentifierValidator.validate(relation.childTable());
        String parentKey = IdentifierValidator.validate(relation.parentKey());
        String orderColumn = IdentifierValidator.validate(relation.orderColumn());

        // Validate child field identifiers
        for (FieldMapping field : relation.fields()) {
            IdentifierValidator.validate(field.column());
            validateSqlType(field.sqlType());
        }

        Set<String> childSystemColumns = new HashSet<>();
        childSystemColumns.add("id");
        childSystemColumns.add(parentKey);
        childSystemColumns.add(orderColumn);

        boolean tableExists = checkTableExists(childTable);

        if (!tableExists) {
            createChildTable(childTable, parentTable, parentKey, orderColumn, relation.fields());
        } else {
            alterTable(childTable, relation.fields(), childSystemColumns);
        }
    }

    private void createChildTable(String childTable, String parentTable, String parentKey,
                                  String orderColumn, List<FieldMapping> fields) {

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(childTable).append(" (\n");
        sql.append("    id BIGSERIAL PRIMARY KEY,\n");
        sql.append("    ").append(parentKey).append(" BIGINT NOT NULL REFERENCES ")
                .append(parentTable).append("(submission_id) ON DELETE CASCADE,\n");
        sql.append("    ").append(orderColumn).append(" INTEGER NOT NULL");

        for (FieldMapping field : fields) {
            sql.append(",\n    ").append(field.column()).append(" ").append(field.sqlType());
            if (!field.nullable()) {
                sql.append(" NOT NULL");
            }
        }

        sql.append("\n)");

        log.info("Creating child table '{}': {}", childTable, sql);
        jdbcTemplate.execute(sql.toString());
        log.info("Child table '{}' created successfully", childTable);
    }

    private Set<String> getExistingColumns(String tableName) {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ?",
                String.class, tableName);
        return new HashSet<>(columns);
    }

    private void validateSqlType(String sqlType) {
        if (sqlType == null) {
            throw new IllegalArgumentException("SQL type must not be null");
        }
        // Normalize: strip length/precision specifiers for validation (e.g., VARCHAR(255) -> VARCHAR)
        String baseType = sqlType.toUpperCase().replaceAll("\\(.*\\)", "").trim();
        if (!ALLOWED_SQL_TYPES.contains(baseType)) {
            throw new IllegalArgumentException("Disallowed SQL type: " + sqlType);
        }
    }
}
