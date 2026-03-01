# 🎯 Faz 3 — Hedef Yönetimi E2E Testleri

| Alan | Değer |
|------|-------|
| Süre | 1.5-2 gün |
| Bağımlılık | Faz 0 (altyapı), Faz 1 (login) |
| Hedef | Hedef CRUD operasyonları, filtreleme, sıralama, pagination, durum güncelleme, export |
| Test Sayısı | ~18 test case |
| Allure Epic | `Goal Management` |

---

## 📁 Oluşturulacak Dosyalar

### Page Object'ler

#### 1. `GoalListPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/goal/GoalListPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Yeni Hedef butonu | `a[href='/goals/new']` veya `.btn-primary` | Hedef oluşturma linki |
| Hedef kartları | `.goal-card` veya `.card` | Her hedefin kartı |
| Hedef başlığı (kartta) | `.goal-card .card-title` | Kart içindeki başlık |
| Status filtresi | `select[name='status']` veya `.status-filter` | Durum dropdown/pills |
| Kategori filtresi | `select[name='category']` | Kategori dropdown |
| GoalType filtresi | `select[name='goalType']` | Hedef tipi dropdown |
| Arama input | `input[name='query']` | Metin araması |
| Sıralama dropdown | `select[name='sort']` | Sıralama seçenekleri |
| Filtre butonu | `button[type='submit']` (filtre formu) | Filtreleri uygula |
| Pagination | `.pagination` | Sayfalama kontrolleri |
| Sonraki sayfa | `.page-link[aria-label='Next']` | Sonraki sayfa |
| Toplam sayı | `.total-count` veya badge | Toplam hedef sayısı |
| Boş durum | `.empty-state` | "Henüz hedef yok" |

**Fluent Metotlar:**
```java
// Navigasyon
CreateGoalPage clickNewGoal()
GoalDetailPage clickGoal(String title)
GoalDetailPage clickGoalByIndex(int index)

// Filtreleme
GoalListPage filterByStatus(String status)
GoalListPage filterByCategory(String category)
GoalListPage filterByGoalType(String goalType)
GoalListPage searchByQuery(String query)
GoalListPage sortBy(String sortOption)
GoalListPage applyFilters()
GoalListPage clearFilters()

// Veri Okuma
List<String> getGoalTitles()
int getGoalCardCount()
int getTotalGoalCount()
boolean isGoalVisible(String title)
boolean isEmptyStateVisible()
String getEmptyStateMessage()

// Pagination
boolean isPaginationVisible()
GoalListPage goToNextPage()
GoalListPage goToPage(int pageNumber)
int getCurrentPage()

// Doğrulama
boolean isOnGoalListPage()
String getSuccessMessage()
String getErrorMessage()
```

---

#### 2. `CreateGoalPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/goal/CreateGoalPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Başlık input | `#title` | Hedef başlığı |
| Açıklama textarea | `#description` | Açıklama |
| Birim input | `#unit` | Ölçü birimi |
| Hedef Değeri input | `#targetValue` | Hedef sayısal değer |
| Hedef Tipi select | `#goalType` | DAILY/CUMULATIVE/RATE |
| Frekans select | `#frequency` | DAILY/WEEKLY/MONTHLY |
| Kategori select | `#category` | Kategori seçimi |
| Başlangıç Tarihi | `#startDate` | Başlangıç (date input) |
| Bitiş Tarihi | `#endDate` | Bitiş (date input) |
| Renk input | `#color` | Renk seçici |
| Kaydet butonu | `button[type='submit']` | Form gönder |
| Geri butonu | `a[href='/goals']` veya geri linki | Listeye dön |
| Validation hataları | `.invalid-feedback`, `.is-invalid` | Alan hataları |
| Genel hata | `.alert-danger` | Sunucu hatası |

**Fluent Metotlar:**
```java
CreateGoalPage fillTitle(String title)
CreateGoalPage fillDescription(String description)
CreateGoalPage fillUnit(String unit)
CreateGoalPage fillTargetValue(String value)
CreateGoalPage selectGoalType(String type)
CreateGoalPage selectFrequency(String frequency)
CreateGoalPage selectCategory(String category)
CreateGoalPage fillStartDate(LocalDate date)
CreateGoalPage fillEndDate(LocalDate date)
CreateGoalPage fillColor(String color)
GoalDetailPage submit()                            // Başarılı → detay
CreateGoalPage submitExpectingErrors()              // Hatalı → aynı sayfa
GoalListPage clickBack()

// Doğrulama
String getFieldError(String fieldName)
boolean isFieldInvalid(String fieldName)
boolean isOnCreatePage()

// Hızlı form doldurma
CreateGoalPage fillMinimalForm(String title, String unit, String targetValue,
                                LocalDate startDate, LocalDate endDate)
CreateGoalPage fillCompleteForm(String title, String description, String unit,
                                 String targetValue, String goalType, String frequency,
                                 String category, LocalDate start, LocalDate end, String color)
```

---

#### 3. `GoalDetailPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/goal/GoalDetailPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Hedef başlığı | `h1`, `h2`, `.goal-title` | Hedef adı |
| Durum badge | `.badge`, `.status-badge` | ACTIVE/PAUSED/COMPLETED/ARCHIVED |
| Açıklama | `.goal-description` | Hedef açıklaması |
| İlerleme yüzdesi | `.completion-pct`, `.progress-bar` | Tamamlanma yüzdesi |
| Mevcut ilerleme | `.current-progress` | Mevcut toplam değer |
| Kalan gün | `.days-left` | Kalan gün sayısı |
| Hedef değeri | `.target-value` | Hedef değer |
| Düzenle butonu | `a[href*='/edit']` | Düzenleme sayfasına link |
| Sil butonu | `.btn-delete`, `button.btn-danger` | Silme butonu |
| Sil onay modal | `.modal .btn-danger`, `#confirmDelete` | Modal onay butonu |
| Durum güncelle | `select[name='newStatus']`, `.status-update` | Durum değiştirme |
| Export Excel | `a[href*='export/excel']` | Excel indirme |
| Export PDF | `a[href*='export/pdf']` | PDF indirme |
| Export CSV | `a[href*='export/csv']` | CSV indirme |
| Entry formu | `form.entry-form` | Yeni entry formu |
| Entry listesi | `.entry-list .entry-item` veya `table tbody tr` | Entry satırları |
| Başarı mesajı | `.alert-success` | Flash success |
| Hata mesajı | `.alert-danger` | Flash error |
| Chart alanı | `canvas`, `#chart` | Grafik |

**Fluent Metotlar:**
```java
// Okuma
String getTitle()
String getStatus()
String getDescription()
String getProgressPercentage()
String getCurrentProgress()
String getDaysLeft()
String getTargetValue()
boolean isChartVisible()

// Aksiyon
EditGoalPage clickEdit()
GoalDetailPage clickDelete()
GoalListPage confirmDelete()
GoalDetailPage updateStatus(String newStatus)

// Export
void clickExportExcel()
void clickExportPdf()
void clickExportCsv()

// Entry (delegation to EntryFormComponent)
GoalDetailPage createEntry(LocalDate date, String value, String note)
int getEntryCount()
List<String> getEntryDates()

// Mesajlar
String getSuccessMessage()
String getErrorMessage()
boolean isSuccessMessageVisible()
boolean isOnGoalDetailPage()
```

---

#### 4. `EditGoalPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/goal/EditGoalPage.java`

**Locator'lar:** CreateGoalPage ile aynı, ek olarak pre-filled değer kontrolü.

**Fluent Metotlar:**
```java
// Pre-filled değer okuma
String getPrefilledTitle()
String getPrefilledUnit()
String getPrefilledTargetValue()

// Güncelleme
EditGoalPage clearAndFillTitle(String title)
EditGoalPage clearAndFillDescription(String description)
EditGoalPage clearAndFillUnit(String unit)
EditGoalPage clearAndFillTargetValue(String value)
GoalDetailPage submit()
EditGoalPage submitExpectingErrors()
GoalDetailPage clickCancel()

boolean isOnEditPage()
```

---

## 🧪 Test Sınıfı

### `GoalManagementE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/goal/GoalManagementE2eTest.java`

**Allure Metadata:**
```java
@Epic("Goal Management")
@Feature("Hedef Yönetimi")
```

---

### Test Case'ler — Hedef Listesi

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 1 | `shouldShowGoalListPage` | Hedef listesi sayfası doğru render edilir | 1. Login yap<br>2. `/goals` sayfasına git | ✅ URL `/goals` içerir<br>✅ "Yeni Hedef" butonu mevcut<br>✅ Filtre kontrolleri görünür |
| 2 | `shouldShowEmptyGoalList` | Hedefi olmayan kullanıcıda boş durum | 1. Yeni user ile login<br>2. `/goals` git | ✅ Boş durum mesajı görünür<br>✅ Hedef kartı yok |

---

### Test Case'ler — Hedef Oluşturma

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 3 | `shouldCreateGoalWithMinimalFields` | Zorunlu alanlarla hedef oluşturur | 1. Login<br>2. `/goals/new` git<br>3. Başlık, birim, hedef değer, tarihler gir<br>4. Kaydet tıkla | ✅ Hedef detay sayfasına redirect<br>✅ "Hedef başarıyla oluşturuldu" success mesajı<br>✅ Başlık doğru<br>✅ Durum ACTIVE |
| 4 | `shouldCreateGoalWithAllFields` | Tüm alanlarla hedef oluşturur | 1. Login<br>2. Tüm alanları doldur (açıklama, kategori, tip, frekans, renk)<br>3. Kaydet | ✅ Detay sayfasında tüm bilgiler doğru<br>✅ Kategori, tip, frekans gösteriliyor |
| 5 | `shouldShowValidationErrorsOnCreateGoal` | Geçersiz form validation gösterir | 1. Boş form gönder | ✅ Create sayfasında kalır<br>✅ Başlık zorunlu hatası<br>✅ Birim zorunlu hatası<br>✅ Hedef değeri hatası |
| 6 | `shouldShowErrorForEndDateBeforeStartDate` | Bitiş < başlangıç tarihi hatası | 1. Başlangıç: yarın, bitiş: dün<br>2. Kaydet | ✅ Hata mesajı görünür<br>✅ Create sayfasında kalır |

---

### Test Case'ler — Hedef Detay

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 7 | `shouldNavigateToGoalDetail` | Listeden detaya geçiş | 1. Hedef oluştur<br>2. Listeden hedefe tıkla | ✅ URL `/goals/{id}` formatında<br>✅ Doğru başlık görünür<br>✅ Stats kartları mevcut |

---

### Test Case'ler — Hedef Düzenleme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 8 | `shouldEditGoalSuccessfully` | Hedef başlığını değiştirir | 1. Hedef oluştur<br>2. Detaydan "Düzenle" tıkla<br>3. Başlığı değiştir<br>4. Kaydet | ✅ Detay sayfasına redirect<br>✅ "Hedef başarıyla güncellendi" mesajı<br>✅ Yeni başlık görünür |
| 9 | `shouldPreFillEditFormWithExistingValues` | Düzenleme formu mevcut değerlerle dolu | 1. Hedef oluştur<br>2. "Düzenle" tıkla | ✅ Başlık input'u eski değeri içerir<br>✅ Birim input'u eski değeri içerir<br>✅ Hedef değeri doğru |

---

### Test Case'ler — Hedef Silme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 10 | `shouldDeleteGoalSuccessfully` | Hedefi siler | 1. Hedef oluştur<br>2. Detaydan "Sil" tıkla<br>3. Modal'da onayla | ✅ `/goals` sayfasına redirect<br>✅ "Hedef başarıyla silindi" mesajı<br>✅ Silinen hedef listede yok |

---

### Test Case'ler — Durum Güncelleme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 11 | `shouldUpdateGoalStatusTopaused` | ACTIVE → PAUSED | 1. Aktif hedef oluştur<br>2. Detaydan PAUSED'a güncelle | ✅ "Hedef durumu güncellendi" mesajı<br>✅ Durum badge "PAUSED" |
| 12 | `shouldUpdateGoalStatusToCompleted` | ACTIVE → COMPLETED | 1. Aktif hedef<br>2. COMPLETED'a güncelle | ✅ Durum badge "COMPLETED"<br>✅ Success mesajı |

---

### Test Case'ler — Filtreleme & Arama

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 13 | `shouldFilterGoalsByStatus` | Status filtresi çalışır | 1. 2 ACTIVE + 1 COMPLETED hedef oluştur<br>2. ACTIVE filtresi uygula | ✅ Sadece ACTIVE hedefler görünür<br>✅ COMPLETED hedef listede yok |
| 14 | `shouldFilterGoalsByCategory` | Kategori filtresi çalışır | 1. HEALTH + FITNESS kategorilerinde hedef oluştur<br>2. HEALTH filtrele | ✅ Sadece HEALTH kategorisi hedefler görünür |
| 15 | `shouldSearchGoalsByQuery` | Metin araması çalışır | 1. "Kitap Okuma" ve "Koşu" hedefleri oluştur<br>2. "Kitap" ara | ✅ Sadece "Kitap Okuma" görünür<br>✅ "Koşu" görünmez |
| 16 | `shouldPaginateGoals` | Pagination çalışır | 1. 15 hedef oluştur (sayfa boyutu 12)<br>2. Listeye git | ✅ İlk sayfada 12 hedef<br>✅ Pagination kontrolleri görünür<br>✅ 2. sayfaya geç → kalan hedefler |

---

### Test Case'ler — Export

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 17 | `shouldExportGoalAsExcel` | Excel export çalışır | 1. Hedef + entry oluştur<br>2. Detayda Excel butonuna tıkla | ✅ Dosya indirilir (HTTP 200)<br>✅ Content-Type `application/vnd.openxmlformats...`<br>✅ Dosya boyutu > 0 |
| 18 | `shouldExportGoalAsPdf` | PDF export çalışır | 1. Hedef + entry oluştur<br>2. PDF butonuna tıkla | ✅ Dosya indirilir<br>✅ Content-Type `application/pdf` |
| 19 | `shouldExportGoalAsCsv` | CSV export çalışır | 1. CSV butonuna tıkla | ✅ Dosya indirilir<br>✅ Content-Type `text/csv` |

---

## 🛡️ Edge Case'ler

| # | Senaryo | Doğrulama |
|---|---------|-----------|
| 1 | Çok uzun hedef başlığı (200 karakter) | Başlık kabul edilir veya max length validation |
| 2 | Hedef değeri 0 veya negatif | Validation hatası |
| 3 | Aynı tarihler (başlangıç = bitiş) | Kabul edilir veya hata (uygulama davranışına göre) |
| 4 | Birden fazla filtre kombinasyonu | Status + Category + Query birlikte çalışır |
| 5 | Boş arama sonucu | "Sonuç bulunamadı" mesajı veya boş liste |
| 6 | Geçersiz durum geçişi (COMPLETED → ACTIVE) | Hata mesajı gösterilir |
| 7 | Başka kullanıcının hedefine URL ile erişim | 403 veya 404 hatası |
| 8 | Çok büyük hedef değeri (999999.99) | Kabul edilir, doğru gösterilir |

---

## 🏷️ Allure Etiketleme

```java
@Epic("Goal Management")
@Feature("Hedef Oluşturma")
@Story("Zorunlu Alanlarla Hedef Oluşturma")

@Epic("Goal Management")
@Feature("Hedef Düzenleme")
@Story("Başlık Güncelleme")

@Epic("Goal Management")
@Feature("Hedef Silme")
@Story("Onaylı Silme")

@Epic("Goal Management")
@Feature("Filtreleme & Arama")
@Story("Status Filtreleme")

@Epic("Goal Management")
@Feature("Export")
@Story("Excel Export")
```

---

## 🔧 Export Test Stratejisi

Export testleri için özel Chrome ayarı gerekir:

```java
// WebDriverConfig'de download dizini ayarı
ChromeOptions options = new ChromeOptions();
Map<String, Object> prefs = new HashMap<>();
prefs.put("download.default_directory", downloadDir.toString());
prefs.put("download.prompt_for_download", false);
options.setExperimentalOption("prefs", prefs);
```

**Dosya doğrulama:**
```java
// Belirli süre içinde dosyanın disk'e yazılmasını bekle
Path downloadedFile = waitForFileInDirectory(downloadDir, ".xlsx", 10);
assertThat(downloadedFile).exists();
assertThat(Files.size(downloadedFile)).isGreaterThan(0);
```

**Alternatif yaklaşım:** Export linkleri `download` attribute'u taşıyorsa, HTTP status code kontrolü yeterli olabilir. `RestTemplate` ile cookie-based auth üzerinden GET isteği atılabilir.

---

## ✅ Faz 3 Tamamlama Kriterleri

- [ ] GoalListPage, CreateGoalPage, GoalDetailPage, EditGoalPage oluşturuldu
- [ ] 19 test case yazıldı ve başarıyla çalışıyor
- [ ] CRUD operasyonları uçtan uca doğrulandı
- [ ] Filtreleme, arama, pagination test edildi
- [ ] Durum güncelleme test edildi
- [ ] Export işlevselliği doğrulandı
- [ ] Edge case'ler cover edildi
- [ ] Allure etiketleri eklendi

---

## 📁 Dosya Listesi

```
src/test/java/com/goaltracker/e2e/
    page/goal/
        GoalListPage.java
        CreateGoalPage.java
        GoalDetailPage.java
        EditGoalPage.java
    tests/goal/
        GoalManagementE2eTest.java
```

