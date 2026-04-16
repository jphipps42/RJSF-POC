package com.egs.rjsf.service.sync;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Utility for safely extracting typed values from Map<String, Object> formData.
 * Handles the loose typing that comes from Jackson JSON deserialization.
 */
public final class FormDataExtractor {

    private FormDataExtractor() {}

    public static String getString(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null) return null;
        return val.toString().strip();
    }

    public static BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(val.toString().strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean getBoolean(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(val.toString());
    }

    public static LocalDate getLocalDate(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        try {
            return LocalDate.parse(val.toString().strip());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the raw value for JSONB columns (Map, List, or other).
     * Hypersistence JsonType handles serialization to JSONB on persist.
     */
    public static Object getJsonb(Map<String, Object> data, String key) {
        return data.get(key);
    }
}
