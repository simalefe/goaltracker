package com.goaltracker.e2e.page.component;

import com.goaltracker.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Navbar + Sidebar navigasyon bileşeni — tüm sayfalarda ortak kullanılır.
 * <p>
 * Ana navigasyon linkleri sidebar'da, kullanıcı menüsü ve bildirimler navbar'dadır.
 */
public class NavbarComponent {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Sidebar locators
    private static final By DASHBOARD_LINK = By.cssSelector("#sidebarMenu a[href='/dashboard']");
    private static final By GOALS_LINK = By.cssSelector("#sidebarMenu a[href='/goals']");
    private static final By SOCIAL_LINK = By.cssSelector("#sidebarMenu a[href='/social']");
    private static final By NOTIFICATIONS_LINK = By.cssSelector("#sidebarMenu a[href='/notifications']");
    private static final By PROFILE_LINK = By.cssSelector("#sidebarMenu a[href='/profile']");

    // Navbar locators
    private static final By LOGOUT_FORM = By.cssSelector("form[action='/auth/logout']");
    private static final By LOGOUT_BUTTON = By.cssSelector("form[action='/auth/logout'] button[type='submit']");
    private static final By NOTIFICATION_BADGE = By.id("notificationBadge");
    private static final By NOTIFICATION_COUNT = By.id("notificationCount");
    private static final By NOTIFICATION_BELL = By.id("notificationDropdownToggle");

    // User dropdown
    private static final By USER_DROPDOWN = By.cssSelector(".navbar .nav-link.dropdown-toggle");

    public NavbarComponent(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /**
     * Dashboard sayfasına gider (sidebar üzerinden).
     */
    public void goToDashboard() {
        clickSidebarLink(DASHBOARD_LINK);
    }

    /**
     * Hedefler sayfasına gider (sidebar üzerinden).
     */
    public void goToGoals() {
        clickSidebarLink(GOALS_LINK);
    }

    /**
     * Sosyal sayfasına gider (sidebar üzerinden).
     */
    public void goToSocial() {
        clickSidebarLink(SOCIAL_LINK);
    }

    /**
     * Bildirimler sayfasına gider (sidebar üzerinden).
     */
    public void goToNotifications() {
        clickSidebarLink(NOTIFICATIONS_LINK);
    }

    /**
     * Profil sayfasına gider (sidebar üzerinden).
     */
    public void goToProfile() {
        clickSidebarLink(PROFILE_LINK);
    }

    /**
     * Kullanıcı menüsünden logout yapar.
     */
    public void logout() {
        // Kullanıcı dropdown'ını aç
        wait.until(ExpectedConditions.elementToBeClickable(USER_DROPDOWN)).click();
        // Logout butonuna tıkla
        wait.until(ExpectedConditions.elementToBeClickable(LOGOUT_BUTTON)).click();
    }

    /**
     * Bildirim badge sayısını döndürür.
     *
     * @return bildirim sayısı, badge görünmüyorsa 0
     */
    public int getNotificationBadgeCount() {
        try {
            WebElement badge = driver.findElement(NOTIFICATION_BADGE);
            if (!badge.isDisplayed()) {
                return 0;
            }
            WebElement count = driver.findElement(NOTIFICATION_COUNT);
            String text = count.getText().trim();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NoSuchElementException | NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Bildirim badge'inin görünür olup olmadığını kontrol eder.
     */
    public boolean isNotificationBadgeVisible() {
        try {
            WebElement badge = driver.findElement(NOTIFICATION_BADGE);
            return badge.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Dashboard linkinin var olup olmadığını kontrol eder (kullanıcı giriş yapmış mı?).
     */
    public boolean isLoggedIn() {
        try {
            driver.findElement(DASHBOARD_LINK);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private void clickSidebarLink(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }
}

