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
 * End-to-end integration tests for the MAPPER ↔ POJO sync mode toggle.
 * Tests the full flow: REST toggle → form save → relational table verification.
 * Requires: docker-compose up (PostgreSQL with seeded data).
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
@DisplayName("Sync Mode Toggle — End-to-End Integration Tests")
class SyncModeToggleIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static String submissionId;
    private static String awardId;

    // ================================================================
    //  Setup
    // ================================================================

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

        // Ensure we start in MAPPER mode
        mvc.perform(put("/api/sync-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"MAPPER\"}"))
                .andExpect(status().isOk());
    }

    // ================================================================
    //  Sync Mode API Tests
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("GET /api/sync-mode returns current mode")
    void getSyncModeReturnsCurrentMode() throws Exception {
        mvc.perform(get("/api/sync-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("MAPPER"));
    }

    @Test
    @Order(3)
    @DisplayName("PUT /api/sync-mode switches to POJO")
    void putSyncModeSwitchesToPojo() throws Exception {
        mvc.perform(put("/api/sync-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"POJO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("POJO"));

        // Verify it persists
        mvc.perform(get("/api/sync-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("POJO"));
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/sync-mode switches back to MAPPER")
    void putSyncModeSwitchesBackToMapper() throws Exception {
        mvc.perform(put("/api/sync-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"MAPPER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("MAPPER"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/sync-mode rejects unknown mode")
    void putSyncModeRejectsUnknown() throws Exception {
        mvc.perform(put("/api/sync-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"INVALID\"}"))
                .andExpect(status().is4xxClientError());
    }

    // ================================================================
    //  MAPPER Mode — Write Path
    // ================================================================

    @Test
    @Order(10)
    @DisplayName("MAPPER: overview save writes to relational table via transformer pipeline")
    void mapperOverviewSync() throws Exception {
        // Ensure MAPPER mode
        mvc.perform(put("/api/sync-mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"MAPPER\"}"));

        // Reset section
        mvc.perform(put("/api/form-submissions/{id}/reset", submissionId)
                .param("section", "overview"));

        String body = """
                {
                    "form_data": {
                        "pi_budget": 111111,
                        "program_manager": "Mapper PM"
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

        assertThat(((BigDecimal) row.get("pi_budget")).intValue()).isEqualTo(111111);
        assertThat(row.get("program_manager")).isEqualTo("Mapper PM");
    }

    @Test
    @Order(11)
    @DisplayName("MAPPER: safety save writes to relational table via transformer pipeline")
    void mapperSafetySync() throws Exception {
        mvc.perform(put("/api/form-submissions/{id}/reset", submissionId)
                .param("section", "safety_review"));

        String body = """
                {
                    "form_data": {
                        "safety_q1": "yes",
                        "safety_notes": "Mapper note"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "safety_review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT safety_q1, safety_notes FROM form_pre_award_safety WHERE award_id = ?::uuid",
                awardId);

        assertThat(row.get("safety_q1")).isEqualTo("yes");
        assertThat(row.get("safety_notes")).isEqualTo("Mapper note");
    }

    // ================================================================
    //  POJO Mode — Write Path
    // ================================================================

    @Test
    @Order(20)
    @DisplayName("POJO: overview save writes to relational table via JPA entities")
    void pojoOverviewSync() throws Exception {
        // Switch to POJO
        mvc.perform(put("/api/sync-mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"POJO\"}"));

        String body = """
                {
                    "form_data": {
                        "pi_budget": 222222,
                        "program_manager": "POJO PM"
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

        assertThat(((BigDecimal) row.get("pi_budget")).intValue()).isEqualTo(222222);
        assertThat(row.get("program_manager")).isEqualTo("POJO PM");
    }

    @Test
    @Order(21)
    @DisplayName("POJO: safety save writes to relational table via JPA entities")
    void pojoSafetySync() throws Exception {
        String body = """
                {
                    "form_data": {
                        "safety_q1": "no",
                        "safety_q3": "yes",
                        "safety_notes": "POJO note"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "safety_review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT safety_q1, safety_q3, safety_notes FROM form_pre_award_safety WHERE award_id = ?::uuid",
                awardId);

        assertThat(row.get("safety_q1")).isEqualTo("no");
        assertThat(row.get("safety_q3")).isEqualTo("yes");
        assertThat(row.get("safety_notes")).isEqualTo("POJO note");
    }

    @Test
    @Order(22)
    @DisplayName("POJO: human subsection save does not clobber other subsections")
    void pojoHumanSectionIsolation() throws Exception {
        // Save human_no_regulatory first
        mvc.perform(put("/api/form-submissions/{id}/reset", submissionId)
                .param("section", "human_no_regulatory"));

        String noRegBody = """
                {
                    "form_data": {
                        "no_review_default_no": true,
                        "human_s1_q1": "no"
                    }
                }
                """;
        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "human_no_regulatory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noRegBody))
                .andExpect(status().isOk());

        // Now save human_anatomical
        mvc.perform(put("/api/form-submissions/{id}/reset", submissionId)
                .param("section", "human_anatomical"));

        String anatBody = """
                {
                    "form_data": {
                        "has_default_no": false,
                        "human_has_q1": "yes"
                    }
                }
                """;
        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "human_anatomical")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(anatBody))
                .andExpect(status().isOk());

        // Verify both subsections' data is present — anatomical didn't clobber no_regulatory
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT no_review_default_no, human_s1_q1, has_default_no, human_has_q1 " +
                        "FROM form_pre_award_human WHERE award_id = ?::uuid",
                awardId);

        assertThat(row.get("no_review_default_no")).isEqualTo(true);
        assertThat(row.get("human_s1_q1")).isEqualTo("no");
        assertThat(row.get("has_default_no")).isEqualTo(false);
        assertThat(row.get("human_has_q1")).isEqualTo("yes");
    }

    @Test
    @Order(23)
    @DisplayName("POJO: final recommendation save writes to relational table")
    void pojoFinalSync() throws Exception {
        mvc.perform(put("/api/form-submissions/{id}/reset", submissionId)
                .param("section", "final_recommendation"));

        String body = """
                {
                    "form_data": {
                        "scientific_overlap": "no",
                        "so_recommendation": "approval",
                        "gor_comments": "Approved via POJO"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "final_recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT scientific_overlap, so_recommendation, gor_comments " +
                        "FROM form_pre_award_final WHERE award_id = ?::uuid",
                awardId);

        assertThat(row.get("scientific_overlap")).isEqualTo("no");
        assertThat(row.get("so_recommendation")).isEqualTo("approval");
        assertThat(row.get("gor_comments")).isEqualTo("Approved via POJO");
    }

    // ================================================================
    //  Toggle Mid-Session — Both Paths Write to Same Table
    // ================================================================

    @Test
    @Order(30)
    @DisplayName("Toggle: MAPPER writes, then POJO updates the same row")
    void toggleMapperThenPojo() throws Exception {
        // Step 1: Write with MAPPER
        mvc.perform(put("/api/sync-mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"MAPPER\"}"));

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"form_data\":{\"pi_budget\":333333,\"program_manager\":\"Mapper First\"}}"))
                .andExpect(status().isOk());

        // Verify MAPPER wrote
        Map<String, Object> row1 = jdbcTemplate.queryForMap(
                "SELECT pi_budget, program_manager FROM form_pre_award_overview WHERE award_id = ?::uuid",
                awardId);
        assertThat(((BigDecimal) row1.get("pi_budget")).intValue()).isEqualTo(333333);
        assertThat(row1.get("program_manager")).isEqualTo("Mapper First");

        // Step 2: Toggle to POJO and update the same row
        mvc.perform(put("/api/sync-mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"POJO\"}"));

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"form_data\":{\"pi_budget\":444444,\"program_manager\":\"POJO Second\"}}"))
                .andExpect(status().isOk());

        // Verify POJO updated the same row (still 1 row, not 2)
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM form_pre_award_overview WHERE award_id = ?::uuid",
                Integer.class, awardId);
        assertThat(count).isEqualTo(1);

        Map<String, Object> row2 = jdbcTemplate.queryForMap(
                "SELECT pi_budget, program_manager FROM form_pre_award_overview WHERE award_id = ?::uuid",
                awardId);
        assertThat(((BigDecimal) row2.get("pi_budget")).intValue()).isEqualTo(444444);
        assertThat(row2.get("program_manager")).isEqualTo("POJO Second");
    }

    @Test
    @Order(31)
    @DisplayName("Toggle: POJO writes, then MAPPER updates the same row")
    void togglePojoThenMapper() throws Exception {
        // Step 1: Write with POJO
        mvc.perform(put("/api/sync-mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"POJO\"}"));

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "safety_review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"form_data\":{\"safety_q1\":\"yes\",\"safety_notes\":\"POJO wrote this\"}}"))
                .andExpect(status().isOk());

        // Step 2: Toggle to MAPPER and overwrite
        mvc.perform(put("/api/sync-mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"MAPPER\"}"));

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "safety_review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"form_data\":{\"safety_q1\":\"no\",\"safety_notes\":\"Mapper overwrote\"}}"))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT safety_q1, safety_notes FROM form_pre_award_safety WHERE award_id = ?::uuid",
                awardId);

        assertThat(row.get("safety_q1")).isEqualTo("no");
        assertThat(row.get("safety_notes")).isEqualTo("Mapper overwrote");
    }

    // ================================================================
    //  Cleanup: restore MAPPER mode
    // ================================================================

    @Test
    @Order(99)
    @DisplayName("Cleanup: restore MAPPER mode")
    void cleanup() throws Exception {
        mvc.perform(put("/api/sync-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"MAPPER\"}"))
                .andExpect(status().isOk());
    }
}
