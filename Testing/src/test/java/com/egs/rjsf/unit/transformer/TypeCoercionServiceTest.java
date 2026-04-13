package com.egs.rjsf.unit.transformer;

import com.egs.rjsf.transformer.engine.TypeCoercionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TypeCoercionService Unit Tests")
class TypeCoercionServiceTest {

    private TypeCoercionService coercionService;

    @BeforeEach
    void setUp() {
        coercionService = new TypeCoercionService(new ObjectMapper());
    }

    @Test
    @DisplayName("coerces string to TEXT")
    void coerceText() {
        Object result = coercionService.coerce(42, "TEXT");
        assertThat(result).isEqualTo("42");
    }

    @Test
    @DisplayName("coerces string to INTEGER")
    void coerceInteger() {
        Object result = coercionService.coerce("42", "INTEGER");
        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("coerces number to INTEGER")
    void coerceNumberToInteger() {
        Object result = coercionService.coerce(42.7, "INTEGER");
        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("coerces string to NUMERIC (BigDecimal)")
    void coerceNumeric() {
        Object result = coercionService.coerce("145000.50", "NUMERIC");
        assertThat(result).isEqualTo(new BigDecimal("145000.50"));
    }

    @Test
    @DisplayName("coerces string to BOOLEAN")
    void coerceBoolean() {
        assertThat(coercionService.coerce("true", "BOOLEAN")).isEqualTo(true);
        assertThat(coercionService.coerce("false", "BOOLEAN")).isEqualTo(false);
    }

    @Test
    @DisplayName("coerces number to BOOLEAN")
    void coerceNumberToBoolean() {
        assertThat(coercionService.coerce(1, "BOOLEAN")).isEqualTo(true);
        assertThat(coercionService.coerce(0, "BOOLEAN")).isEqualTo(false);
    }

    @Test
    @DisplayName("coerces ISO string to DATE")
    void coerceDate() {
        Object result = coercionService.coerce("2025-05-01", "DATE");
        assertThat(result).isEqualTo(LocalDate.of(2025, 5, 1));
    }

    @Test
    @DisplayName("coerces Map to JSONB as PGobject")
    void coerceMapToJsonb() {
        Object result = coercionService.coerce(Map.of("key", "value"), "JSONB");
        assertThat(result).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) result;
        assertThat(pg.getType()).isEqualTo("jsonb");
        assertThat(pg.getValue()).contains("key");
    }

    @Test
    @DisplayName("coerces List to JSONB as PGobject")
    void coerceListToJsonb() {
        Object result = coercionService.coerce(List.of("a", "b"), "JSONB");
        assertThat(result).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) result;
        assertThat(pg.getValue()).contains("a");
    }

    @Test
    @DisplayName("coerces string to UUID")
    void coerceUuid() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        Object result = coercionService.coerce(uuid, "UUID");
        assertThat(result).isEqualTo(UUID.fromString(uuid));
    }

    @Test
    @DisplayName("returns null for null input")
    void coerceNull() {
        Object result = coercionService.coerce(null, "TEXT");
        assertThat(result).isNull();
    }
}
