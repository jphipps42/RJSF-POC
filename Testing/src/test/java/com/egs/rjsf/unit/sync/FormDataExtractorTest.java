package com.egs.rjsf.unit.sync;

import com.egs.rjsf.service.sync.FormDataExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FormDataExtractor Unit Tests")
class FormDataExtractorTest {

    @Nested
    @DisplayName("getString()")
    class GetString {

        @Test
        @DisplayName("extracts string value")
        void extractsString() {
            assertThat(FormDataExtractor.getString(Map.of("k", "hello"), "k")).isEqualTo("hello");
        }

        @Test
        @DisplayName("returns null for missing key")
        void returnsNullForMissing() {
            assertThat(FormDataExtractor.getString(Map.of(), "k")).isNull();
        }

        @Test
        @DisplayName("converts non-string to string")
        void convertsToString() {
            assertThat(FormDataExtractor.getString(Map.of("k", 42), "k")).isEqualTo("42");
        }

        @Test
        @DisplayName("strips whitespace")
        void stripsWhitespace() {
            assertThat(FormDataExtractor.getString(Map.of("k", "  padded  "), "k")).isEqualTo("padded");
        }
    }

    @Nested
    @DisplayName("getBigDecimal()")
    class GetBigDecimal {

        @Test
        @DisplayName("extracts from integer")
        void fromInteger() {
            assertThat(FormDataExtractor.getBigDecimal(Map.of("k", 750000), "k"))
                    .isEqualByComparingTo(new BigDecimal("750000"));
        }

        @Test
        @DisplayName("extracts from double")
        void fromDouble() {
            assertThat(FormDataExtractor.getBigDecimal(Map.of("k", 3.14), "k"))
                    .isEqualByComparingTo(new BigDecimal("3.14"));
        }

        @Test
        @DisplayName("extracts from string")
        void fromString() {
            assertThat(FormDataExtractor.getBigDecimal(Map.of("k", "999.99"), "k"))
                    .isEqualByComparingTo(new BigDecimal("999.99"));
        }

        @Test
        @DisplayName("returns null for missing key")
        void returnsNullForMissing() {
            assertThat(FormDataExtractor.getBigDecimal(Map.of(), "k")).isNull();
        }

        @Test
        @DisplayName("returns null for non-numeric string")
        void returnsNullForNonNumeric() {
            assertThat(FormDataExtractor.getBigDecimal(Map.of("k", "not_a_number"), "k")).isNull();
        }
    }

    @Nested
    @DisplayName("getBoolean()")
    class GetBoolean {

        @Test
        @DisplayName("extracts true boolean")
        void extractsTrue() {
            assertThat(FormDataExtractor.getBoolean(Map.of("k", true), "k")).isTrue();
        }

        @Test
        @DisplayName("extracts false boolean")
        void extractsFalse() {
            assertThat(FormDataExtractor.getBoolean(Map.of("k", false), "k")).isFalse();
        }

        @Test
        @DisplayName("parses string 'true'")
        void parsesStringTrue() {
            assertThat(FormDataExtractor.getBoolean(Map.of("k", "true"), "k")).isTrue();
        }

        @Test
        @DisplayName("treats non-zero number as true")
        void nonZeroIsTrue() {
            assertThat(FormDataExtractor.getBoolean(Map.of("k", 1), "k")).isTrue();
        }

        @Test
        @DisplayName("treats zero as false")
        void zeroIsFalse() {
            assertThat(FormDataExtractor.getBoolean(Map.of("k", 0), "k")).isFalse();
        }

        @Test
        @DisplayName("returns null for missing key")
        void returnsNullForMissing() {
            assertThat(FormDataExtractor.getBoolean(Map.of(), "k")).isNull();
        }
    }

    @Nested
    @DisplayName("getLocalDate()")
    class GetLocalDate {

        @Test
        @DisplayName("parses ISO date string")
        void parsesIsoDate() {
            assertThat(FormDataExtractor.getLocalDate(Map.of("k", "2025-03-15"), "k"))
                    .isEqualTo(LocalDate.of(2025, 3, 15));
        }

        @Test
        @DisplayName("returns null for invalid date")
        void returnsNullForInvalid() {
            assertThat(FormDataExtractor.getLocalDate(Map.of("k", "not-a-date"), "k")).isNull();
        }

        @Test
        @DisplayName("returns null for missing key")
        void returnsNullForMissing() {
            assertThat(FormDataExtractor.getLocalDate(Map.of(), "k")).isNull();
        }
    }

    @Nested
    @DisplayName("getJsonb()")
    class GetJsonb {

        @Test
        @DisplayName("returns map as-is")
        void returnsMap() {
            Map<String, Object> inner = Map.of("a", 1);
            assertThat(FormDataExtractor.getJsonb(Map.of("k", inner), "k")).isSameAs(inner);
        }

        @Test
        @DisplayName("returns list as-is")
        void returnsList() {
            List<String> list = List.of("x", "y");
            assertThat(FormDataExtractor.getJsonb(Map.of("k", list), "k")).isSameAs(list);
        }

        @Test
        @DisplayName("returns null for missing key")
        void returnsNullForMissing() {
            assertThat(FormDataExtractor.getJsonb(Map.of(), "k")).isNull();
        }
    }
}
