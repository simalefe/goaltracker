# 📈 Faz 4 — İlerleme Takibi E2E Testleri

| Alan | Değer |
|------|-------|
| Süre | 1-1.5 gün |
| Bağımlılık | Faz 0 (altyapı), Faz 1 (login), Faz 3 (hedef oluşturma, GoalDetailPage) |
| Hedef | Goal Entry CRUD, istatistik doğrulama, chart görünürlüğü, dashboard yansıması |
| Test Sayısı | ~13 test case |
| Allure Epic | `Progress Tracking` |

---

## 📁 Oluşturulacak Dosyalar

### Page Object Bileşenleri

#### 1. `EntryFormComponent.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/goal/EntryFormComponent.java`

**Konum:** `GoalDetailPage` içinde composite bileşen olarak kullanılır.

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Tarih input | `input[name='entryDate']` | Entry tarihi (date input) |
| Değer input | `input[name='actualValue']` | Gerçekleşen değer |
| Not textarea | `textarea[name='note']`, `input[name='note']` | İsteğe bağlı not |
| Kaydet butonu | `.entry-form button[type='submit']` | Entry kaydet |

**Fluent Metotlar:**
```java
EntryFormComponent fillDate(LocalDate date)
EntryFormComponent fillValue(String value)
EntryFormComponent fillNote(String note)
GoalDetailPage submit()                        // Form gönder → sayfa yenilenir
EntryFormComponent clearDate()
EntryFormComponent clearValue()
```

---

#### 2. `EntryRowComponent.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/goal/EntryRowComponent.java`

**Konum:** `GoalDetailPage`'deki entry listesindeki her satır.

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Tarih | `td:nth-child(1)`, `.entry-date` | Entry tarihi |
| Değer | `td:nth-child(2)`, `.entry-value` | Gerçekleşen değer |
| Not | `td:nth-child(3)`, `.entry-note` | Not alanı |
| Düzenle butonu | `.btn-edit`, `a[href*='edit']` | Entry düzenleme |
| Sil butonu | `.btn-delete`, `button.btn-danger` | Entry silme |

**Fluent Metotlar:**
```java
String getDate()
String getValue()
String getNote()
GoalDetailPage clickEdit()
GoalDetailPage clickDelete()
```

---

### GoalDetailPage Genişletmesi

Faz 3'te oluşturulan `GoalDetailPage`'e aşağıdaki entry-specific metotlar eklenir:

```java
// Entry Form
GoalDetailPage createEntry(LocalDate date, String value, String note)
GoalDetailPage createEntry(LocalDate date, String value)   // Not olmadan

// Entry Listesi
int getEntryCount()
List<EntryRowComponent> getEntryRows()
EntryRowComponent getEntry(int index)
EntryRowComponent getEntryByDate(String date)
boolean isEntryListEmpty()

// Entry Düzenleme (inline veya modal)
GoalDetailPage editEntry(int index, String newValue, String newNote)

// Entry Silme
GoalDetailPage deleteEntry(int index)
GoalDetailPage confirmDeleteEntry()

// Stats (istatistik kartları)
String getCompletionPercentage()       // "%45.5" formatında
String getCurrentProgressValue()       // "23.5 / 50 kitap" formatında
String getDaysLeftText()              // "42 gün kaldı"
String getTrackingStatus()            // "AHEAD" / "ON_TRACK" / "BEHIND"
String getGapValue()                  // "+5.2" veya "-3.1"
String getRequiredRate()              // "Günlük gerekli: 2.3"
boolean isStatsCardVisible()

// Chart
boolean isChartCanvasVisible()
```

---

## 🧪 Test Sınıfı

### `ProgressTrackingE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/goal/ProgressTrackingE2eTest.java`

**Allure Metadata:**
```java
@Epic("Progress Tracking")
@Feature("İlerleme Takibi")
```

**Setup Stratejisi:**
```java
@BeforeEach
void setUp() {
    // Her test için izole kullanıcı ve hedef
    testUser = helper.createVerifiedUser();
    goalId = helper.createGoalForUser(
        testUser.id(),
        "Test Hedefi",
        GoalCategory.HEALTH,
        GoalType.CUMULATIVE,
        BigDecimal.valueOf(100),
        "sayfa",
        LocalDate.now().minusDays(30),
        LocalDate.now().plusDays(30)
    );
    loginAs(testUser.email(), testUser.password());
}
```

---

### Test Case'ler — Entry Oluşturma

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 1 | `shouldCreateEntrySuccessfully` | Tarih, değer ve not ile entry oluşturur | 1. Hedef detay sayfasına git<br>2. Entry formunda: bugünün tarihi, "5", "İyi bir gün" gir<br>3. Kaydet tıkla | ✅ "İlerleme kaydedildi" success mesajı<br>✅ Entry listesinde yeni kayıt görünür<br>✅ Tarih bugünün tarihi<br>✅ Değer "5" |
| 2 | `shouldCreateEntryWithoutNote` | Not olmadan entry oluşturur | 1. Hedef detay sayfasına git<br>2. Tarih ve değer gir, not boş bırak<br>3. Kaydet | ✅ "İlerleme kaydedildi" mesajı<br>✅ Entry listesinde kayıt mevcut<br>✅ Not alanı boş/"-" |
| 3 | `shouldShowErrorForDuplicateDate` | Aynı tarihe iki entry eklemeyi engeller | 1. Bugün için entry oluştur<br>2. Aynı tarihle tekrar entry oluşturmayı dene | ✅ Hata mesajı görünür ("zaten mevcut" veya "duplicate")<br>✅ Entry sayısı artmaz |
| 4 | `shouldShowErrorForNegativeValue` | Negatif değer girişini engeller | 1. Değer olarak "-5" gir<br>2. Kaydet | ✅ Hata mesajı veya validation hatası<br>✅ Entry oluşturulmaz |
| 5 | `shouldShowErrorForZeroValue` | Sıfır değer girişini kontrol eder | 1. Değer olarak "0" gir<br>2. Kaydet | ✅ Hata mesajı (eğer 0 engelliyse) veya başarılı (eğer izin veriliyorsa) |

---

### Test Case'ler — Entry Düzenleme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 6 | `shouldUpdateEntrySuccessfully` | Mevcut entry'nin değerini günceller | 1. Entry oluştur (değer: 5)<br>2. Entry satırında düzenle tıkla<br>3. Değeri 10'a güncelle<br>4. Kaydet | ✅ "Kayıt güncellendi" success mesajı<br>✅ Entry listesinde güncel değer "10" |
| 7 | `shouldUpdateEntryNote` | Entry notunu günceller | 1. Entry oluştur<br>2. Düzenle → notu değiştir<br>3. Kaydet | ✅ Güncellenen not görünür |

---

### Test Case'ler — Entry Silme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 8 | `shouldDeleteEntrySuccessfully` | Entry'yi siler | 1. Entry oluştur<br>2. Entry satırında sil tıkla<br>3. (Onay varsa) onayla | ✅ "Kayıt silindi" success mesajı<br>✅ Entry listesinde kayıt yok |

---

### Test Case'ler — İstatistik Doğrulama

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 9 | `shouldShowStatsAfterEntryCreation` | Entry eklendikten sonra istatistikler güncellenir | 1. Hedef (target: 100, birim: sayfa)<br>2. 25 değerinde entry ekle<br>3. Sayfa yenilenir | ✅ İlerleme yüzdesi > 0%<br>✅ Mevcut değer "25" veya "25/100"<br>✅ Stats kartları görünür |
| 10 | `shouldShowCompletionWhenTargetReached` | Hedef değerine ulaşılınca %100 gösterir | 1. Hedef (target: 10)<br>2. 10 değerinde entry ekle | ✅ İlerleme %100<br>✅ Tamamlanma durumu gösterilir |
| 11 | `shouldShowZeroStatsWithoutEntries` | Entry olmadan istatistikler sıfır | 1. Yeni hedef oluştur<br>2. Detay sayfasına git | ✅ İlerleme %0<br>✅ Mevcut değer 0<br>✅ Entry listesi boş |

---

### Test Case'ler — Chart & Dashboard Yansıması

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 12 | `shouldShowChartAfterEntries` | Entry'ler eklendikten sonra grafik görünür | 1. 3 farklı tarihte entry ekle<br>2. Detay sayfasını yenile | ✅ Chart canvas/container görünür<br>✅ Grafik verisi yüklendi (AJAX success) |
| 13 | `shouldReflectEntryInDashboard` | Entry eklendikten sonra dashboard metrikleri güncellenir | 1. Hedef oluştur<br>2. Bugün entry ekle<br>3. Dashboard'a git | ✅ `todayEntryCount` ≥ 1<br>✅ Son girişler listesinde yeni entry görünür |

---

## 🛡️ Edge Case'ler

| # | Senaryo | Doğrulama |
|---|---------|-----------|
| 1 | Çok büyük entry değeri (999999.99) | Kabul edilir, stats doğru hesaplanır |
| 2 | Ondalıklı değer (3.75) | Doğru kaydedilir ve gösterilir |
| 3 | Hedef tarih aralığı dışında entry (out of range) | Hata mesajı, entry oluşturulmaz |
| 4 | PAUSED durumdaki hedefe entry ekleme | Hata mesajı (GoalNotActiveException) |
| 5 | COMPLETED durumdaki hedefe entry ekleme | Hata mesajı |
| 6 | ARCHIVED durumdaki hedefe entry ekleme | Hata mesajı |
| 7 | Birden fazla entry hızlıca ekleme | Her biri başarılı, sayaç doğru artar |
| 8 | Entry silindiğinde istatistik güncellemesi | Mevcut değer azalır, yüzde düşer |
| 9 | Tüm entry'ler silindiğinde | Stats sıfıra döner, chart boş |

---

## 🏷️ Allure Etiketleme

```java
@Epic("Progress Tracking")
@Feature("Entry Oluşturma")
@Story("Başarılı Entry Kaydı")

@Epic("Progress Tracking")
@Feature("Entry Düzenleme")
@Story("Değer Güncelleme")

@Epic("Progress Tracking")
@Feature("Entry Silme")
@Story("Onaylı Silme")

@Epic("Progress Tracking")
@Feature("İstatistik Doğrulama")
@Story("İlerleme Yüzdesi Hesaplaması")

@Epic("Progress Tracking")
@Feature("Chart")
@Story("Grafik Görünürlüğü")
```

---

## 🔧 Test Data Setup

```java
// Her entry testi için
TestUser user = helper.createVerifiedUser();
Long goalId = helper.createGoalForUser(user.id(),
    "Kitap Okuma",
    GoalCategory.EDUCATION,
    GoalType.CUMULATIVE,
    BigDecimal.valueOf(100),
    "sayfa",
    LocalDate.now().minusDays(30),
    LocalDate.now().plusDays(30)
);

// Dolu stats testi için (DB helper ile arka planda entry ekleme)
helper.createEntryForGoal(goalId, LocalDate.now().minusDays(5), BigDecimal.valueOf(10));
helper.createEntryForGoal(goalId, LocalDate.now().minusDays(4), BigDecimal.valueOf(15));
helper.createEntryForGoal(goalId, LocalDate.now().minusDays(3), BigDecimal.valueOf(8));
```

---

## ✅ Faz 4 Tamamlama Kriterleri

- [ ] EntryFormComponent ve EntryRowComponent oluşturuldu
- [ ] GoalDetailPage entry metotlarıyla genişletildi
- [ ] 13 test case yazıldı ve başarıyla çalışıyor
- [ ] Entry CRUD uçtan uca doğrulandı
- [ ] İstatistik kartları doğru değer gösteriyor
- [ ] Chart görünürlüğü doğrulandı
- [ ] Dashboard yansıması test edildi
- [ ] Duplicate entry, negatif değer gibi edge case'ler cover edildi
- [ ] Allure etiketleri eklendi

---

## 📁 Dosya Listesi

```
src/test/java/com/goaltracker/e2e/
    page/goal/
        EntryFormComponent.java
        EntryRowComponent.java
        GoalDetailPage.java (güncelleme — entry metotları)
    tests/goal/
        ProgressTrackingE2eTest.java
```

