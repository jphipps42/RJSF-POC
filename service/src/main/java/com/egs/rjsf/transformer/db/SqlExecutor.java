package com.egs.rjsf.transformer.db;

import com.egs.rjsf.transformer.util.IdentifierValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

@Component
public class SqlExecutor {

    private static final Logger log = LoggerFactory.getLogger(SqlExecutor.class);

    private final NamedParameterJdbcTemplate namedJdbc;

    public SqlExecutor(NamedParameterJdbcTemplate namedJdbc) {
        this.namedJdbc = namedJdbc;
    }

    public long insert(String tableName, Map<String, Object> columnValues) {
        IdentifierValidator.validate(tableName);
        columnValues.keySet().forEach(IdentifierValidator::validate);

        StringJoiner colJoiner = new StringJoiner(", ");
        StringJoiner paramJoiner = new StringJoiner(", ");

        for (String col : columnValues.keySet()) {
            colJoiner.add(col);
            paramJoiner.add(":" + col);
        }

        String sql = new StringBuilder()
                .append("INSERT INTO ").append(tableName)
                .append(" (").append(colJoiner).append(")")
                .append(" VALUES (").append(paramJoiner).append(")")
                .append(" RETURNING submission_id")
                .toString();

        log.debug("Executing insert on '{}': {}", tableName, sql);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = new MapSqlParameterSource(columnValues);
        namedJdbc.update(sql, params, keyHolder, new String[]{"submission_id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert into '" + tableName + "' did not return a generated key");
        }
        return key.longValue();
    }

    public void batchInsertChildren(String childTable, String parentKeyColumn, long parentId,
                                    String orderColumn, List<Map<String, Object>> rows) {
        IdentifierValidator.validate(childTable);
        IdentifierValidator.validate(parentKeyColumn);
        IdentifierValidator.validate(orderColumn);

        if (rows == null || rows.isEmpty()) {
            return;
        }

        // Validate all column names from the first row
        rows.get(0).keySet().forEach(IdentifierValidator::validate);

        // Build the column list from the first row, plus parent key and order column
        Map<String, Object> sampleRow = rows.get(0);
        StringJoiner colJoiner = new StringJoiner(", ");
        StringJoiner paramJoiner = new StringJoiner(", ");

        colJoiner.add(parentKeyColumn);
        paramJoiner.add(":" + parentKeyColumn);

        colJoiner.add(orderColumn);
        paramJoiner.add(":" + orderColumn);

        for (String col : sampleRow.keySet()) {
            colJoiner.add(col);
            paramJoiner.add(":" + col);
        }

        String sql = new StringBuilder()
                .append("INSERT INTO ").append(childTable)
                .append(" (").append(colJoiner).append(")")
                .append(" VALUES (").append(paramJoiner).append(")")
                .toString();

        log.debug("Executing batch insert on '{}': {}", childTable, sql);

        MapSqlParameterSource[] batchParams = new MapSqlParameterSource[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            MapSqlParameterSource paramSource = new MapSqlParameterSource(row);
            paramSource.addValue(parentKeyColumn, parentId);
            paramSource.addValue(orderColumn, i);
            batchParams[i] = paramSource;
        }

        namedJdbc.batchUpdate(sql, batchParams);
        log.debug("Inserted {} child rows into '{}'", rows.size(), childTable);
    }

    public void upsertByAwardId(String tableName, UUID awardId, Map<String, Object> columnValues) {
        IdentifierValidator.validate(tableName);
        columnValues.keySet().forEach(IdentifierValidator::validate);

        StringJoiner colJoiner = new StringJoiner(", ");
        StringJoiner paramJoiner = new StringJoiner(", ");
        StringJoiner updateJoiner = new StringJoiner(", ");

        colJoiner.add("award_id");
        paramJoiner.add(":award_id");

        for (String col : columnValues.keySet()) {
            colJoiner.add(col);
            paramJoiner.add(":" + col);
            updateJoiner.add(col + " = EXCLUDED." + col);
        }

        // Always update updated_at on conflict
        updateJoiner.add("updated_at = NOW()");

        String sql = "INSERT INTO " + tableName
                + " (" + colJoiner + ")"
                + " VALUES (" + paramJoiner + ")"
                + " ON CONFLICT (award_id) DO UPDATE SET " + updateJoiner;

        log.debug("Executing upsert on '{}': {}", tableName, sql);

        MapSqlParameterSource params = new MapSqlParameterSource(columnValues);
        params.addValue("award_id", awardId);
        namedJdbc.update(sql, params);
    }

    public Map<String, Object> selectById(String tableName, List<String> columns, long submissionId) {
        IdentifierValidator.validate(tableName);
        columns.forEach(IdentifierValidator::validate);

        String colList = String.join(", ", columns);
        String sql = new StringBuilder()
                .append("SELECT ").append(colList)
                .append(" FROM ").append(tableName)
                .append(" WHERE submission_id = :submissionId")
                .toString();

        log.debug("Executing selectById on '{}': {}", tableName, sql);

        MapSqlParameterSource params = new MapSqlParameterSource("submissionId", submissionId);

        try {
            return namedJdbc.queryForMap(sql, params);
        } catch (EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException(
                    "No row found in '" + tableName + "' with submission_id=" + submissionId, 1, e);
        }
    }

    public List<Map<String, Object>> selectChildren(String childTable, List<String> columns,
                                                     String parentKeyColumn, long parentId,
                                                     String orderColumn) {
        IdentifierValidator.validate(childTable);
        columns.forEach(IdentifierValidator::validate);
        IdentifierValidator.validate(parentKeyColumn);
        IdentifierValidator.validate(orderColumn);

        String colList = String.join(", ", columns);
        String sql = new StringBuilder()
                .append("SELECT ").append(colList)
                .append(" FROM ").append(childTable)
                .append(" WHERE ").append(parentKeyColumn).append(" = :parentId")
                .append(" ORDER BY ").append(orderColumn).append(" ASC")
                .toString();

        log.debug("Executing selectChildren on '{}': {}", childTable, sql);

        MapSqlParameterSource params = new MapSqlParameterSource("parentId", parentId);
        return namedJdbc.queryForList(sql, params);
    }
}
