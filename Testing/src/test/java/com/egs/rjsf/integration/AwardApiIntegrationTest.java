package com.egs.rjsf.integration;

import com.egs.rjsf.RjsfFormServiceApplication;
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
@DisplayName("Award API Integration Tests")
class AwardApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @Order(1)
    @DisplayName("GET /api/awards returns seeded award list")
    void listAwards() throws Exception {
        mvc.perform(get("/api/awards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].log_number").value("TE020005"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/awards/by-log/{logNumber} returns award with submissions and personnel")
    void getAwardByLogNumber() throws Exception {
        mvc.perform(get("/api/awards/by-log/TE020005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log_number").value("TE020005"))
                .andExpect(jsonPath("$.principal_investigator").value("Bill Jones"))
                .andExpect(jsonPath("$.submissions", hasSize(1)))
                .andExpect(jsonPath("$.submissions[0].form_key").value("pre_award_composite"))
                .andExpect(jsonPath("$.personnel", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/awards/by-log/{logNumber} returns 404 for unknown log")
    void getAwardNotFound() throws Exception {
        mvc.perform(get("/api/awards/by-log/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/login succeeds with seeded user jphipps/test")
    void loginWithSeededUser() throws Exception {
        String body = """
                { "username": "jphipps", "password": "test" }
                """;

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jphipps"))
                .andExpect(jsonPath("$.role").value("SO"))
                .andExpect(jsonPath("$.display_name").value("Joshua Phipps"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/auth/login fails with wrong password")
    void loginWithWrongPassword() throws Exception {
        String body = """
                { "username": "jphipps", "password": "wrong" }
                """;

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
