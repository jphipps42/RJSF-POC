package com.egs.rjsf.integration;

import com.egs.rjsf.RjsfFormServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests verifying transformer relational sync against the
 * already-running PostgreSQL instance (localhost:5432).
 * Requires: docker-compose up (PostgreSQL with seeded data) + service must have run once to create tables.
 */
@SpringBootTest(
        classes = RjsfFormServiceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:5432/rjsf_forms",
                "spring.datasource.username=rjsf_user",
                "spring.datasource.password=rjsf_pass"
        }
)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Transformer Relational Sync Integration Tests")
class TransformerSyncIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static String submissionId;
    private static String awardId;

    @Test
    @Order(1)
    @DisplayName("Setup: fetch seeded submission and award IDs")
    void setup() throws Exception {
        MvcResult result = mvc.perform(get("/api/awards/by-log/TE020005"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        submissionId = root.get("submissions").get(0).get("id").asText();
        awardId = root.get("id").asText();
        assertThat(submissionId).isNotNull();
        assertThat(awardId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Overview save writes to form_pre_award_overview table")
    void overviewSyncToRelational() throws Exception {
        String body = """
                {
                    "form_data": {
                        "pi_budget": 750000,
                        "program_manager": "Sync Test PM"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT pi_budget, program_manager FROM form_pre_award_overview WHERE award_id = ?::uuid",
                awardId);

        assertThat(((BigDecimal) row.get("pi_budget")).intValue()).isEqualTo(750000);
        assertThat(row.get("program_manager")).isEqualTo("Sync Test PM");
    }

    @Test
    @Order(3)
    @DisplayName("Safety save writes to form_pre_award_safety table")
    void safetySyncToRelational() throws Exception {
        String body = """
                {
                    "form_data": {
                        "safety_q1": "yes",
                        "safety_q2": "no",
                        "safety_notes": "Test note"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "safety_review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT safety_q1, safety_q2, safety_notes FROM form_pre_award_safety WHERE award_id = ?::uuid",
                awardId);

        assertThat(row.get("safety_q1")).isEqualTo("yes");
        assertThat(row.get("safety_q2")).isEqualTo("no");
        assertThat(row.get("safety_notes")).isEqualTo("Test note");
    }

    @Test
    @Order(4)
    @DisplayName("Subsequent saves update the same row (upsert)")
    void upsertUpdatesExistingRow() throws Exception {
        String body = """
                { "form_data": { "pi_budget": 999999 } }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM form_pre_award_overview WHERE award_id = ?::uuid",
                Integer.class, awardId);
        assertThat(count).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT pi_budget FROM form_pre_award_overview WHERE award_id = ?::uuid", awardId);
        assertThat(((BigDecimal) row.get("pi_budget")).intValue()).isEqualTo(999999);
    }
}
