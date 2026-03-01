package com.goaltracker.e2e.util;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * WebDriver wait yardımcıları — tüm metotlar {@link WebDriverWait} + {@link ExpectedConditions} kullanır.
 */
public final class WaitUtils {

    private WaitUtils() {
        // Utility class
    }

    /**
     * Elementin DOM'da var olmasını bekler.
     */
    @Step("Wait for element presence: {locator}")
    public static WebElement waitForElement(WebDriver driver, By locator, int timeoutSec) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element not found after %d seconds: %s", timeoutSec, locator), e);
        }
    }

    /**
     * Elementin görünür olmasını bekler.
     */
    @Step("Wait for element visible: {locator}")
    public static WebElement waitForElementVisible(WebDriver driver, By locator, int timeoutSec) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element not visible after %d seconds: %s", timeoutSec, locator), e);
        }
    }

    /**
     * Elementin tıklanabilir olmasını bekler.
     */
    @Step("Wait for element clickable: {locator}")
    public static WebElement waitForElementClickable(WebDriver driver, By locator, int timeoutSec) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element not clickable after %d seconds: %s", timeoutSec, locator), e);
        }
    }

    /**
     * URL'nin belirli bir metin içermesini bekler.
     */
    @Step("Wait for URL to contain: {urlPart}")
    public static void waitForUrlContains(WebDriver driver, String urlPart, int timeoutSec) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.urlContains(urlPart));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("URL did not contain '%s' after %d seconds. Current URL: %s",
                            urlPart, timeoutSec, driver.getCurrentUrl()), e);
        }
    }

    /**
     * Elementin belirli bir metin içermesini bekler.
     */
    @Step("Wait for text '{text}' in element: {locator}")
    public static void waitForTextPresent(WebDriver driver, By locator, String text, int timeoutSec) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Text '%s' not present in element %s after %d seconds",
                            text, locator, timeoutSec), e);
        }
    }

    /**
     * Elementin DOM'dan kalkmasını bekler.
     *
     * @return true eğer element kaybolursa, false timeout olursa
     */
    @Step("Wait for element absent: {locator}")
    public static boolean waitForElementAbsent(WebDriver driver, By locator, int timeoutSec) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Sayfanın tam yüklenmesini bekler (document.readyState === 'complete').
     */
    @Step("Wait for page to fully load")
    public static void waitForPageLoad(WebDriver driver, int timeoutSec) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(d -> ((JavascriptExecutor) d)
                            .executeScript("return document.readyState").equals("complete"));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Page did not fully load after %d seconds", timeoutSec), e);
        }
    }

    /**
     * Birden fazla elementin DOM'da var olmasını bekler.
     */
    @Step("Wait for elements presence: {locator}")
    public static List<WebElement> waitForElements(WebDriver driver, By locator, int timeoutSec) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Elements not found after %d seconds: %s", timeoutSec, locator), e);
        }
    }
}

