package com.egs.rjsf.transformer.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TypeCoercionService {

    private static final Logger log = LoggerFactory.getLogger(TypeCoercionService.class);

    private final ObjectMapper objectMapper;

    public TypeCoercionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Coerce a Java value to the target SQL type expected by the database column.
     */
    public Object coerce(Object value, String sqlType) {
        if (value == null) {
            return null;
        }

        String type = sqlType.toUpperCase();

        if ("TEXT".equals(type) || type.startsWith("VARCHAR")) {
            return String.valueOf(value);
        }

        return switch (type) {
            case "INTEGER" -> coerceToInteger(value);
            case "NUMERIC" -> coerceToNumeric(value);
            case "BOOLEAN" -> coerceToBoolean(value);
            case "DATE" -> coerceToDate(value);
            case "TIMESTAMPTZ" -> coerceToTimestamp(value);
            case "JSONB" -> coerceToJsonb(value);
            case "UUID" -> coerceToUuid(value);
            default -> {
                log.warn("Unknown SQL type '{}' — returning value as-is (type: {})",
                        sqlType, value.getClass().getSimpleName());
                yield value;
            }
        };
    }

    private Integer coerceToInteger(Object value) {
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private BigDecimal coerceToNumeric(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private Boolean coerceToBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number num) return num.intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDate coerceToDate(Object value) {
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private Instant coerceToTimestamp(Object value) {
        if (value instanceof Instant inst) return inst;
        if (value instanceof OffsetDateTime odt) return odt.toInstant();
        return Instant.parse(value.toString());
    }

    private Object coerceToJsonb(Object value) {
        if (value instanceof String) return value;
        if (value instanceof Map || value instanceof List) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize value to JSONB", e);
            }
        }
        return String.valueOf(value);
    }

    private UUID coerceToUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }
}
