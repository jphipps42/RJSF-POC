package com.egs.rjsf.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Base class for Selenium E2E tests.
 *
 * Prerequisites:
 *   - Spring Boot service running on localhost:3001
 *   - React dev server running on localhost:5173
 *   - PostgreSQL with seeded data (user: jphipps / password: test)
 *   - Chrome browser installed
 *
 * Run with: mvn verify -pl Testing -De2e.headless=false
 */
public abstract class BaseE2ETest {

    protected static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:5173");
    protected static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("e2e.headless", "true"));
    protected static final int TIMEOUT = Integer.parseInt(System.getProperty("e2e.timeout", "15"));

    // Seeded test user credentials from init.sql comment: "jphipps / test / SO / CDMRP"
    protected static final String TEST_USERNAME = "jphipps";
    protected static final String TEST_PASSWORD = "test";

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeAll
    static void setupDriver() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void initBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (HEADLESS) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1200");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Logs in and waits for the review page to fully render.
     */
    protected void loginAndNavigateToReview() {
        driver.get(BASE_URL + "/login");

        // Wait for login form
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        // MUI TextFields: find inputs inside the form
        List<WebElement> inputs = driver.findElements(By.cssSelector("form input"));
        WebElement usernameInput = null;
        WebElement passwordInput = null;
        for (WebElement input : inputs) {
            String type = input.getAttribute("type");
            if ("password".equals(type)) {
                passwordInput = input;
            } else if (usernameInput == null) {
                usernameInput = input;
            }
        }

        if (usernameInput == null || passwordInput == null) {
            throw new RuntimeException("Could not find username/password inputs on login page");
        }

        usernameInput.clear();
        usernameInput.sendKeys(TEST_USERNAME);
        passwordInput.clear();
        passwordInput.sendKeys(TEST_PASSWORD);

        // Click Sign In
        WebElement signInBtn = driver.findElement(
                By.xpath("//button[contains(text(), 'Sign In')]"));
        signInBtn.click();

        // Wait for the review page — look for the award summary area
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Pre-Award')]")));
        // Give React a moment to finish rendering
        sleep(1000);
    }

    /**
     * Clicks an MUI accordion header to expand it, scrolling into view first.
     * Waits for the accordion content (AccordionDetails) to appear.
     */
    protected void expandAccordion(String headerText) {
        WebElement header = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), '" + headerText + "')]/ancestor::div[contains(@class, 'MuiAccordionSummary')]"
                        + " | //*[contains(text(), '" + headerText + "')]")));
        scrollTo(header);
        sleep(300);
        jsClick(header);
        // Wait for the accordion body to appear
        sleep(500);
    }

    /**
     * Scrolls an element into the center of the viewport.
     */
    protected void scrollTo(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);
    }

    /**
     * Clicks via JavaScript to avoid "element click intercepted" errors.
     */
    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Returns the full visible text of the page body.
     */
    protected String bodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    protected void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
