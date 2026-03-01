package com.goaltracker.e2e.util;

import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screenshot ve page source yakalama yardımcısı — Allure raporlarına eklenir.
 */
public final class ScreenshotUtils {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {
        // Utility class
    }

    /**
     * WebDriver'dan ekran görüntüsü alır ve Allure raporuna PNG olarak ekler.
     */
    @Attachment(value = "Screenshot", type = "image/png")
    public static byte[] takeScreenshot(WebDriver driver) {
        if (driver == null) {
            log.warn("WebDriver is null, cannot take screenshot");
            return new byte[0];
        }

        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.error("Failed to take screenshot: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Sayfa kaynağını alır ve Allure raporuna HTML olarak ekler (debug için).
     */
    @Attachment(value = "Page Source", type = "text/html")
    public static String getPageSource(WebDriver driver) {
        if (driver == null) {
            log.warn("WebDriver is null, cannot get page source");
            return "";
        }

        try {
            return driver.getPageSource();
        } catch (Exception e) {
            log.error("Failed to get page source: {}", e.getMessage());
            return "";
        }
    }
}

