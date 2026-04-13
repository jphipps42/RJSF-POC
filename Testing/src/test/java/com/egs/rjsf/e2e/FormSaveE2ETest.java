package com.egs.rjsf.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Form Save/Submit E2E Tests")
class FormSaveE2ETest extends BaseE2ETest {

    private void clickFirstRadio(String value) {
        List<WebElement> radios = driver.findElements(
                By.cssSelector("input[type='radio'][value='" + value + "']"));
        for (WebElement r : radios) {
            try {
                scrollTo(r);
                sleep(200);
                jsClick(r);
                return;
            } catch (Exception ignored) {}
        }
    }

    private void clickSaveDraft() {
        List<WebElement> saveBtns = driver.findElements(
                By.xpath("//button[contains(text(), 'Save Draft')]"));
        for (WebElement btn : saveBtns) {
            if (btn.isDisplayed()) {
                scrollTo(btn);
                sleep(200);
                jsClick(btn);
                return;
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Save Draft button appears in Safety section")
    void saveDraftButtonPresent() {
        loginAndNavigateToReview();
        expandAccordion("Safety Requirements Review");
        sleep(500);

        List<WebElement> saveBtns = driver.findElements(
                By.xpath("//button[contains(text(), 'Save Draft')]"));
        assertThat(saveBtns).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Save shows success notification")
    void saveShowsSuccessNotification() {
        loginAndNavigateToReview();
        expandAccordion("Safety Requirements Review");
        sleep(500);

        // Click a radio to dirty the form
        clickFirstRadio("yes");
        sleep(300);

        // Click Save Draft
        clickSaveDraft();

        // Wait for success notification
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'saved') or contains(text(), 'Saved') or contains(text(), 'success')]")));
    }

    @Test
    @Order(3)
    @DisplayName("Submit button shows confirmation dialog")
    void submitShowsConfirmation() {
        loginAndNavigateToReview();
        expandAccordion("Safety Requirements Review");
        sleep(500);

        List<WebElement> submitBtns = driver.findElements(
                By.xpath("//button[contains(text(), 'Submit to Safety')]"));

        if (!submitBtns.isEmpty()) {
            scrollTo(submitBtns.get(0));
            sleep(200);
            jsClick(submitBtns.get(0));

            // Confirmation dialog should appear
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(text(), 'Confirm')]")));
            String body = bodyText();
            assertThat(body).contains("Confirm");

            // Cancel to avoid actually submitting
            List<WebElement> cancelBtns = driver.findElements(
                    By.xpath("//button[contains(text(), 'Cancel')]"));
            if (!cancelBtns.isEmpty()) {
                jsClick(cancelBtns.get(0));
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Data persists across page reload")
    void dataPersistsAcrossReload() {
        loginAndNavigateToReview();
        expandAccordion("Safety Requirements Review");
        sleep(500);

        // Click a yes radio
        clickFirstRadio("yes");
        sleep(300);

        // Save
        clickSaveDraft();
        sleep(1500);

        // Reload — localStorage preserves login session
        driver.navigate().refresh();
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Pre-Award')]")));
        sleep(1000);

        expandAccordion("Safety Requirements Review");
        sleep(500);

        // Verify a radio is checked
        List<WebElement> checkedRadios = driver.findElements(
                By.cssSelector("input[type='radio']:checked"));
        assertThat(checkedRadios).isNotEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("Unauthenticated access redirects to login page")
    void unauthenticatedRedirectsToLogin() {
        driver.get(BASE_URL + "/review/TE020005");
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(text(), 'Sign In')]")));
        assertThat(driver.getCurrentUrl()).contains("/login");
    }
}
