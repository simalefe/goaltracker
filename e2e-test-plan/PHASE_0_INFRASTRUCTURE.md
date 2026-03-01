# ⚙️ Faz 0 — Altyapı & Base Setup

| Alan | Değer |
|------|-------|
| Süre | 1-2 gün |
| Bağımlılık | Mevcut proje derlenebilir durumda olmalı |
| Hedef | E2E test altyapısının sıfırdan kurulması: WebDriver, Allure, Base sınıflar, yardımcı araçlar |

---

## 📋 Yapılacaklar

### 1. Maven Bağımlılıkları (`pom.xml`)

Aşağıdaki bağımlılıklar `pom.xml`'e eklenecek:

```xml
<!-- Selenium WebDriver -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.18.1</version>
    <scope>test</scope>
</dependency>

<!-- WebDriverManager — otomatik driver yönetimi -->
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>

<!-- Allure JUnit 5 -->
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit5</artifactId>
    <version>2.25.0</version>
    <scope>test</scope>
</dependency>

<!-- Allure Selenide (screenshot desteği) -->
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-java-commons</artifactId>
    <version>2.25.0</version>
    <scope>test</scope>
</dependency>
```

Maven plugin (Allure raporlama):
```xml
<plugin>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-maven</artifactId>
    <version>2.12.0</version>
    <configuration>
        <reportVersion>2.25.0</reportVersion>
    </configuration>
</plugin>
```

Maven profili (E2E testlerini ayırma):
```xml
<profile>
    <id>e2e</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <includes>
                        <include>**/*E2eTest.java</include>
                    </includes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### 2. Test Profili (`application-e2e.yml`)

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///goaltrackertest
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: create-drop
  thymeleaf:
    cache: false

jwt:
  secret: e2eTestSecretKeyAtLeast256BitsLong...
  access-expiration: 3600000    # 1 saat (testler için uzun)
  refresh-expiration: 86400000  # 1 gün

e2e:
  browser: chrome
  headless: true
  timeout:
    implicit: 5
    explicit: 10
    page-load: 30
```

---

## 📁 Oluşturulacak Dosyalar

### 2.1 `WebDriverConfig.java` — WebDriver Fabrikası

**Dosya:** `src/test/java/com/goaltracker/e2e/config/WebDriverConfig.java`

**Sorumluluk:**
- `e2e.browser` property'sine göre Chrome veya Firefox driver oluşturma
- `e2e.headless` property'sine göre headless mod ayarlama
- WebDriverManager ile otomatik driver yönetimi
- Chrome options: `--no-sandbox`, `--disable-dev-shm-usage`, `--window-size=1920,1080`
- Download dizini ayarı (export testleri için): `download.default_directory`
- Implicit wait timeout ayarı

**Metotlar:**
```java
public WebDriver createDriver()
public ChromeOptions getChromeOptions()
public FirefoxOptions getFirefoxOptions()
```

---

### 2.2 `BaseE2eTest.java` — Tüm E2E Testlerin Base Sınıfı

**Dosya:** `src/test/java/com/goaltracker/e2e/base/BaseE2eTest.java`

**Sorumluluk:**
- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
- `@ActiveProfiles("e2e")`
- `@ExtendWith(ScreenshotOnFailureExtension.class)`
- `@BeforeEach`: WebDriver oluştur, base URL ayarla
- `@AfterEach`: WebDriver kapat
- `@LocalServerPort` ile dinamik port injection
- `getBaseUrl()` → `http://localhost:{port}` döndürür
- `loginAs(String email, String password)` → Login page üzerinden giriş yapar (sık kullanılan yardımcı)
- `navigateTo(String path)` → Tam URL oluşturup navigasyon yapar

**Anotasyonlar:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@ExtendWith(ScreenshotOnFailureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
```

---

### 2.3 `BasePage.java` — Tüm Page Object'lerin Base Sınıfı

**Dosya:** `src/test/java/com/goaltracker/e2e/page/BasePage.java`

**Sorumluluk:**
- `WebDriver` ve `WebDriverWait` referansı tutar
- Ortak fluent metotlar:
  - `navigateTo(String url)` — sayfa navigasyonu
  - `getCurrentUrl()` — mevcut URL
  - `getPageTitle()` — sayfa başlığı
  - `getFlashSuccessMessage()` — `.alert-success` text
  - `getFlashErrorMessage()` — `.alert-danger` text
  - `isFlashSuccessVisible()` — success mesajı görünür mü
  - `isFlashErrorVisible()` — error mesajı görünür mü
  - `waitForPageLoad()` — sayfa tam yüklenmesini bekle
  - `scrollToElement(WebElement)` — elemente kaydır
  - `clearAndType(WebElement, String)` — clear + sendKeys
  - `selectDropdownByValue(By, String)` — Select element value seçimi
  - `selectDropdownByVisibleText(By, String)` — Select element text seçimi
  - `isElementPresent(By)` — element var mı kontrolü (NoSuchElementException yakalanır)
  - `waitForUrlContains(String)` — URL belirli text içerene kadar bekle

**Design Pattern:**
```java
public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final String baseUrl;

    protected BasePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // Fluent API: her metot this döndürür (veya yeni page)
}
```

---

### 2.4 `NavbarComponent.java` — Navigasyon Bileşeni

**Dosya:** `src/test/java/com/goaltracker/e2e/page/component/NavbarComponent.java`

**Locator'lar:**
| Element | Locator Stratejisi |
|---------|-------------------|
| Dashboard linki | `a[href='/dashboard']` veya link text "Dashboard" |
| Hedefler linki | `a[href='/goals']` |
| Sosyal linki | `a[href='/social']` |
| Bildirimler linki | `a[href='/notifications']` |
| Profil linki | `a[href='/profile']` |
| Logout butonu | Form action `/logout` |
| Bildirim badge | `.badge` notification icon yanında |

**Metotlar:**
```java
DashboardPage goToDashboard()
GoalListPage goToGoals()
SocialPage goToSocial()
NotificationListPage goToNotifications()
ProfilePage goToProfile()
LoginPage logout()
int getNotificationBadgeCount()
boolean isNotificationBadgeVisible()
```

---

### 2.5 `WaitUtils.java` — Wait Yardımcıları

**Dosya:** `src/test/java/com/goaltracker/e2e/util/WaitUtils.java`

**Metotlar:**
```java
static WebElement waitForElement(WebDriver driver, By locator, int timeoutSec)
static WebElement waitForElementVisible(WebDriver driver, By locator, int timeoutSec)
static WebElement waitForElementClickable(WebDriver driver, By locator, int timeoutSec)
static void waitForUrlContains(WebDriver driver, String urlPart, int timeoutSec)
static void waitForTextPresent(WebDriver driver, By locator, String text, int timeoutSec)
static boolean waitForElementAbsent(WebDriver driver, By locator, int timeoutSec)
static void waitForPageLoad(WebDriver driver, int timeoutSec)
static List<WebElement> waitForElements(WebDriver driver, By locator, int timeoutSec)
```

**Best Practice:**
- Tüm metotlar `WebDriverWait` + `ExpectedConditions` kullanır
- `TimeoutException` anlamlı mesajla fırlatılır
- `@Step` (Allure) anotasyonu ile raporlamaya eklenir

---

### 2.6 `ScreenshotUtils.java` — Screenshot Yardımcısı

**Dosya:** `src/test/java/com/goaltracker/e2e/util/ScreenshotUtils.java`

**Metotlar:**
```java
@Attachment(value = "Screenshot", type = "image/png")
static byte[] takeScreenshot(WebDriver driver)

@Attachment(value = "Page Source", type = "text/html")
static String getPageSource(WebDriver driver)
```

---

### 2.7 `ScreenshotOnFailureExtension.java` — JUnit 5 Extension

**Dosya:** `src/test/java/com/goaltracker/e2e/extension/ScreenshotOnFailureExtension.java`

**Sorumluluk:**
- `TestWatcher` implement eder
- `testFailed()` event'inde:
  1. WebDriver'dan screenshot al
  2. Allure'a `@Attachment` olarak ekle
  3. Page source'u da ekle (debug için)
- `testSuccessful()` event'inde opsiyonel log

---

### 2.8 `TestDataFactory.java` — Test Verisi Üretici

**Dosya:** `src/test/java/com/goaltracker/e2e/factory/TestDataFactory.java`

**Metotlar:**
```java
static String randomEmail()         // "test-{uuid}@goaltracker.com"
static String randomUsername()      // "user-{uuid8}"
static String randomPassword()     // "Test1234!" (sabit güvenli şifre)
static String randomDisplayName()  // "Test User {uuid4}"
static String randomGoalTitle()    // "Hedef-{uuid4}"
static LocalDate today()
static LocalDate futureDate(int daysFromNow)
static LocalDate pastDate(int daysAgo)
```

---

### 2.9 `UserRegistrationHelper.java` — Hızlı Kullanıcı Setup

**Dosya:** `src/test/java/com/goaltracker/e2e/factory/UserRegistrationHelper.java`

**Sorumluluk:**
- Spring `JdbcTemplate` veya `UserRepository` + `PasswordEncoder` inject ederek doğrudan DB'ye kullanıcı yazar
- Email verification'ı otomatik olarak `true` yapar
- UI'a girmeden saniyeler içinde kullanıcı oluşturur (test hızı için)
- Her test izole kullanıcı ile çalışır

**Metotlar:**
```java
TestUser createVerifiedUser()                    // Rastgele verified user
TestUser createVerifiedUser(String email, String username, String password)
void deleteUser(Long userId)
Long createGoalForUser(Long userId, String title, ...)
void createEntryForGoal(Long goalId, LocalDate date, BigDecimal value)
```

**`TestUser` record:**
```java
record TestUser(Long id, String email, String username, String password, String displayName) {}
```

---

### 2.10 `AllureUtils.java` — Allure Yardımcı

**Dosya:** `src/test/java/com/goaltracker/e2e/util/AllureUtils.java`

**Metotlar:**
```java
static void addDescription(String description)
static void addStep(String stepName)
static void addLink(String name, String url)
```

---

### 2.11 `allure.properties`

**Dosya:** `src/test/resources/allure.properties`

```properties
allure.results.directory=target/allure-results
allure.link.issue.pattern=https://github.com/goaltracker/issues/{}
allure.link.tms.pattern=https://goaltracker.atlassian.net/browse/{}
```

---

## ✅ Faz 0 Tamamlama Kriterleri

- [ ] Tüm Maven bağımlılıkları eklendi ve `mvn compile -Pe2e` başarılı
- [ ] `BaseE2eTest` Spring Boot uygulama başlatıyor (random port)
- [ ] WebDriver Chrome headless başarıyla açılıyor
- [ ] Basit bir "smoke" test yazılıp çalıştırıldı (sayfa açılıyor mu?)
- [ ] Allure raporu oluşturuluyor (`mvn allure:serve`)
- [ ] Test failure'da otomatik screenshot Allure raporuna ekleniyor
- [ ] `TestDataFactory` rastgele veri üretiyor
- [ ] `UserRegistrationHelper` DB'ye kullanıcı yazabiliyor
- [ ] `NavbarComponent` tüm linkleri doğru buluyor

---

## 📁 Dosya Listesi

```
pom.xml (güncelleme)
src/test/resources/application-e2e.yml
src/test/resources/allure.properties
src/test/java/com/goaltracker/e2e/
    config/WebDriverConfig.java
    base/BaseE2eTest.java
    extension/ScreenshotOnFailureExtension.java
    util/WaitUtils.java
    util/ScreenshotUtils.java
    util/AllureUtils.java
    factory/TestDataFactory.java
    factory/UserRegistrationHelper.java
    page/BasePage.java
    page/component/NavbarComponent.java
```

