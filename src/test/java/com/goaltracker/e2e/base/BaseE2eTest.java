package com.goaltracker.e2e.base;

import com.goaltracker.e2e.config.WebDriverConfig;
import com.goaltracker.e2e.extension.ScreenshotOnFailureExtension;
import com.goaltracker.e2e.factory.UserRegistrationHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

/**
 * Tüm E2E testlerinin base sınıfı.
 * <p>
 * Spring Boot uygulamasını random port ile başlatır, WebDriver oluşturur,
 * ve her test için temiz bir tarayıcı oturumu sağlar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@ExtendWith(ScreenshotOnFailureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseE2eTest {

    private static final Logger log = LoggerFactory.getLogger(BaseE2eTest.class);

    @LocalServerPort
    protected int port;

    @Autowired
    protected WebDriverConfig webDriverConfig;

    @Autowired
    protected UserRegistrationHelper userRegistrationHelper;

    protected WebDriver driver;

    /**
     * Her test öncesi yeni bir WebDriver oluşturur ve base URL'yi ayarlar.
     */
    @BeforeEach
    void setUpWebDriver(ExtensionContext context) {
        log.info("Starting WebDriver for test: {}", context.getDisplayName());
        driver = webDriverConfig.createDriver();

        // ScreenshotOnFailureExtension'ın WebDriver'a erişebilmesi için store'a kaydet
        context.getStore(ScreenshotOnFailureExtension.NAMESPACE)
                .put(ScreenshotOnFailureExtension.WEBDRIVER_KEY, driver);
    }

    /**
     * Her test sonrası WebDriver'ı kapatır.
     */
    @AfterEach
    void tearDownWebDriver() {
        if (driver != null) {
            log.info("Closing WebDriver");
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Error closing WebDriver: {}", e.getMessage());
            }
        }
    }

    /**
     * Uygulamanın base URL'sini döndürür.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Belirtilen path'e navigasyon yapar.
     *
     * @param path slash ile başlayan relative path (örn: "/dashboard")
     */
    protected void navigateTo(String path) {
        String url = getBaseUrl() + path;
        log.debug("Navigating to: {}", url);
        driver.get(url);
    }

    /**
     * Login sayfası üzerinden giriş yapar.
     * Email (username) ve password alanlarını doldurur, submit eder.
     *
     * @param email    kullanıcının email adresi
     * @param password kullanıcının şifresi
     */
    protected void loginAs(String email, String password) {
        navigateTo("/auth/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
                .sendKeys(email);

        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Dashboard'a yönlendirilmesini bekle
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        log.info("Logged in as: {}", email);
    }
}

