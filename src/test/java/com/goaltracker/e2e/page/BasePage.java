package com.goaltracker.e2e.page;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Tüm Page Object'lerin base sınıfı — ortak fluent metotlar sağlar.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final String baseUrl;

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    protected BasePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    /**
     * Belirtilen URL'ye navigasyon yapar.
     */
    public void navigateTo(String url) {
        driver.get(url);
    }

    /**
     * Mevcut URL'yi döndürür.
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Sayfa başlığını döndürür.
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * .alert-success CSS sınıfına sahip flash mesaj metnini döndürür.
     */
    public String getFlashSuccessMessage() {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert-success")));
        return element.getText().trim();
    }

    /**
     * .alert-danger CSS sınıfına sahip flash mesaj metnini döndürür.
     */
    public String getFlashErrorMessage() {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert-danger")));
        return element.getText().trim();
    }

    /**
     * Success flash mesajının görünür olup olmadığını kontrol eder.
     */
    public boolean isFlashSuccessVisible() {
        return isElementPresent(By.cssSelector(".alert-success"));
    }

    /**
     * Error flash mesajının görünür olup olmadığını kontrol eder.
     */
    public boolean isFlashErrorVisible() {
        return isElementPresent(By.cssSelector(".alert-danger"));
    }

    /**
     * Sayfanın tam yüklenmesini bekler (document.readyState === 'complete').
     */
    public void waitForPageLoad() {
        wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
    }

    /**
     * Belirtilen elemente JavaScript ile scroll yapar.
     */
    public void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
    }

    /**
     * Elementi temizleyip yeni değer yazar.
     */
    public void clearAndType(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Select (dropdown) elementinde value'ya göre seçim yapar.
     */
    public void selectDropdownByValue(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        new Select(element).selectByValue(value);
    }

    /**
     * Select (dropdown) elementinde görünen metne göre seçim yapar.
     */
    public void selectDropdownByVisibleText(By locator, String text) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        new Select(element).selectByVisibleText(text);
    }

    /**
     * Elementin DOM'da var olup olmadığını kontrol eder.
     *
     * @return true element varsa, false yoksa
     */
    public boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * URL'nin belirli bir metin içermesini bekler.
     */
    public void waitForUrlContains(String urlPart) {
        wait.until(ExpectedConditions.urlContains(urlPart));
    }
}

