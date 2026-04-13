package com.egs.rjsf.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
        // The page should show the award header area — check for known content
        String body = bodyText();
        assertThat(body).contains("Pre-Award");
        assertThat(body).contains("Joshua Phipps");
    }

    @Test
    @Order(2)
    @DisplayName("Overview section accordion is visible and expandable")
    void overviewSectionVisible() {
        loginAndNavigateToReview();
        expandAccordion("Pre-Award Overview");
        sleep(500);
        String body = bodyText();
        assertThat(body).contains("PI Budget");
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
        assertThat(body).contains("specific chemical agents");
        assertThat(body).contains("pesticides outside of established lab");
        assertThat(body).contains("significant negative effects");
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
    @DisplayName("Human Review subsections are visible")
    void humanReviewSubsectionsPresent() {
        loginAndNavigateToReview();
        expandAccordion("Human Research Review");
        sleep(500);
        String body = bodyText();
        // Check for subsection titles visible after expanding the group header
        assertThat(body).containsAnyOf(
                "NOT Requiring Regulatory Review",
                "Human Anatomical",
                "Secondary Use",
                "Human Subjects",
                "Special Topics",
                "Estimated Start"
        );
    }

    @Test
    @Order(6)
    @DisplayName("Acquisition subsections are visible")
    void acquisitionSubsectionsPresent() {
        loginAndNavigateToReview();
        expandAccordion("Acquisition/Contracting Review");
        sleep(500);
        String body = bodyText();
        assertThat(body).containsAnyOf("Personnel", "Equipment", "Travel", "Materials");
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
