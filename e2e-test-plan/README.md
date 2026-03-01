# 🧪 GoalTracker Pro — Selenium E2E Test Otomasyon Planı

| Alan | Değer |
|------|-------|
| Proje | GoalTracker Pro |
| Test Türü | End-to-End (E2E) UI Otomasyon |
| Framework | Selenium WebDriver + JUnit 5 |
| Raporlama | Allure Report |
| Dil | Java 21 |
| Build | Maven |
| Mimari | Page Object Model (POM) + Fluent API |
| Tarayıcı | Chrome (varsayılan) + Firefox (opsiyonel) |
| CI | Headless mod varsayılan |

---

## 📋 Faz Listesi

| Faz | Başlık | Kapsam | Tahmini Test Sayısı |
|-----|--------|--------|---------------------|
| [Faz 0](PHASE_0_INFRASTRUCTURE.md) | Altyapı & Base Setup | WebDriver config, base test class, Allure, Page Object base, test data factory, utilities | — |
| [Faz 1](PHASE_1_AUTHENTICATION.md) | Authentication E2E Testleri | Login, Register, Email Verification, Forgot/Reset Password, Logout | ~15 test |
| [Faz 2](PHASE_2_DASHBOARD.md) | Dashboard E2E Testleri | Dashboard metrikleri, boş durum, navigasyon | ~10 test |
| [Faz 3](PHASE_3_GOAL_MANAGEMENT.md) | Hedef Yönetimi E2E Testleri | Goal CRUD, filtreleme, sıralama, pagination, durum güncelleme, export | ~18 test |
| [Faz 4](PHASE_4_PROGRESS_TRACKING.md) | İlerleme Takibi E2E Testleri | Entry CRUD, istatistik doğrulama, chart, dashboard yansıması | ~13 test |
| [Faz 5](PHASE_5_SOCIAL.md) | Sosyal Özellikler E2E Testleri | Arkadaşlık, lider tablosu, aktivite akışı, kullanıcı arama | ~14 test |
| [Faz 6](PHASE_6_NOTIFICATIONS_PROFILE.md) | Bildirimler & Profil E2E Testleri | Bildirim listesi, ayarlar, profil, rozetler | ~15 test |

**Toplam Tahmini Test Sayısı:** ~85 E2E test case

---

## 🏗️ Mimari Genel Bakış

```
src/test/java/com/goaltracker/e2e/
├── config/
│   └── WebDriverConfig.java          # WebDriver fabrikası
├── base/
│   └── BaseE2eTest.java              # Tüm E2E testlerin base sınıfı
├── extension/
│   └── ScreenshotOnFailureExtension.java  # JUnit 5 TestWatcher
├── util/
│   ├── WaitUtils.java                # Explicit wait yardımcıları
│   ├── ScreenshotUtils.java          # Allure screenshot entegrasyonu
│   └── AllureUtils.java              # Allure step/description helper
├── factory/
│   ├── TestDataFactory.java          # Rastgele test verisi üretici
│   └── UserRegistrationHelper.java   # Hızlı kullanıcı setup (DB direct)
├── page/
│   ├── BasePage.java                 # Tüm page object'lerin base'i
│   ├── component/
│   │   └── NavbarComponent.java      # Navigasyon bileşeni
│   ├── auth/
│   │   ├── LoginPage.java
│   │   ├── RegisterPage.java
│   │   ├── ForgotPasswordPage.java
│   │   ├── ResetPasswordPage.java
│   │   └── EmailVerificationSentPage.java
│   ├── dashboard/
│   │   └── DashboardPage.java
│   ├── goal/
│   │   ├── GoalListPage.java
│   │   ├── CreateGoalPage.java
│   │   ├── GoalDetailPage.java
│   │   ├── EditGoalPage.java
│   │   ├── EntryFormComponent.java
│   │   └── EntryRowComponent.java
│   ├── social/
│   │   ├── SocialPage.java
│   │   ├── FriendsTabComponent.java
│   │   ├── LeaderboardTabComponent.java
│   │   ├── ActivityFeedTabComponent.java
│   │   └── UserSearchPage.java
│   ├── notification/
│   │   ├── NotificationListPage.java
│   │   └── NotificationSettingsPage.java
│   └── profile/
│       └── ProfilePage.java
└── tests/
    ├── auth/
    │   └── AuthenticationE2eTest.java
    ├── dashboard/
    │   └── DashboardE2eTest.java
    ├── goal/
    │   ├── GoalManagementE2eTest.java
    │   └── ProgressTrackingE2eTest.java
    ├── social/
    │   └── SocialE2eTest.java
    └── notification/
        ├── NotificationE2eTest.java
        └── ProfileE2eTest.java
```

---

## 🔧 Çalıştırma Komutları

```bash
# Tüm E2E testlerini çalıştır (headless)
mvn test -Pe2e -De2e.headless=true

# Belirli bir faz/sınıfı çalıştır
mvn test -Pe2e -Dtest=AuthenticationE2eTest

# Allure rapor oluştur
mvn allure:serve

# Headed mod (debug için)
mvn test -Pe2e -De2e.headless=false -De2e.browser=chrome
```

---

## 🎯 Kalite Hedefleri

- Her sayfa en az 1 pozitif + 1 negatif test case'e sahip
- Tüm form validasyonları test edilir
- Tüm CRUD operasyonları uçtan uca doğrulanır
- Cross-page navigasyon testleri mevcut
- Hata mesajları ve success mesajları doğrulanır
- Empty state'ler test edilir
- Authorization (yetkisiz erişim) kontrolleri yapılır
- Allure raporunda her test `@Epic`, `@Feature`, `@Story` ile etiketli
- Her failure'da otomatik screenshot Allure'a eklenir

---

## 📦 Bağımlılık Sırası

```
Faz 0 → Faz 1 → Faz 2 → Faz 3 → Faz 4
                                    ↓
                               Faz 5, Faz 6
```

- **Faz 0** tüm fazların temelidir
- **Faz 1** (Auth) tüm testler için gereklidir (login yapabilme)
- **Faz 2** (Dashboard) Faz 1'e bağımlıdır
- **Faz 3** (Goal) Faz 1'e bağımlıdır
- **Faz 4** (Progress) Faz 3'e bağımlıdır (hedef oluşturma)
- **Faz 5** (Social) Faz 1'e bağımlıdır
- **Faz 6** (Notifications & Profile) Faz 1'e bağımlıdır

