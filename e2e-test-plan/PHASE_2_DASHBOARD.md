# 📊 Faz 2 — Dashboard E2E Testleri

| Alan | Değer |
|------|-------|
| Süre | 0.5-1 gün |
| Bağımlılık | Faz 0 (altyapı), Faz 1 (login yapabilme) |
| Hedef | Dashboard sayfasının metrik kartları, boş durum, hedef listesi, navigasyon akışları |
| Test Sayısı | ~10 test case |
| Allure Epic | `Dashboard` |

---

## 📁 Oluşturulacak Dosyalar

### Page Object

#### `DashboardPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/dashboard/DashboardPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Aktif Hedef kartı | `.metric-card:nth-child(1) .metric-value` veya `#activeGoalCount` | Aktif hedef sayısı |
| Bugün Giriş kartı | `.metric-card:nth-child(2) .metric-value` veya `#todayEntryCount` | Bugün girilen entry sayısı |
| Streak kartı | `.metric-card:nth-child(3) .metric-value` veya `#totalStreakDays` | Toplam streak gün |
| Yolunda kartı | `#goalsOnTrack` | Yolunda olan hedef sayısı |
| Geride kartı | `#goalsBehind` | Geride olan hedef sayısı |
| Top Hedefler listesi | `.top-goals-list .goal-item` veya kartın içindeki liste | En iyi 5 hedef |
| Son Girişler listesi | `.recent-entries .entry-item` | Son 5 entry |
| Streak listesi | `.streak-list .streak-item` | Aktif streak'ler |
| Boş durum mesajı | `.empty-state` | "Henüz hedefin yok!" |
| Yeni Hedef Oluştur butonu | `a[href='/goals/new']` veya `.empty-state a` | Hedef oluşturma linki |
| Tümünü Gör linki | `a[href='/goals']` | Hedef listesine link |
| Hata mesajı | `.alert-danger` | Dashboard hata durumu |
| Sayfa başlığı | `h1`, `h2` veya `.page-title` | "Dashboard" |

**Fluent Metotlar:**
```java
// Metrik Kartları
int getActiveGoalCount()
int getTodayEntryCount()
int getTotalStreakDays()
int getGoalsOnTrack()
int getGoalsBehind()

// Top Hedefler
List<String> getTopGoalTitles()
int getTopGoalCount()
boolean isTopGoalsListVisible()
GoalDetailPage clickTopGoal(int index)

// Son Girişler
List<String> getRecentEntryTitles()
int getRecentEntryCount()
boolean isRecentEntriesVisible()

// Streak
List<String> getStreakGoalTitles()
boolean isStreakListVisible()

// Boş Durum
boolean isEmptyStateVisible()
String getEmptyStateMessage()
CreateGoalPage clickNewGoalFromEmptyState()

// Navigasyon
CreateGoalPage clickNewGoal()
GoalListPage clickViewAllGoals()

// Sayfa Doğrulama
boolean isOnDashboard()
String getPageTitle()
boolean isErrorMessageVisible()
String getErrorMessage()
```

---

## 🧪 Test Sınıfı

### `DashboardE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/dashboard/DashboardE2eTest.java`

**Allure Metadata:**
```java
@Epic("Dashboard")
@Feature("Dashboard Görüntüleme")
```

---

### Test Case'ler — Boş Dashboard

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 1 | `shouldShowDashboardAfterLogin` | Login sonrası dashboard yüklenir | 1. Verified user oluştur (DB)<br>2. Login yap<br>3. Dashboard'a yönlendirilir | ✅ URL `/dashboard` içerir<br>✅ Sayfa başlığı "Dashboard" |
| 2 | `shouldShowEmptyStateForNewUser` | Yeni kullanıcıda boş durum gösterir | 1. Yeni user oluştur, hiç hedef yok<br>2. Login yap<br>3. Dashboard'a git | ✅ Boş durum mesajı görünür<br>✅ "Yeni Hedef Oluştur" butonu mevcut<br>✅ Aktif hedef sayısı 0 |
| 3 | `shouldNavigateToNewGoalFromEmptyState` | Boş durumda "Yeni Hedef Oluştur" çalışır | 1. Yeni user ile dashboard'a git<br>2. "Yeni Hedef Oluştur" butonuna tıkla | ✅ URL `/goals/new` içerir |

---

### Test Case'ler — Dolu Dashboard

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 4 | `shouldShowMetricCardsWithCorrectValues` | Metrik kartları doğru değer gösterir | 1. User oluştur<br>2. 3 aktif hedef oluştur (DB helper)<br>3. 1 hedefe bugün entry ekle<br>4. Login yap → dashboard | ✅ Aktif hedef sayısı = 3<br>✅ Bugün giriş sayısı ≥ 1<br>✅ Metrik kartları görünür |
| 5 | `shouldShowTopGoalsList` | En iyi hedefler listesi görünür | 1. User oluştur<br>2. 3 hedef oluştur, entry'ler ekle<br>3. Dashboard'a git | ✅ Top hedefler listesi görünür<br>✅ En az 1 hedef gösterilir<br>✅ Hedef başlıkları görünür |
| 6 | `shouldShowRecentEntries` | Son girişler listesi görünür | 1. User oluştur<br>2. Hedef oluştur, 3 entry ekle<br>3. Dashboard'a git | ✅ Son girişler listesi görünür<br>✅ En az 1 entry gösterilir |
| 7 | `shouldShowStreaksOnDashboard` | Streak listesi görünür | 1. User oluştur<br>2. Hedef oluştur, art arda 3 gün entry ekle<br>3. Dashboard'a git | ✅ Streak listesi görünür (streak > 0 olan hedefler) |

---

### Test Case'ler — Navigasyon

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 8 | `shouldNavigateToGoalDetailFromTopGoals` | Top hedeften detaya git | 1. Dolu dashboard<br>2. Top hedeflerden birine tıkla | ✅ URL `/goals/{id}` formatında<br>✅ Hedef detay sayfası yüklendi |
| 9 | `shouldNavigateToGoalListFromDashboard` | Dashboard'dan hedef listesine git | 1. Dashboard'da<br>2. "Tümünü Gör" veya Navbar'dan "Hedefler" tıkla | ✅ URL `/goals` içerir |
| 10 | `shouldNavigateToAllMainPagesFromNavbar` | Navbar navigasyonu çalışır | 1. Dashboard'da login<br>2. Navbar'dan sırayla: Hedefler, Sosyal, Bildirimler, Profil tıkla | ✅ Her tıklamada doğru URL'ye git:<br>- `/goals`<br>- `/social`<br>- `/notifications`<br>- `/profile` |

---

## 🛡️ Edge Case'ler

| # | Senaryo | Doğrulama |
|---|---------|-----------|
| 1 | Tüm hedefler COMPLETED/ARCHIVED | Aktif hedef = 0, ama completed > 0 olabilir, empty state'den farklı |
| 2 | Çok fazla hedef (50+) | Dashboard hâlâ performanslı yüklenir, top 5 sınırı aşılmaz |
| 3 | Bugün henüz entry girilmemiş | `todayEntryCount` = 0 |
| 4 | Dashboard servisi hata verirse | Hata mesajı gösterilir, sayfa çökmez (graceful degradation) |

---

## 🏷️ Allure Etiketleme

```java
@Epic("Dashboard")
@Feature("Metrik Kartları")
@Story("Aktif Hedef Sayısı")

@Epic("Dashboard")
@Feature("Boş Dashboard")
@Story("Yeni Kullanıcı Boş Durum")

@Epic("Dashboard")
@Feature("Navigasyon")
@Story("Navbar ile Sayfa Geçişi")
```

---

## 🔧 Test Data Setup Stratejisi

Dashboard testleri için `@BeforeEach` veya `@BeforeAll`'da:

```java
// Boş dashboard testi
TestUser newUser = helper.createVerifiedUser();

// Dolu dashboard testi
TestUser user = helper.createVerifiedUser();
Long goalId1 = helper.createGoalForUser(user.id(), "Kitap Okuma", HEALTH, DAILY, 30, ...);
Long goalId2 = helper.createGoalForUser(user.id(), "Koşu", FITNESS, CUMULATIVE, 100, ...);
Long goalId3 = helper.createGoalForUser(user.id(), "Tasarruf", FINANCE, DAILY, 5000, ...);
helper.createEntryForGoal(goalId1, LocalDate.now(), BigDecimal.valueOf(5));
helper.createEntryForGoal(goalId1, LocalDate.now().minusDays(1), BigDecimal.valueOf(3));
helper.createEntryForGoal(goalId1, LocalDate.now().minusDays(2), BigDecimal.valueOf(4));
```

---

## ✅ Faz 2 Tamamlama Kriterleri

- [ ] DashboardPage page object oluşturuldu
- [ ] 10 test case yazıldı ve başarıyla çalışıyor
- [ ] Boş ve dolu dashboard durumları test edildi
- [ ] Metrik kartları doğru değerleri gösteriyor
- [ ] Navbar navigasyonu doğrulandı
- [ ] Allure etiketleri eklendi
- [ ] Edge case'ler cover edildi

---

## 📁 Dosya Listesi

```
src/test/java/com/goaltracker/e2e/
    page/dashboard/
        DashboardPage.java
    tests/dashboard/
        DashboardE2eTest.java
```

