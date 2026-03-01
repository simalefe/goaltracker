package com.goaltracker.e2e.extension;

import com.goaltracker.e2e.util.ScreenshotUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * JUnit 5 TestWatcher extension — test başarısız olduğunda otomatik screenshot alır
 * ve Allure raporuna ekler.
 */
public class ScreenshotOnFailureExtension implements TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotOnFailureExtension.class);

    /**
     * ExtensionContext Store namespace — WebDriver referansını paylaşmak için.
     */
    public static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ScreenshotOnFailureExtension.class);

    public static final String WEBDRIVER_KEY = "webDriver";

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testName = context.getDisplayName();
        log.error("Test FAILED: {} — Reason: {}", testName, cause.getMessage());

        getWebDriver(context).ifPresent(driver -> {
            log.info("Capturing screenshot for failed test: {}", testName);
            ScreenshotUtils.takeScreenshot(driver);
            ScreenshotUtils.getPageSource(driver);
        });
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        log.info("Test PASSED: {}", context.getDisplayName());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        log.warn("Test ABORTED: {} — Reason: {}", context.getDisplayName(), cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        log.info("Test DISABLED: {} — Reason: {}", context.getDisplayName(), reason.orElse("N/A"));
    }

    /**
     * ExtensionContext Store'dan WebDriver referansını alır.
     * BaseE2eTest bu store'a WebDriver'ı kaydetmelidir.
     */
    private Optional<WebDriver> getWebDriver(ExtensionContext context) {
        // Önce test instance'ından deneyelim
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        WebDriver driver = store.get(WEBDRIVER_KEY, WebDriver.class);

        if (driver == null) {
            // Parent context'ten dene
            Optional<ExtensionContext> parent = context.getParent();
            if (parent.isPresent()) {
                driver = parent.get().getStore(NAMESPACE).get(WEBDRIVER_KEY, WebDriver.class);
            }
        }

        return Optional.ofNullable(driver);
    }
}

