package com.egs.rjsf.transformer.transform;

import com.egs.rjsf.transformer.model.FieldMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BuiltInTransforms {

    private BuiltInTransforms() {}

    // ── 1. toLocalDate ──────────────────────────────────────────────────

    @TransformFunction("toLocalDate")
    public static class ToLocalDateTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            if (jsValue instanceof LocalDate) return jsValue;
            return LocalDate.parse(jsValue.toString());
        }
    }

    // ── 2. toIsoDateString ──────────────────────────────────────────────

    @TransformFunction("toIsoDateString")
    public static class ToIsoDateStringTransform implements FromDbFunction {
        @Override
        public Object apply(Object dbValue, FieldMapping field) {
            if (dbValue == null) return null;
            if (dbValue instanceof LocalDate ld) return ld.toString();
            if (dbValue instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate().toString();
            return dbValue.toString();
        }
    }

    // ── 3. toInstant ────────────────────────────────────────────────────

    @TransformFunction("toInstant")
    public static class ToInstantTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            if (jsValue instanceof Instant) return jsValue;
            return Instant.parse(jsValue.toString());
        }
    }

    // ── 4. fromInstant ──────────────────────────────────────────────────

    @TransformFunction("fromInstant")
    public static class FromInstantTransform implements FromDbFunction {
        @Override
        public Object apply(Object dbValue, FieldMapping field) {
            if (dbValue == null) return null;
            if (dbValue instanceof Instant inst) return inst.toString();
            return dbValue.toString();
        }
    }

    // ── 5. jsonStringify ────────────────────────────────────────────────

    @TransformFunction("jsonStringify")
    public static class JsonStringifyTransform implements ToDbFunction {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            try {
                return MAPPER.writeValueAsString(jsValue);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Failed to serialize value to JSON for field " + field.column(), e);
            }
        }
    }

    // ── 6. jsonParse ────────────────────────────────────────────────────

    @TransformFunction("jsonParse")
    public static class JsonParseTransform implements FromDbFunction {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public Object apply(Object dbValue, FieldMapping field) {
            if (dbValue == null) return null;
            if (dbValue instanceof Map || dbValue instanceof List) return dbValue;
            try {
                String json = dbValue.toString();
                // Try map first, fall back to list
                if (json.trim().startsWith("{")) {
                    return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
                } else {
                    return MAPPER.readValue(json, new TypeReference<List<Object>>() {});
                }
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Failed to parse JSON for field " + field.column(), e);
            }
        }
    }

    // ── 7. trimString ───────────────────────────────────────────────────

    @TransformFunction("trimString")
    public static class TrimStringTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            return jsValue.toString().strip();
        }
    }

    // ── 8. toLowerCase ──────────────────────────────────────────────────

    @TransformFunction("toLowerCase")
    public static class ToLowerCaseTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            return jsValue.toString().toLowerCase(Locale.ROOT);
        }
    }

    // ── 9. toBoolean ────────────────────────────────────────────────────

    @TransformFunction("toBoolean")
    public static class ToBooleanTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            if (jsValue instanceof Boolean) return jsValue;
            if (jsValue instanceof Number num) return num.intValue() != 0;
            return Boolean.parseBoolean(jsValue.toString());
        }
    }

    // ── 10. toBigDecimal ────────────────────────────────────────────────

    @TransformFunction("toBigDecimal")
    public static class ToBigDecimalTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            if (jsValue instanceof BigDecimal) return jsValue;
            if (jsValue instanceof Number num) return new BigDecimal(num.toString());
            try {
                return new BigDecimal(jsValue.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Cannot convert '" + jsValue + "' to BigDecimal for field " + field.column(), e);
            }
        }
    }

    // ── 11. toInteger ───────────────────────────────────────────────────

    @TransformFunction("toInteger")
    public static class ToIntegerTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            if (jsValue instanceof Integer) return jsValue;
            if (jsValue instanceof Number num) return num.intValue();
            try {
                return Integer.parseInt(jsValue.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Cannot convert '" + jsValue + "' to Integer for field " + field.column(), e);
            }
        }
    }

    // ── 12. maskLast4 ───────────────────────────────────────────────────

    @TransformFunction("maskLast4")
    public static class MaskLast4Transform implements FromDbFunction {
        @Override
        public Object apply(Object dbValue, FieldMapping field) {
            if (dbValue == null) return null;
            String str = dbValue.toString();
            if (str.length() <= 4) return str;
            return "*".repeat(str.length() - 4) + str.substring(str.length() - 4);
        }
    }

    // ── 13. toUUID ──────────────────────────────────────────────────────

    @TransformFunction("toUUID")
    public static class ToUUIDTransform implements ToDbFunction {
        @Override
        public Object apply(Object jsValue, FieldMapping field) {
            if (jsValue == null) return null;
            if (jsValue instanceof UUID) return jsValue;
            try {
                return UUID.fromString(jsValue.toString());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid UUID format '" + jsValue + "' for field " + field.column(), e);
            }
        }
    }
}
