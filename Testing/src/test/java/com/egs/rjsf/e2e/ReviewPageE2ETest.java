package com.egs.rjsf.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Review Page E2E Tests")
class ReviewPageE2ETest extends BaseE2ETest {

    @Test
    @Order(1)
    @DisplayName("Login and page loads with award summary")
    void pageLoadsWithAwardSummary() {
        loginAndNavigateToReview();
        String body = bodyText();
        assertThat(body).contains("Pre-Award");
        assertThat(body).contains("Joshua Phipps");
    }

    @Test
    @Order(2)
    @DisplayName("Overview section is expanded by default and shows PI Budget")
    void overviewSectionVisible() {
        loginAndNavigateToReview();
        // Overview starts expanded by default — don't click it (would collapse)
        sleep(500);
        String body = bodyText();
        assertThat(body).contains("PI Budget");
        assertThat(body).contains("Program Manager");
    }

    @Test
    @Order(3)
    @DisplayName("Safety section shows correct question text from POC")
    void safetySectionQuestionsCorrect() {
        loginAndNavigateToReview();
        expandAccordion("Safety Requirements Review");
        sleep(500);
        String body = bodyText();
        assertThat(body).contains("Programmatic Record of Environmental Compliance");
        assertThat(body).contains("Army-provided infectious agents");
        assertThat(body).contains("Biological Select Agents or Toxins");
    }

    @Test
    @Order(4)
    @DisplayName("Animal section shows correct question text")
    void animalSectionQuestionsCorrect() {
        loginAndNavigateToReview();
        expandAccordion("Animal Research Review");
        sleep(500);
        String body = bodyText();
        assertThat(body).contains("Animals used?");
    }

    @Test
    @Order(5)
    @DisplayName("Human Review group header is visible and expandable")
    void humanReviewGroupPresent() {
        loginAndNavigateToReview();
        expandAccordion("Human Research Review");
        sleep(500);
        String body = bodyText();
        // After expanding the group header, subsection titles should appear
        assertThat(body).containsAnyOf(
                "Regulatory Review",
                "Anatomical",
                "Secondary Use",
                "Human Subjects",
                "Special Topics",
                "Estimated Start"
        );
    }

    @Test
    @Order(6)
    @DisplayName("Acquisition group header is visible and expandable")
    void acquisitionGroupPresent() {
        loginAndNavigateToReview();
        expandAccordion("Acquisition/Contracting");
        sleep(500);
        String body = bodyText();
        // After expanding, budget review sub-group or subsection titles should appear
        assertThat(body).containsAnyOf("Budget Review", "Peer", "Statement of Work", "Data Management");
    }

    @Test
    @Order(7)
    @DisplayName("Final Recommendation section shows correct question text")
    void finalRecommendationQuestionsCorrect() {
        loginAndNavigateToReview();
        expandAccordion("Final Recommendation");
        sleep(500);
        String body = bodyText();
        assertThat(body).contains("scientific overlap");
        assertThat(body).contains("RISG");
    }

    @Test
    @Order(8)
    @DisplayName("Logged-in user display name appears in header")
    void userDisplayNameVisible() {
        loginAndNavigateToReview();
        String body = bodyText();
        assertThat(body).contains("Joshua Phipps");
    }
}
