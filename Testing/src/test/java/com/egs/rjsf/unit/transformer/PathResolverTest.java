package com.egs.rjsf.unit.transformer;

import com.egs.rjsf.transformer.engine.PathResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PathResolver Unit Tests")
class PathResolverTest {

    private final PathResolver resolver = new PathResolver();

    @Test
    @DisplayName("get() retrieves top-level value")
    void getTopLevel() {
        Map<String, Object> data = Map.of("pi_budget", 100000);
        Optional<Object> result = resolver.get("pi_budget", data);
        assertThat(result).contains(100000);
    }

    @Test
    @DisplayName("get() retrieves nested value via dot notation")
    void getNestedValue() {
        Map<String, Object> data = Map.of(
                "personal", Map.of("firstName", "Jane", "lastName", "Smith")
        );
        Optional<Object> result = resolver.get("personal.firstName", data);
        assertThat(result).contains("Jane");
    }

    @Test
    @DisplayName("get() returns empty for missing path")
    void getMissingPath() {
        Map<String, Object> data = Map.of("pi_budget", 100000);
        Optional<Object> result = resolver.get("nonexistent", data);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("get() returns empty for missing nested path")
    void getMissingNestedPath() {
        Map<String, Object> data = Map.of("personal", Map.of("firstName", "Jane"));
        Optional<Object> result = resolver.get("personal.email", data);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("set() creates top-level value")
    void setTopLevel() {
        Map<String, Object> data = new HashMap<>();
        resolver.set("pi_budget", 200000, data);
        assertThat(data).containsEntry("pi_budget", 200000);
    }

    @Test
    @DisplayName("set() creates nested value with auto-vivification")
    void setNestedWithAutoVivification() {
        Map<String, Object> data = new HashMap<>();
        resolver.set("personal.firstName", "Alex", data);
        @SuppressWarnings("unchecked")
        Map<String, Object> personal = (Map<String, Object>) data.get("personal");
        assertThat(personal).containsEntry("firstName", "Alex");
    }

    @Test
    @DisplayName("set() overwrites existing value")
    void setOverwrite() {
        Map<String, Object> data = new HashMap<>(Map.of("pi_budget", 100));
        resolver.set("pi_budget", 999, data);
        assertThat(data).containsEntry("pi_budget", 999);
    }
}
