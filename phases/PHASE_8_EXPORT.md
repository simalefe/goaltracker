# 📤 Faz 8 — Export (Excel, PDF, CSV)

> **Süre:** 3-5 gün  
> **Bağımlılık:** Faz 3 (GoalEntry, GoalCalculator, GoalStatsResponse hazır olmalı)  
> **Hedef:** Apache POI ile Excel (3 sayfalı), iText 7 ile PDF raporu, CSV export, frontend indirme butonları, aylık otomatik rapor scheduler'ı.

---

## 📋 Görev Listesi

### Backend — ExportService
- [ ] `ExportService.java` — `@Service`:
  - `exportGoalToExcel(Long goalId, Long userId)` → `byte[]` (Apache POI):
    - Ownership kontrolü
    - **Sayfa 1: "Özet"** — başlık, tarih aralığı, birim, hedef değer, tamamlanma %, gap, requiredRate, streak
    - **Sayfa 2: "İlerleme Kayıtları"** — header satırı (Tarih, Değer, Birim, Not) + tüm entry'ler
    - **Sayfa 3: "İstatistikler"** — günlük/haftalık ortalama, en yüksek entry, hedefsiz günler sayısı
    - Stil: header satırları bold, koşullu renklendirme (hedef üstü yeşil, altı kırmızı)
    - Sütun genişliği otomatik (`sheet.autoSizeColumn(i)`)
    - Dosya adı: `{title}-{yyyy-MM}.xlsx` (Türkçe karakter normalize et)
  - `exportGoalToPdf(Long goalId, Long userId)` → `byte[]` (iText 7):
    - Ownership kontrolü
    - **Bölüm 1:** Başlık + hedef bilgileri tablosu
    - **Bölüm 2:** İstatistik tablosu (2 sütun: metrik adı / değer)
    - **Bölüm 3:** Entry listesi tablosu (Tarih, Değer, Birim, Not)
    - Font: Türkçe karakter desteği için `PdfFontFactory.createFont(StandardFonts.HELVETICA)`
    - Sayfa başlığı/alt bilgisi (header/footer): hedef adı + sayfa numarası
    - Dosya adı: `{title}-{yyyy-MM}.pdf`
  - `exportGoalToCsv(Long goalId, Long userId)` → `byte[]`:
    - Ownership kontrolü
    - Header: `Tarih,Değer,Birim,Not`
    - UTF-8 BOM (`\uFEFF`) — Excel Türkçe karakter uyumu için
    - RFC 4180 CSV formatı (tırnak işareti, virgül içeren not alanları escape)
    - Dosya adı: `{title}-{yyyy-MM}.csv`
  - `generateMonthlyReport(Long userId, int year, int month)` → `byte[]` (PDF):
    - Kullanıcının o aydaki tüm aktif hedefleri
    - Her hedef için özet (tamamlanma %, toplam entry, streak)
    - Kapak sayfası (kullanıcı adı, tarih aralığı)
  - `normalizeFilename(String title)` — Türkçe karakter → ASCII, boşluk → `-`

### Backend — Controller Güncellemeleri
- [ ] `GoalController.java` güncelleme — export endpoint'leri:
  - `GET /api/goals/{id}/export/excel`
  - `GET /api/goals/{id}/export/pdf`
  - `GET /api/goals/{id}/export/csv`
  - Her endpoint: `ResponseEntity<byte[]>` döndür, `Content-Disposition: attachment` header'ı
- [ ] `UserController.java` güncelleme — aylık rapor:
  - `GET /api/users/me/reports/monthly?year=2026&month=2`

### Backend — Scheduler (Opsiyonel)
- [ ] `ReportScheduler.java`:
  - `@Scheduled(cron = "0 0 8 1 * *")` — Her ayın 1'i saat 08:00
  - Tüm kullanıcılar için geçen ayın raporunu oluştur
  - `NotificationService` + `MailService` ile PDF e-posta olarak gönder
  - Kullanıcı `weeklySummaryEnabled` kontrolü (zaten varsa buraya da uygulanabilir)

### Frontend (Thymeleaf — Export Butonları)
- [ ] `templates/goals/detail.html` güncelleme — export buton grubu:
  ```html
  <div class="btn-group">
    <a th:href="@{/api/goals/{id}/export/excel(id=${goal.id})}"
       class="btn btn-success btn-sm" download>
      <i class="bi bi-file-earmark-excel"></i> Excel İndir
    </a>
    <a th:href="@{/api/goals/{id}/export/pdf(id=${goal.id})}"
       class="btn btn-danger btn-sm" download>
      <i class="bi bi-file-pdf"></i> PDF İndir
    </a>
    <a th:href="@{/api/goals/{id}/export/csv(id=${goal.id})}"
       class="btn btn-primary btn-sm" download>
      <i class="bi bi-filetype-csv"></i> CSV İndir
    </a>
  </div>
  ```
- [ ] `templates/profile/index.html` güncelleme — aylık rapor indirme:
  - Ay/yıl seçici
  - "Aylık Raporu İndir" butonu (`/api/users/me/reports/monthly?year=...&month=...`)

---

## 🔌 API Endpoint'leri

```
GET /api/goals/{id}/export/excel
    Authorization: Bearer <token>
    → 200
    Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
    Content-Disposition: attachment; filename="kitap-okuma-2026-02.xlsx"
    Body: binary Excel data

GET /api/goals/{id}/export/pdf
    → 200
    Content-Type: application/pdf
    Content-Disposition: attachment; filename="kitap-okuma-2026-02.pdf"

GET /api/goals/{id}/export/csv
    → 200
    Content-Type: text/csv; charset=UTF-8
    Content-Disposition: attachment; filename="kitap-okuma-2026-02.csv"

GET /api/users/me/reports/monthly?year=2026&month=2
    → 200
    Content-Type: application/pdf
    Content-Disposition: attachment; filename="aylik-rapor-2026-02.pdf"
```

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── service/
│   └── ExportService.java
├── scheduler/
│   └── ReportScheduler.java             (opsiyonel)
└── controller/
    ├── GoalController.java               (güncelleme — export endpoints)
    └── UserController.java               (güncelleme — monthly report)
```


---

## 💡 İş Kuralları

### Excel Sayfa 1 — Özet Alanları
```
Satır 1: Hedef Adı (H1 bold, büyük font)
Satır 2: boş
Satır 3: "Birim:"           | {unit}
Satır 4: "Hedef Değer:"     | {targetValue}
Satır 5: "Başlangıç:"       | {startDate}
Satır 6: "Bitiş:"           | {endDate}
Satır 7: "Tamamlanma:"      | {completionPct}%
Satır 8: "Gerçekleşen:"     | {currentProgress} {unit}
Satır 9: "Fark (Gap):"      | {gap} (yeşil + ise, kırmızı - ise)
Satır 10: "Kalan Gerekli:"  | {requiredRate}/gün
Satır 11: "Mevcut Streak:"  | {currentStreak} gün
```

### PDF Türkçe Karakter Sorunu
```java
// iText 7'de Türkçe
PdfFont font = PdfFontFactory.createFont(
    StandardFonts.HELVETICA, PdfEncodings.IDENTITY_H
);
// Veya embed font:
PdfFont font = PdfFontFactory.createFont("path/to/arial.ttf", PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED);
```

### CSV UTF-8 BOM
```java
// Excel'in Türkçe karakterleri doğru okuyabilmesi için BOM ekle
baos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}); // UTF-8 BOM
```

### Dosya Adı Normalizasyonu
```java
// "Kitap Okuma!" → "kitap-okuma" (export güvenli dosya adı)
String safe = Normalizer.normalize(title, Normalizer.Form.NFD)
    .replaceAll("[^\\p{ASCII}]", "")
    .replaceAll("[^a-zA-Z0-9\\-]", "-")
    .toLowerCase()
    .replaceAll("-+", "-");
```

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ SXSSFWorkbook yerine XSSFWorkbook kullanmak (büyük veri)
//    → Entry sayısı 1000+ olabilir, SXSSFWorkbook (streaming) kullan

// ❌ iText 7'de Türkçe karakter desteğini atlamak
//    → HELVETICA + IDENTITY_H encoding zorunlu

// ❌ CSV'de virgül içeren not alanlarını tırnak içine almamak → RFC 4180 ihlali
//    → StringEscapeUtils.escapeCsv() veya manuel tırnak ekle

// ❌ Büyük PDF'lerde memory sorunu
//    → ByteArrayOutputStream yerine streaming yaklaşım (PdfWriter + OutputStream)

// ❌ Export endpoint'lerinde ownership kontrolü yapmamak → güvenlik açığı

// ❌ Content-Disposition header'ında Türkçe dosya adı doğrudan kullanmak
//    → RFC 5987: filename*=UTF-8''... encoding kullan veya normalize et
```

```typescript
// ❌ responseType: 'blob' ayarlamadan export isteği yapmak → bozuk dosya
// ❌ URL.revokeObjectURL() yapmamak → memory leak
// ❌ İndirme sırasında kullanıcıyı başka sayfaya yönlendirmek → indirme kesilir
```

---

## 🧪 Test Senaryoları

### Backend Unit (`ExportService`)
- [ ] Excel oluşturuluyor, 3 sayfa mevcut
- [ ] PDF oluşturuluyor, Türkçe karakterler doğru
- [ ] CSV oluşturuluyor, UTF-8 BOM mevcut
- [ ] Başka kullanıcının hedefini export etme → 403
- [ ] Entry'siz hedef export → boş tablolar, crash yok
- [ ] 500+ entry → bellek hatası yok (streaming)

### Frontend
- [ ] Excel butonu → `.xlsx` dosyası indiriliyor
- [ ] PDF butonu → `.pdf` dosyası indiriliyor
- [ ] CSV butonu → `.csv` dosyası indiriliyor
- [ ] İndirme sırasında buton disabled + spinner gösteriliyor
- [ ] İndirme tamamlanınca buton normal haline dönüyor

---

## ✅ Kabul Kriterleri

### Excel
- [ ] İndirilen `.xlsx` Excel'de açılıyor (hata yok)
- [ ] 3 sayfa mevcut: Özet, İlerleme Kayıtları, İstatistikler
- [ ] Header satırları bold, sütunlar otomatik genişlik
- [ ] Türkçe karakterler (ş, ğ, ü, ö, ı, ç) doğru görünüyor
- [ ] Gap pozitifse yeşil, negatifse kırmızı renk

### PDF
- [ ] İndirilen `.pdf` açılıyor (hata yok)
- [ ] Türkçe karakterler doğru
- [ ] Sayfa numarası ve hedef adı header/footer'da
- [ ] Entry tablosu sütun hizalaması düzgün

### CSV
- [ ] İndirilen `.csv` Excel'de Türkçe karakter sorunu olmadan açılıyor (UTF-8 BOM)
- [ ] Virgül içeren not alanları tırnak içinde
- [ ] Header satırı: `Tarih,Değer,Birim,Not`

### Güvenlik & Genel
- [ ] Sadece hedef sahibi export edebilir (başkasının hedefi → 403)
- [ ] Export sırasında frontend butonu disabled + spinner gösteriyor
- [ ] İndirme başarısız olursa Türkçe hata mesajı gösteriliyor
- [ ] Aylık rapor PDF endpoint'i çalışıyor
