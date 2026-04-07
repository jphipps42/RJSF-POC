package com.egs.rjsf.transformer.engine;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PathResolver {

    /**
     * Get a value from a nested map using dot-notation path.
     * Example: get("patientInfo.dateOfBirth", formData)
     */
    public Optional<Object> get(String dotPath, Map<String, Object> data) {
        String[] parts = dotPath.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                if (!map.containsKey(part)) {
                    return Optional.empty();
                }
                current = map.get(part);
            } else {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(current);
    }

    /**
     * Set a value in a nested map using dot-notation path.
     * Creates intermediate maps as needed.
     * Example: set("patientInfo.dateOfBirth", "1985-04-22", formData)
     */
    @SuppressWarnings("unchecked")
    public void set(String dotPath, Object value, Map<String, Object> data) {
        String[] parts = dotPath.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(parts[i], newMap);
                current = newMap;
            }
        }
        current.put(parts[parts.length - 1], value);
    }
}
