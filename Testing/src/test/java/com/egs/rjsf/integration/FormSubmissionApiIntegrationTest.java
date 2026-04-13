package com.egs.rjsf.integration;

import com.egs.rjsf.RjsfFormServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests against the already-running PostgreSQL instance (localhost:5432).
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
@DisplayName("Form Submission API Integration Tests")
class FormSubmissionApiIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    private static String submissionId;

    @Test
    @Order(1)
    @DisplayName("Fetch seeded submission ID via award lookup")
    void fetchSubmissionId() throws Exception {
        MvcResult result = mvc.perform(get("/api/awards/by-log/TE020005"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        submissionId = root.get("submissions").get(0).get("id").asText();
        Assertions.assertNotNull(submissionId);
    }

    @Test
    @Order(2)
    @DisplayName("PUT /save?section=overview saves draft and sets section status")
    void saveDraftOverview() throws Exception {
        String body = """
                {
                    "form_data": {
                        "pi_budget": 1500000,
                        "program_manager": "Integration Test PM",
                        "prime_award_type": "extramural"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.section_status.overview").value("in_progress"))
                .andExpect(jsonPath("$.form_data.pi_budget").value(1500000))
                .andExpect(jsonPath("$.form_data.program_manager").value("Integration Test PM"));
    }

    @Test
    @Order(3)
    @DisplayName("PUT /save?section=safety_review saves safety data (resets if locked)")
    void saveDraftSafety() throws Exception {
        // Reset safety section in case a prior test run submitted it
        mvc.perform(put("/api/form-submissions/{id}/reset", submissionId)
                .param("section", "safety_review"));

        String body = """
                {
                    "form_data": {
                        "safety_q1": "yes",
                        "safety_q2": "no",
                        "safety_q3": "no"
                    }
                }
                """;

        mvc.perform(put("/api/form-submissions/{id}/save", submissionId)
                        .param("section", "safety_review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section_status.safety_review").value("in_progress"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /form-submissions/{id} returns submission with schema")
    void getSubmissionById() throws Exception {
        mvc.perform(get("/api/form-submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId))
                .andExpect(jsonPath("$.form_key").value("pre_award_composite"))
                .andExpect(jsonPath("$.json_schema").isNotEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("GET /form-submissions/{id} returns 404 for unknown ID")
    void getSubmissionNotFound() throws Exception {
        mvc.perform(get("/api/form-submissions/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
