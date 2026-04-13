package com.egs.rjsf.unit.transformer;

import com.egs.rjsf.transformer.model.FieldMapping;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TransformerTemplate Unit Tests")
class TransformerTemplateTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("loads overview template from classpath")
    void loadsOverviewTemplate() throws Exception {
        TransformerTemplate template = loadTemplate("templates/pre-award-overview.transformer.json");

        assertThat(template.formId()).isEqualTo("pre-award-overview");
        assertThat(template.tableName()).isEqualTo("form_pre_award_overview");
        assertThat(template.version()).isEqualTo(1);
        assertThat(template.fields()).hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    @DisplayName("fieldsBySection() returns all fields when sectionId is null")
    void fieldsBySectionNull() throws Exception {
        TransformerTemplate template = loadTemplate("templates/pre-award-human.transformer.json");

        List<FieldMapping> all = template.fieldsBySection(null);
        assertThat(all).hasSize(template.fields().size());
    }

    @Test
    @DisplayName("fieldsBySection() filters by tag")
    void fieldsBySectionFilters() throws Exception {
        TransformerTemplate template = loadTemplate("templates/pre-award-human.transformer.json");

        List<FieldMapping> noReg = template.fieldsBySection("human_no_regulatory");
        assertThat(noReg).isNotEmpty();
        assertThat(noReg).allSatisfy(f ->
                assertThat(f.tags()).contains("human_no_regulatory")
        );

        List<FieldMapping> anatomical = template.fieldsBySection("human_anatomical");
        assertThat(anatomical).isNotEmpty();
        assertThat(anatomical).allSatisfy(f ->
                assertThat(f.tags()).contains("human_anatomical")
        );

        // No overlap between sections
        assertThat(noReg).noneMatch(anatomical::contains);
    }

    @Test
    @DisplayName("fieldsBySection() returns empty for unknown section")
    void fieldsBySectionUnknown() throws Exception {
        TransformerTemplate template = loadTemplate("templates/pre-award-overview.transformer.json");

        List<FieldMapping> result = template.fieldsBySection("nonexistent_section");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("all 6 templates have correct table names and formIds")
    void allTemplatesValid() throws Exception {
        String[][] expected = {
                {"pre-award-overview", "form_pre_award_overview"},
                {"pre-award-safety", "form_pre_award_safety"},
                {"pre-award-animal", "form_pre_award_animal"},
                {"pre-award-human", "form_pre_award_human"},
                {"pre-award-acquisition", "form_pre_award_acquisition"},
                {"pre-award-final", "form_pre_award_final"},
        };

        for (String[] pair : expected) {
            TransformerTemplate t = loadTemplate("templates/" + pair[0] + ".transformer.json");
            assertThat(t.formId()).isEqualTo(pair[0]);
            assertThat(t.tableName()).isEqualTo(pair[1]);
            assertThat(t.fields()).isNotEmpty();
            assertThat(t.schemaVersion()).isNotNull();
            assertThat(t.auditColumns()).isNotNull();
        }
    }

    private TransformerTemplate loadTemplate(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Template resource %s should exist", resourcePath).isNotNull();
            return mapper.readValue(is, TransformerTemplate.class);
        }
    }
}
