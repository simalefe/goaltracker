package com.goaltracker.e2e;

import com.goaltracker.e2e.base.BaseE2eTest;
import com.goaltracker.e2e.factory.TestDataFactory;
import com.goaltracker.e2e.factory.UserRegistrationHelper.TestUser;
import com.goaltracker.e2e.page.component.NavbarComponent;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test — E2E altyapısının doğru çalıştığını doğrular.
 * <ul>
 *   <li>Spring Boot uygulaması başlıyor mu?</li>
 *   <li>WebDriver sayfa açabiliyor mu?</li>
 *   <li>Login sayfası erişilebilir mi?</li>
 *   <li>Kullanıcı oluşturma ve login akışı çalışıyor mu?</li>
 *   <li>Navbar linkleri doğru mu?</li>
 * </ul>
 */
@Feature("Altyapı")
@DisplayName("E2E Smoke Test")
class SmokeE2eTest extends BaseE2eTest {

    @Test
    @Order(1)
    @DisplayName("Login sayfası açılıyor")
    @Description("Uygulama başlatılıp login sayfasının erişilebilir olduğu doğrulanır")
    void loginPageShouldBeAccessible() {
        navigateTo("/auth/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        String title = driver.getTitle();
        assertThat(title).contains("GoalTracker");

        // Email ve şifre alanlarının var olduğunu doğrula
        assertThat(driver.findElement(By.id("username")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("password")).isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Kullanıcı oluşturma ve login")
    @Description("DB'ye kullanıcı yazılıp UI üzerinden login yapılabildiği doğrulanır")
    void shouldCreateUserAndLogin() {
        // DB'ye verified kullanıcı yaz
        TestUser user = userRegistrationHelper.createVerifiedUser();

        // Login yap
        loginAs(user.email(), user.password());

        // Dashboard'a yönlendirildiğini doğrula
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    @Test
    @Order(3)
    @DisplayName("Navbar linkleri mevcut")
    @Description("Giriş yapıldıktan sonra sidebar ve navbar linklerinin görüntülendiği doğrulanır")
    void navbarLinksShouldBePresent() {
        TestUser user = userRegistrationHelper.createVerifiedUser();
        loginAs(user.email(), user.password());

        NavbarComponent navbar = new NavbarComponent(driver);
        assertThat(navbar.isLoggedIn()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("TestDataFactory rastgele veri üretiyor")
    @Description("TestDataFactory'nin benzersiz veriler ürettiği doğrulanır")
    void testDataFactoryShouldGenerateUniqueData() {
        String email1 = TestDataFactory.randomEmail();
        String email2 = TestDataFactory.randomEmail();
        assertThat(email1).isNotEqualTo(email2);
        assertThat(email1).endsWith("@goaltracker.com");

        String username1 = TestDataFactory.randomUsername();
        String username2 = TestDataFactory.randomUsername();
        assertThat(username1).isNotEqualTo(username2);
        assertThat(username1).startsWith("user-");

        assertThat(TestDataFactory.randomPassword()).isEqualTo("Test1234!");
        assertThat(TestDataFactory.today()).isNotNull();
        assertThat(TestDataFactory.futureDate(7)).isAfter(TestDataFactory.today());
        assertThat(TestDataFactory.pastDate(7)).isBefore(TestDataFactory.today());
    }
}

