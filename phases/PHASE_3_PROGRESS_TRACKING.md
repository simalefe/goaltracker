# 📊 Faz 3 — İlerleme Takibi (Progress Tracking)

| Field | Value |
|-------|-------|
| Version | 2.0.0 |
| Status | Enhanced — Architect Review Complete |
| Document Owner | Senior Architect |
| Last Updated | 2026-02-28 |
| Estimated Duration | 1 hafta (5 iş günü) |
| Dependency | Faz 2 (Goal entity, `GoalService.getGoalById()`, ownership pattern hazır olmalı) |
| Hedef | Günlük/periyodik ilerleme kayıtları, `GoalCalculator` hesaplama motoru, istatistik ve grafik veri endpoint'leri, `GoalDetailPage` tamamlanması, RATE tipi hesaplama, timezone yönetimi. |

---

## 📝 Changes Made

| # | Change | Reason |
|---|--------|--------|
| 1 | `## SQL DDL — Flyway Migration` bölümü eklendi (goal_entries tablosu tam DDL) | DB şema belirsizliğini gidermek |
| 2 | `## TypeScript Interfaces` bölümü eklendi (8+ interface) | Frontend tip güvenliği |
| 3 | `## JSON Request/Response Examples` bölümü eklendi (tüm endpoint'ler, hata senaryoları dahil) | API sözleşme örnekleri |
| 4 | `## Timezone Handling` bölümü eklendi | Tarih tutarsızlığı riskini gidermek |
| 5 | `## Performance Optimization` bölümü eklendi (stats caching, aggregation) | Büyük veri setleri için performans |
| 6 | `## Security Considerations` bölümü eklendi | Ownership chain, input validation |
| 7 | `## Risk Assessment` bölümü eklendi | Sıfıra bölme, timezone, performans riskleri |
| 8 | RATE tipi hesaplama formülü detaylandırıldı (`totalPeriods`, `elapsedPeriods`) | Eksik formül tamamlandı |
| 9 | GoalCalculator edge case'leri eklendi (startDate > today, totalDays == 0, startDate == endDate) | Hata koruması |
| 10 | PAUSED hedeflere entry davranışı `[DECISION REQUIRED]` olarak işaretlendi | Belirsiz karar |
| 11 | `GoalStatsResponse`'a `trackingStatus` (BEHIND/ON_TRACK/AHEAD) eklendi | UX kolaylığı |
| 12 | Entry creation flow diagram eklendi | Akış görselleştirme |
| 13 | `@Transactional` gereksinimleri detaylandırıldı | Atomik operasyon garantisi |

---

## 📋 Görev Listesi

### Backend — Entity & Repository
- [ ] `GoalEntry.java` — Entity:
  - `@Entity`, `@Table(name = "goal_entries")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `Goal`
  - `entry_date` → `LocalDate`
  - `actual_value` → `BigDecimal` (≥ 0)
  - `note` → `String` (max 500 karakter, opsiyonel)
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "entry_date"}))` — günde bir kayıt
  - `@CreationTimestamp` / `@UpdateTimestamp`
- [ ] `GoalEntryRepository.java`:
  - `List<GoalEntry> findByGoalIdOrderByEntryDateDesc(Long goalId)`
  - `Optional<GoalEntry> findByGoalIdAndEntryDate(Long goalId, LocalDate date)`
  - `boolean existsByGoalIdAndEntryDate(Long goalId, LocalDate date)`
  - `@Query("SELECT SUM(e.actualValue) FROM GoalEntry e WHERE e.goal.id = :goalId") BigDecimal sumActualValueByGoalId(Long goalId)`
  - `@Query("SELECT COALESCE(SUM(e.actualValue), 0) FROM GoalEntry e WHERE e.goal.id = :goalId") BigDecimal sumActualValueByGoalIdSafe(Long goalId)` — null-safe
  - `List<GoalEntry> findByGoalIdAndEntryDateBetween(Long goalId, LocalDate from, LocalDate to)`
  - `long countByGoalIdAndEntryDateBetween(Long goalId, LocalDate from, LocalDate to)` — RATE tipi için
  - `long countByGoalId(Long goalId)` — toplam entry sayısı

### Backend — GoalEntry Service
- [ ] `GoalEntryService.java`:
  - `getEntries(Long goalId, Long userId)` — ownership kontrolü
  - `createEntry(Long goalId, Long userId, CreateEntryRequest)`:
    - Ownership kontrolü (goal.user.id == userId)
    - Hedefin durumu kontrol et:
      - `ARCHIVED` → entry engeli → `GoalNotActiveException`
      - `COMPLETED` → entry engeli → `GoalNotActiveException`
      - `PAUSED` → **[DECISION REQUIRED: PAUSED hedefe entry kabul edilecek mi? Öneri: HAYIR — önce ACTIVE'e dönsün]**
    - Entry tarihi hedef aralığında mı? (`startDate <= entryDate <= endDate`) → `EntryOutOfRangeException`
    - Duplicate entry kontrolü → `DuplicateEntryException` (409)
    - Entry kaydet
    - **Spring Event publish:** `GoalEntryCreatedEvent(goalId, userId, entry)` → (Faz 5 streak + badge için)
    - Hedef tamamlandıysa (`completionPct >= 100`) status otomatik COMPLETED yapılabilir
      > **[DECISION REQUIRED: Auto-complete özelliği açılacak mı? Varsayılan: HAYIR — kullanıcı manuel tamamlasın]**
  - `updateEntry(Long entryId, Long userId, UpdateEntryRequest)`:
    - Entry'nin goal sahibinin userId olduğunu doğrula (ownership chain)
    - actualValue güncelle, note güncelle
    - **Spring Event publish:** `GoalEntryUpdatedEvent` (Faz 5 streak recalc için)
  - `deleteEntry(Long entryId, Long userId)`:
    - Ownership doğrula
    - Sil → streak güncelleme event'i publish: `GoalEntryDeletedEvent`
  - `getEntryById(Long entryId, Long userId)` — ownership kontrolü
  - Tüm CUD metotları: `@Transactional` (entry + event publish atomik olmalı)

- [ ] `GoalCalculator.java` — `@Component` utility:
  - `BigDecimal calculateCurrentProgress(Long goalId)` → `SUM(actual_value)` (null-safe, COALESCE)
  - `BigDecimal calculatePlannedProgress(Goal goal, LocalDate asOfDate)`:
    - **DAILY/CUMULATIVE:**
      ```java
      totalDays = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getEndDate()) + 1;
      daysSinceStart = ChronoUnit.DAYS.between(goal.getStartDate(), asOfDate) + 1;
      daysSinceStart = Math.max(0, Math.min(daysSinceStart, totalDays)); // clamp
      planned = targetValue × (daysSinceStart / totalDays)
      ```
    - **RATE:**
      ```java
      // Örnek: "Haftada 3 antrenman" → frequency=WEEKLY, targetValue=3
      // totalPeriods = hedef süresindeki toplam hafta sayısı
      // elapsedPeriods = başlangıçtan bugüne kadar geçen hafta sayısı
      totalPeriods = calculateTotalPeriods(goal);
      elapsedPeriods = calculateElapsedPeriods(goal, asOfDate);
      planned = targetValue × elapsedPeriods;  // toplam beklenen antrenman sayısı
      ```
  - `long calculateTotalPeriods(Goal goal)`:
    ```java
    long totalDays = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getEndDate()) + 1;
    return switch (goal.getFrequency()) {
        case DAILY -> totalDays;
        case WEEKLY -> (totalDays + 6) / 7;   // ceiling division
        case MONTHLY -> ChronoUnit.MONTHS.between(goal.getStartDate(), goal.getEndDate()) + 1;
    };
    ```
  - `long calculateElapsedPeriods(Goal goal, LocalDate asOfDate)`:
    ```java
    long daysSinceStart = ChronoUnit.DAYS.between(goal.getStartDate(), asOfDate) + 1;
    daysSinceStart = Math.max(0, daysSinceStart);
    return switch (goal.getFrequency()) {
        case DAILY -> daysSinceStart;
        case WEEKLY -> (daysSinceStart + 6) / 7;
        case MONTHLY -> ChronoUnit.MONTHS.between(goal.getStartDate(), asOfDate) + 1;
    };
    ```
  - `BigDecimal calculateGap(Goal goal)` → `currentProgress - plannedProgress`
  - `BigDecimal calculateRequiredRate(Goal goal)`:
    ```java
    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getEndDate());
    if (daysLeft <= 0) return BigDecimal.ZERO;  // SIFIRA BÖLME KORUMASI!
    BigDecimal remaining = goal.getTargetValue().subtract(currentProgress);
    if (remaining.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO; // zaten tamamlanmış
    return remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP);
    ```
  - `int calculateDaysLeft(Goal goal)` → `max(0, endDate - today)`
  - `BigDecimal calculateCompletionPct(Goal goal)`:
    ```java
    if (goal.getTargetValue().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO; // sıfıra bölme
    return currentProgress
        .divide(goal.getTargetValue(), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .min(new BigDecimal("100.00"));
    ```
  - `BigDecimal calculateExpectedPct(Goal goal)` → `(daysSinceStart / totalDays) × 100`
  - `String determineTrackingStatus(Goal goal)`:
    ```java
    BigDecimal gap = calculateGap(goal);
    if (gap.compareTo(BigDecimal.ZERO) > 0) return "AHEAD";
    if (gap.compareTo(BigDecimal.ZERO) == 0) return "ON_TRACK";
    return "BEHIND";
    ```
  - `List<ChartDataPointResponse> buildChartData(Goal goal, List<GoalEntry> entries)`:
    - startDate'ten TODAY'e (veya endDate, hangisi daha erken) kadar HER GÜN
    - Kümülatif planned ve actual hesapla
    - Entry olmayan günlerde `dailyActual = null`, `actual = önceki günün kümülatif değeri`

  **Edge Case'ler:**
  - `startDate > today` (henüz başlamamış hedef) → `daysSinceStart = 0`, `plannedProgress = 0`
  - `startDate == endDate` → `totalDays = 1`
  - `totalDays == 0` → hesaplama yapma, `completionPct = 0`
  - `targetValue == 0` → sıfıra bölme → `completionPct = 0` (validation zaten engelliyor ama defensive)
  - `currentProgress > targetValue` → `completionPct = 100.00` (max cap)
  - `daysLeft < 0` (süre dolmuş) → `daysLeft = 0`, `requiredRate = 0`

### Backend — Stats & Chart Endpoint'leri
- [ ] `GoalController.java` güncelleme:
  - `GET /api/goals/{id}/stats` → `GoalStatsResponse`
  - `GET /api/goals/{id}/chart-data` → `ChartDataResponse`
- [ ] `GoalEntryController.java`:
  - `GET /api/goals/{goalId}/entries`
  - `POST /api/goals/{goalId}/entries`
  - `PUT /api/entries/{entryId}`
  - `DELETE /api/entries/{entryId}`

### Backend — DTO'lar
- [ ] `CreateEntryRequest.java`:
  ```java
  public record CreateEntryRequest(
      @NotNull LocalDate entryDate,
      @NotNull @DecimalMin("0.0") BigDecimal actualValue,
      @Size(max = 500) String note
  ) {}
  ```
- [ ] `UpdateEntryRequest.java`:
  ```java
  public record UpdateEntryRequest(
      @DecimalMin("0.0") BigDecimal actualValue,
      @Size(max = 500) String note
  ) {}
  // Her iki alan da opsiyonel — partial update
  ```
- [ ] `GoalEntryResponse.java`:
  ```java
  public record GoalEntryResponse(
      Long id,
      Long goalId,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
      BigDecimal actualValue,
      String note,
      @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Instant createdAt
  ) {}
  ```
- [ ] `GoalStatsResponse.java`:
  ```java
  public record GoalStatsResponse(
      BigDecimal currentProgress,    // toplam gerçekleşen
      BigDecimal targetValue,        // hedef değer
      BigDecimal completionPct,      // tamamlanma %
      BigDecimal expectedPct,        // bu noktada beklenen %
      BigDecimal gap,                // fark (+ ileride, - geride)
      String trackingStatus,         // "AHEAD" | "ON_TRACK" | "BEHIND"
      BigDecimal requiredRate,       // kalan gün başına gereken
      String unit,                   // birim (UI'da gösterim için)
      int daysLeft,                  // kalan gün
      int totalDays,                 // toplam gün
      int daysSinceStart,            // başlangıçtan bu yana gün
      int entryCount,                // toplam entry sayısı
      int currentStreak,             // mevcut streak (Faz 5'te doldurulacak, şimdilik 0)
      int longestStreak              // en uzun streak (Faz 5'te doldurulacak, şimdilik 0)
  ) {}
  ```
- [ ] `ChartDataPointResponse.java`:
  ```java
  public record ChartDataPointResponse(
      String date,                   // "2026-03-01" veya "Gün 1"
      BigDecimal planned,            // kümülatif planlanan
      BigDecimal actual,             // kümülatif gerçekleşen (null olabilir)
      BigDecimal dailyActual         // sadece o günün değeri (null olabilir)
  ) {}
  ```
- [ ] `ChartDataResponse.java`:
  ```java
  public record ChartDataResponse(
      List<ChartDataPointResponse> dataPoints,
      BigDecimal dailyTarget         // targetValue / totalDays (grafik referans çizgisi için)
  ) {}
  ```

### Backend — Exception'lar
- [ ] `GoalEntryNotFoundException.java` → 404
- [ ] `DuplicateEntryException.java` → 409
- [ ] `EntryOutOfRangeException.java` → 400 (tarih hedef aralığı dışında)
- [ ] `GoalNotActiveException.java` → 400 (ARCHIVED/COMPLETED/PAUSED hedefe entry denemesi)

### Backend — Event Altyapısı (Faz 5 için hazırlık)
- [ ] `GoalEntryCreatedEvent.java` — `ApplicationEvent` alt sınıfı (`goalId`, `userId`, `entryDate`, `actualValue`)
- [ ] `GoalEntryUpdatedEvent.java` — `ApplicationEvent` alt sınıfı
- [ ] `GoalEntryDeletedEvent.java` — `ApplicationEvent` alt sınıfı
- [ ] `GoalService.java` güncelleme — `applicationEventPublisher.publishEvent()`

### Backend — Mapper
- [ ] `GoalEntryMapper.java` — `GoalEntry` → `GoalEntryResponse`, `CreateEntryRequest` → `GoalEntry`

### Frontend — GoalDetailPage Tamamlama (Thymeleaf)
- [ ] `templates/goals/detail.html` — tam implementasyon:
  - Üst kısım: `th:text="${goal.title}"`, kategori, tarih aralığı, birim, durum badge
  - İstatistik kartları (`th:each` ile model'den veri): currentProgress, completionPct, gap, requiredRate, daysLeft
  - Entry listesi tablosu: `th:each="entry : ${entries}"` — tarih, değer, birim, not, düzenleme/silme
  - Entry ekleme formu (inline Bootstrap modal veya accordion)
  - Grafik alanı (iskelet — Phase 4'te Chart.js ile doldurulacak)
  - Durum değiştirme butonları (form submit)
  - Flash mesajı ile silme/güncelleme onayı
- [ ] `templates/goals/entries/` — Entry form fragment'leri:
  - `add-entry-modal.html` — Bootstrap modal fragment
  - `edit-entry-modal.html` — Bootstrap modal fragment
- [ ] `GoalEntryController.java` (MVC Controller):
  - `POST /goals/{goalId}/entries` → entry oluştur, redirect back
  - `GET /goals/{goalId}/entries/{id}/edit` → edit formu
  - `POST /goals/{goalId}/entries/{id}/edit` → güncelle, redirect
  - `POST /goals/{goalId}/entries/{id}/delete` → sil, redirect

---

## 🗃️ SQL DDL — Flyway Migration

### V3__create_goal_entries.sql
```sql
CREATE TABLE goal_entries (
    id              BIGSERIAL       PRIMARY KEY,
    goal_id         BIGINT          NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    entry_date      DATE            NOT NULL,
    actual_value    NUMERIC(12,2)   NOT NULL CHECK (actual_value >= 0),
    note            VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_goal_entry_date UNIQUE (goal_id, entry_date)
);

-- Performance indexes
CREATE INDEX idx_goal_entries_goal_id ON goal_entries (goal_id);
CREATE INDEX idx_goal_entries_goal_date ON goal_entries (goal_id, entry_date DESC);
CREATE INDEX idx_goal_entries_date ON goal_entries (entry_date);

COMMENT ON TABLE goal_entries IS 'Hedef ilerleme kayıtları — her hedefe günde 1 kayıt';
COMMENT ON COLUMN goal_entries.actual_value IS 'Gerçekleşen değer — NUMERIC(12,2), >= 0';
COMMENT ON CONSTRAINT uq_goal_entry_date ON goal_entries IS 'Bir hedefe günde yalnızca 1 entry girilebilir';
```

---

## 📐 TypeScript Interfaces

```typescript
// ========================
// Entry Types
// ========================
interface GoalEntryResponse {
  id: number;
  goalId: number;
  entryDate: string;           // "yyyy-MM-dd"
  actualValue: number;
  note: string | null;
  createdAt: string;           // ISO 8601
}

interface CreateEntryRequest {
  entryDate: string;           // "yyyy-MM-dd"
  actualValue: number;         // >= 0
  note?: string;               // max 500 chars
}

interface UpdateEntryRequest {
  actualValue?: number;
  note?: string;
}

// ========================
// Stats Types
// ========================
type TrackingStatus = 'AHEAD' | 'ON_TRACK' | 'BEHIND';

interface GoalStatsResponse {
  currentProgress: number;
  targetValue: number;
  completionPct: number;       // 0.00 - 100.00
  expectedPct: number;
  gap: number;                 // + ileride, - geride
  trackingStatus: TrackingStatus;
  requiredRate: number;
  unit: string;
  daysLeft: number;
  totalDays: number;
  daysSinceStart: number;
  entryCount: number;
  currentStreak: number;       // Faz 5
  longestStreak: number;       // Faz 5
}

// ========================
// Chart Types
// ========================
interface ChartDataPoint {
  date: string;
  planned: number;
  actual: number | null;
  dailyActual: number | null;
}

interface ChartDataResponse {
  dataPoints: ChartDataPoint[];
  dailyTarget: number;
}
```

---

## 🔌 API Endpoint'leri

### GET `/api/goals/{id}/entries`
```
Authorization: Bearer <token>
→ 200 ApiResponse<List<GoalEntryResponse>>  (tarih DESC)
```

### POST `/api/goals/{id}/entries`
```
Body: { entryDate: "2026-02-28", actualValue: 2.5, note: "İyi bir gün" }
→ 201 ApiResponse<GoalEntryResponse>
→ 409 Conflict  (aynı gün zaten var — DuplicateEntryException)
→ 400 Bad Request  (tarih hedef aralığı dışında)
→ 400 Bad Request  (hedef ARCHIVED/COMPLETED/PAUSED)
```

### PUT `/api/entries/{entryId}`
```
Body: { actualValue: 3.0, note: "Güncellendi" }
→ 200 ApiResponse<GoalEntryResponse>
→ 403 (başkasının entry'si)
→ 404 (entry bulunamadı)
```

### DELETE `/api/entries/{entryId}`
```
→ 204 No Content
→ 403 | 404
```

### GET `/api/goals/{id}/stats`
```
→ 200 ApiResponse<GoalStatsResponse>
```

### GET `/api/goals/{id}/chart-data`
```
→ 200 ApiResponse<ChartDataResponse>
```

---

## 📦 JSON Request/Response Examples

### Create Entry — Success (201)
```json
// POST /api/goals/1/entries
// Request:
{
  "entryDate": "2026-03-05",
  "actualValue": 25.00,
  "note": "Bugün 25 sayfa okudum"
}

// Response (201):
{
  "success": true,
  "data": {
    "id": 1,
    "goalId": 1,
    "entryDate": "2026-03-05",
    "actualValue": 25.00,
    "note": "Bugün 25 sayfa okudum",
    "createdAt": "2026-03-05T20:30:00Z"
  },
  "message": "İlerleme kaydedildi.",
  "errorCode": null,
  "timestamp": "2026-03-05T20:30:00Z"
}
```

### Create Entry — Duplicate (409)
```json
{
  "success": false,
  "data": null,
  "message": "Bu tarihte zaten bir kayıt mevcut: 2026-03-05",
  "errorCode": "DUPLICATE_ENTRY",
  "timestamp": "2026-03-05T20:31:00Z"
}
```

### Create Entry — Out of Range (400)
```json
{
  "success": false,
  "data": null,
  "message": "Kayıt tarihi hedef aralığı dışında. Hedef: 2026-03-01 — 2026-03-31",
  "errorCode": "ENTRY_OUT_OF_RANGE",
  "timestamp": "2026-03-05T20:31:00Z"
}
```

### Create Entry — Goal Not Active (400)
```json
{
  "success": false,
  "data": null,
  "message": "Bu hedefe kayıt eklenemez. Hedef durumu: ARCHIVED",
  "errorCode": "GOAL_NOT_ACTIVE",
  "timestamp": "2026-03-05T20:31:00Z"
}
```

### Get Stats (200)
```json
// GET /api/goals/1/stats
{
  "success": true,
  "data": {
    "currentProgress": 125.00,
    "targetValue": 300.00,
    "completionPct": 41.67,
    "expectedPct": 16.13,
    "gap": 76.52,
    "trackingStatus": "AHEAD",
    "requiredRate": 7.29,
    "unit": "sayfa",
    "daysLeft": 24,
    "totalDays": 31,
    "daysSinceStart": 7,
    "entryCount": 5,
    "currentStreak": 0,
    "longestStreak": 0
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-03-07T12:00:00Z"
}
```

### Get Chart Data (200)
```json
// GET /api/goals/1/chart-data
{
  "success": true,
  "data": {
    "dailyTarget": 9.68,
    "dataPoints": [
      { "date": "2026-03-01", "planned": 9.68,  "actual": 15.00, "dailyActual": 15.00 },
      { "date": "2026-03-02", "planned": 19.35, "actual": 35.00, "dailyActual": 20.00 },
      { "date": "2026-03-03", "planned": 29.03, "actual": 35.00, "dailyActual": null },
      { "date": "2026-03-04", "planned": 38.71, "actual": 60.00, "dailyActual": 25.00 },
      { "date": "2026-03-05", "planned": 48.39, "actual": 85.00, "dailyActual": 25.00 },
      { "date": "2026-03-06", "planned": 58.06, "actual": 100.00,"dailyActual": 15.00 },
      { "date": "2026-03-07", "planned": 67.74, "actual": 125.00,"dailyActual": 25.00 }
    ]
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-03-07T12:00:00Z"
}
```

---

## 🔄 Entry Creation Flow

```
Client                    GoalEntryService              GoalRepo    EntryRepo     EventPublisher
  |--- POST entry -------->|                             |            |              |
  |                        |-- findById(goalId) -------->|            |              |
  |                        |<-- Goal -------------------|            |              |
  |                        |                             |            |              |
  |                        |-- goal.user.id == userId? ->|            |              |
  |                        |   NO → 403 ACCESS_DENIED    |            |              |
  |                        |                             |            |              |
  |                        |-- goal.status == ACTIVE? -->|            |              |
  |                        |   NO → 400 GOAL_NOT_ACTIVE  |            |              |
  |                        |                             |            |              |
  |                        |-- startDate <= entryDate -->|            |              |
  |                        |   <= endDate?               |            |              |
  |                        |   NO → 400 OUT_OF_RANGE     |            |              |
  |                        |                             |            |              |
  |                        |-- existsByGoalIdAndDate? -->|            |              |
  |                        |   YES → 409 DUPLICATE       |            |------------->|
  |                        |                             |            |              |
  |                        |-- save(entry) ------------->|            |              |
  |                        |                             |            |<-- saved -----|
  |                        |                             |            |              |
  |                        |-- publishEvent(Created) --->|            |              |-->
  |                        |                             |            |              |
  |<-- 201 EntryResponse --|                             |            |              |
```

---

## 🕐 Timezone Handling

> **[DECISION REQUIRED: Aşağıdaki stratejilerden biri seçilmelidir.]**

### Strateji A: Server UTC (Önerilen)
- Tüm `entry_date` değerleri `LocalDate` olarak saklanır (timezone bilgisi yok)
- Frontend, kullanıcının local tarihini `yyyy-MM-dd` formatında gönderir
- Backend tarih karşılaştırmalarını doğrudan `LocalDate` üzerinden yapar
- **Avantaj:** Basit, timezone karmaşıklığı yok
- **Dezavantaj:** Gece yarısı civarında farklı timezone'daki kullanıcılar yanlış güne kayıt girebilir

### Strateji B: User Timezone
- `User.timezone` alanı kullanılır (ör: `Europe/Istanbul`)
- Backend, "bugün" hesabını `ZonedDateTime.now(ZoneId.of(user.getTimezone()))` ile yapar
- **Avantaj:** Doğru "bugün" hesabı
- **Dezavantaj:** Daha karmaşık, timezone değişikliği edge case'leri

**Öneri:** Faz 3'te Strateji A ile başla, gerekirse Faz 9'da Strateji B'ye geç.

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── model/
│   ├── GoalEntry.java
│   └── event/
│       ├── GoalEntryCreatedEvent.java
│       ├── GoalEntryUpdatedEvent.java
│       └── GoalEntryDeletedEvent.java
├── repository/
│   └── GoalEntryRepository.java
├── service/
│   ├── GoalEntryService.java
│   └── GoalService.java               (güncelleme — stats, chart-data)
├── controller/
│   ├── GoalEntryController.java
│   └── GoalController.java            (güncelleme — stats, chart-data)
├── dto/
│   ├── request/
│   │   ├── CreateEntryRequest.java
│   │   └── UpdateEntryRequest.java
│   └── response/
│       ├── GoalEntryResponse.java
│       ├── GoalStatsResponse.java
│       ├── ChartDataPointResponse.java
│       └── ChartDataResponse.java
├── mapper/
│   └── GoalEntryMapper.java
├── util/
│   └── GoalCalculator.java
└── exception/
    ├── GoalEntryNotFoundException.java
    ├── DuplicateEntryException.java
    ├── EntryOutOfRangeException.java
    └── GoalNotActiveException.java
```

### Flyway Migration
```
src/main/resources/db/migration/
└── V3__create_goal_entries.sql
```

### Thymeleaf Templates
```
src/main/resources/templates/
└── goals/
    ├── detail.html         (Phase 2 iskeleti → tam implementasyon)
    └── entries/
        └── entry-form.html (HTMX veya modal fragment)
```

---

## 💡 İş Kuralları

### Hesaplama Formülleri (GoalCalculator)
```java
// Temel değişkenler
long totalDays      = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getEndDate()) + 1;
long daysSinceStart = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now()) + 1;
long daysLeft       = ChronoUnit.DAYS.between(LocalDate.now(), goal.getEndDate());
// daysLeft negatif olabilir! → max(0, daysLeft) kullan

// Kümülatif ilerleme (DAILY / CUMULATIVE tipler)
plannedProgress = targetValue.multiply(
    BigDecimal.valueOf(daysSinceStart).divide(BigDecimal.valueOf(totalDays), 4, HALF_UP)
);

// RATE tipi (Haftada 3 antrenman örneği)
// frequency = WEEKLY, targetValue = 3
// Toplam hafta = (totalDays + 6) / 7
// Geçen hafta = (daysSinceStart + 6) / 7
// Beklenen antrenman = targetValue × geçen hafta sayısı
// Gerçekleşen = COUNT(entries) in elapsed period

// Fark
gap = currentProgress.subtract(plannedProgress);  // + ileride, - geride

// Gereken oran (SIFIRA BÖLME KORUMASI!)
requiredRate = daysLeft > 0
    ? (targetValue.subtract(currentProgress)).divide(BigDecimal.valueOf(daysLeft), 2, HALF_UP)
    : BigDecimal.ZERO;

// Tamamlanma yüzdesi (maks 100)
completionPct = currentProgress.divide(targetValue, 4, HALF_UP)
    .multiply(BigDecimal.valueOf(100))
    .min(new BigDecimal("100.00"));
```

### Chart Data Üretimi
```
startDate'ten TODAY'e (veya endDate, hangisi daha erken) kadar HER GÜN için:
  - date: "2026-03-01" (ISO formatı)
  - planned: Kümülatif planlanan (her gün artarak)
  - actual: O güne kadar kümülatif toplam
    - Entry yoksa → önceki günün kümülatif değerini taşı (null DEĞİL — çizgi devam etsin)
    - İlk gün ve entry yoksa → null (çizgi başlamaz)
  - dailyActual: Sadece o günün entry değeri (entry yoksa null)
```

### Entry Kuralları
- Bir hedefe günde yalnızca **1 entry** girilebilir (DB UNIQUE constraint + service kontrolü)
- `actualValue` ≥ 0 (0 girilebilir — "bugün 0 yaptım" kaydı)
- Entry tarihi `startDate` ile `endDate` arasında olmalı (inclusive)
- ARCHIVED / COMPLETED hedeflere entry girilemez
- PAUSED hedeflere entry: **[DECISION REQUIRED]**
- Entry silindiğinde istatistikler anında değişir (hesaplama her seferinde yapılır, cache yok — Faz 4'te cache eklenebilir)

### Ownership Zinciri (Entry için)
```java
// Entry → Goal → User ownership chain
GoalEntry entry = entryRepository.findById(entryId)
    .orElseThrow(() -> new GoalEntryNotFoundException(entryId));
if (!entry.getGoal().getUser().getId().equals(currentUserId)) {
    throw new GoalAccessDeniedException(entry.getGoal().getId());
}
```

---

## 🛡️ Security Considerations

### Ownership Chain Validation
- Entry'ler doğrudan userId ile ilişkilendirilmez → `entry.goal.user.id` zinciri kullanılır
- Entry CRUD'da hem goal ownership hem entry existence kontrolü yapılır
- İlk önce goal ownership, sonra entry existence → bilgi sızıntısı önlenir (başka kullanıcının entry sayısını öğrenemez)

### Input Validation
- `actualValue` → `BigDecimal`, `@DecimalMin("0.0")` — negatif değer kabul edilmez
- `note` → max 500 karakter, HTML sanitize edilmeli
- `entryDate` → `LocalDate` parse hatası → 400 Bad Request
- BigDecimal precision: NUMERIC(12,2) → max 9999999999.99, scale=2

### Data Integrity
- `UNIQUE(goal_id, entry_date)` constraint → DB seviyesinde duplicate koruması
- `ON DELETE CASCADE` → goal silinince entry'ler de silinir (orphan bırakmaz)
- `@Transactional` → entry + event publish atomik

---

## ⚡ Performance Optimization

### Database Aggregation
```java
// ❌ Kötü — tüm entry'leri belleğe çek, Java'da topla
BigDecimal sum = entries.stream()
    .map(GoalEntry::getActualValue)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// ✅ İyi — DB'de aggregate
@Query("SELECT COALESCE(SUM(e.actualValue), 0) FROM GoalEntry e WHERE e.goal.id = :goalId")
BigDecimal sumActualValueByGoalIdSafe(@Param("goalId") Long goalId);
```

### Stats Caching Strategy
> **[DECISION REQUIRED: Stats cache uygulanacak mı? Önerilen yaklaşım:]**
- Stats hesaplama her istekte DB'ye gider (gerçek zamanlı doğruluk)
- Dashboard'da toplu stats isteği olduğunda (Faz 4) → Spring `@Cacheable` + Caffeine, TTL 5 dakika
- Cache invalidation: entry CUD operasyonlarında `@CacheEvict`

### Chart Data Generation
- Büyük veri setleri (180+ gün) için chart data:
  - DB'den sadece entry'leri çek (az veri)
  - Planlanan değerler Java'da hesapla (CPU — DB sorgusu yok)
  - Map<LocalDate, BigDecimal> yapısı ile O(1) lookup

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ daysLeft = 0 → ArithmeticException (sıfıra bölme)
//    → daysLeft <= 0 ise requiredRate = ZERO

// ❌ BigDecimal yerine double/float → hassasiyet kaybı
//    → NUMERIC(12,2) → BigDecimal, HALF_UP rounding

// ❌ Kümülatif chart data'da null yerine 0 göstermek
//    → Recharts'ta null = bağlantı kesilir (connectNulls=false), 0 = yanlış veri

// ❌ Her stats isteğinde tüm entry'leri belleğe almak → büyük veri setleri
//    → SUM, COUNT aggregation sorgularını DB'de yap

// ❌ Entry tarih validasyonunu sadece frontend'de yapmak
//    → Backend'de de goal.startDate <= entryDate <= goal.endDate kontrolü

// ❌ ARCHIVED hedeflere entry kabul etmek
//    → GoalEntryService'te status kontrolü ekle

// ❌ @Transactional eksik → entry + event publish atomik olmalı

// ❌ RATE tipi için SUM yerine COUNT kullanmayı unutmak
//    → RATE tipi: COUNT(entries in period) vs targetValue

// ❌ totalDays = 0 durumunu handle etmemek
//    → startDate == endDate ise totalDays = 1

// ❌ NULL SUM sonucunu handle etmemek (entry yokken SUM = NULL)
//    → COALESCE(SUM(...), 0) veya Java'da null check
```

```typescript
// ❌ Entry mutation sonrası sadece entry listesini invalidate etmek
//    → Stats ve chart data da invalidate edilmeli:
//    queryClient.invalidateQueries({ queryKey: ['goalStats', goalId] })
//    queryClient.invalidateQueries({ queryKey: ['chartData', goalId] })

// ❌ 409 hata kodunu generic hata olarak göstermek
//    → "Bu tarihte zaten bir kayıt mevcut" Türkçe mesajı göster

// ❌ EntryFormModal'da tarih picker'ı sınırlandırmamak
//    → min={goal.startDate} max={goal.endDate}
```

---

## ⚡ Risk Assessment

| Risk | Olasılık | Etki | Azaltma |
|------|----------|------|---------|
| Sıfıra bölme (daysLeft, totalDays, targetValue) | Yüksek | Kritik | Defensive coding: tüm bölme işlemlerinde guard clause |
| Timezone kaynaklı yanlış gün hesabı | Orta | Orta | Strateji A (LocalDate) ile basit tut, gerekirse Faz 9'da timezone desteği |
| Büyük entry setlerinde (1000+) chart data performansı | Düşük | Orta | DB aggregation, Java'da planlanan hesabı, lazy loading |
| Concurrent entry creation (aynı gün, aynı hedef) | Düşük | Düşük | DB UNIQUE constraint → DataIntegrityViolationException → 409 |
| Event publish başarısızlığı | Düşük | Düşük | Event publish sync yapılır (Spring default), hata durumunda transaction rollback |
| NULL BigDecimal aggregation | Yüksek | Orta | COALESCE(SUM(...), 0) tüm aggregate sorgularda |

---

## 🧪 Test Senaryoları

### Backend Unit (`GoalCalculator`)
- [ ] `calculateRequiredRate`: daysLeft = 0 → `BigDecimal.ZERO` döner
- [ ] `calculateRequiredRate`: daysLeft = 5, targetValue = 100, currentProgress = 50 → 10.00
- [ ] `calculateRequiredRate`: currentProgress >= targetValue → `BigDecimal.ZERO` (zaten tamamlanmış)
- [ ] `calculateCompletionPct`: currentProgress > targetValue → 100.00 (max)
- [ ] `calculateCompletionPct`: targetValue = 0 → 0 (sıfıra bölme koruması)
- [ ] `calculateGap`: ileride → pozitif, geride → negatif
- [ ] `buildChartData`: entry olmayan günlerde dailyActual = null
- [ ] `buildChartData`: kümülatif actual doğru artıyor
- [ ] `calculateTotalPeriods`: WEEKLY, 30 gün → 5 hafta (ceiling)
- [ ] `calculateElapsedPeriods`: WEEKLY, 10 gün geçmiş → 2 hafta (ceiling)
- [ ] `determineTrackingStatus`: gap > 0 → "AHEAD", gap = 0 → "ON_TRACK", gap < 0 → "BEHIND"
- [ ] Edge case: startDate > today → daysSinceStart = 0, plannedProgress = 0
- [ ] Edge case: startDate == endDate → totalDays = 1

### Backend Unit (`GoalEntryService`)
- [ ] Başarılı entry oluşturma → 201
- [ ] Aynı gün aynı hedefe 2. entry → `DuplicateEntryException` (409)
- [ ] Tarih hedef aralığı dışında → `EntryOutOfRangeException` (400)
- [ ] `actualValue < 0` → 400 Bad Request (Bean Validation)
- [ ] ARCHIVED hedefe entry → 400 `GOAL_NOT_ACTIVE`
- [ ] COMPLETED hedefe entry → 400 `GOAL_NOT_ACTIVE`
- [ ] Başka kullanıcının hedefine entry → 403 Forbidden
- [ ] Entry oluşturma sonrası `GoalEntryCreatedEvent` publish edilir
- [ ] Entry silme sonrası `GoalEntryDeletedEvent` publish edilir

### Backend Entegrasyon
- [ ] `POST /api/goals/{id}/entries` → 201 + `GoalEntryResponse`
- [ ] `POST` aynı gün tekrar → 409 `DUPLICATE_ENTRY`
- [ ] `GET /api/goals/{id}/stats` → doğru hesaplanmış değerler
- [ ] `GET /api/goals/{id}/chart-data` → tüm günler için veri noktaları mevcut
- [ ] `PUT /api/entries/{id}` → değer güncelleniyor, stats değişiyor
- [ ] `DELETE /api/entries/{id}` → 204, stats anında güncelleniyor

### Frontend
- [ ] `EntryFormModal`: tarih hedef aralığı dışında seçilemesin
- [ ] 409 gelince "Bu tarihte zaten kayıt var" toast mesajı gösterilsin
- [ ] `StatsPanel`: gap pozitifse yeşil, negatifse kırmızı, sıfırsa mavi
- [ ] `EntryLogTable`: entry silince stats otomatik güncellensin
- [ ] Entry ekleme sonrası stats ve entry listesi güncelleniyor (TanStack invalidation)

---

## ✅ Kabul Kriterleri

### Entry CRUD
- [ ] Entry oluşturma (happy path) → 201, `GoalEntryResponse` dönülür
- [ ] Aynı gün aynı hedefe 2. entry → 409, `errorCode = "DUPLICATE_ENTRY"`
- [ ] `actualValue < 0` → 400, alan bazlı hata mesajı
- [ ] Tarih hedef aralığı dışında → 400, `errorCode = "ENTRY_OUT_OF_RANGE"`
- [ ] ARCHIVED hedefe entry → 400, `errorCode = "GOAL_NOT_ACTIVE"`
- [ ] Başka kullanıcının hedefine entry → 403
- [ ] Entry güncelleme çalışıyor (değer ve not değişiyor)
- [ ] Entry silme → 204, stats anında değişiyor

### Hesaplamalar
- [ ] `GoalCalculator.calculateRequiredRate()`: daysLeft = 0 durumunda sıfıra bölme hatası yok
- [ ] `completionPct`: currentProgress > targetValue → max 100 döner
- [ ] `gap`: doğru işaret (+ ileride, - geride)
- [ ] `trackingStatus`: gap'e göre doğru string döner
- [ ] `buildChartData`: her gün için planned hesaplanmış, sadece entry olan günlerde dailyActual dolu
- [ ] RATE tipi: entry sayısı bazlı hesaplama doğru çalışıyor

### Frontend
- [ ] `GoalDetailPage`: üst bilgi + StatsPanel + EntryFormModal + EntryLogTable tam çalışıyor
- [ ] `StatsPanel`: tüm metrikler gösteriliyor, gap rengi doğru
- [ ] `EntryLogTable`: tarih DESC sıralı, düzenleme/silme çalışıyor
- [ ] Entry eklendikten/silindikten sonra stats ve entry listesi TanStack Query ile otomatik güncelleniyor
- [ ] 409 hatası "Bu tarihte kayıt zaten mevcut" Türkçe mesajıyla gösteriliyor
- [ ] Boş entry listesi `EmptyState` bileşeni gösteriyor
- [ ] Tüm metinler Türkçe
