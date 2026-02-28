# 🎯 Faz 2 — Hedef Yönetimi (Goal Management)

| Field | Value |
|-------|-------|
| Version | 2.0.0 |
| Status | Enhanced — Architect Review Complete |
| Document Owner | Senior Architect |
| Last Updated | 2026-02-28 |
| Estimated Duration | 1 hafta (5 iş günü) |
| Dependency | Faz 1 (User entity, JWT, SecurityConfig, `getCurrentUserId()` helper hazır olmalı) |
| Hedef | Hedef CRUD işlemleri, durum yönetimi, ownership kontrolü, filtreleme/sıralama/sayfalama, frontend hedef sayfaları, optimistic locking. |

---

## 📝 Changes Made

| # | Change | Reason |
|---|--------|--------|
| 1 | `## SQL DDL — Flyway Migration` bölümü eklendi (goals tablosu tam DDL + index'ler) | DB şema belirsizliğini gidermek |
| 2 | `## TypeScript Interfaces` bölümü eklendi (12+ interface + enum'lar) | Frontend tip güvenliği |
| 3 | `## JSON Request/Response Examples` bölümü eklendi (tüm endpoint'ler) | API sözleşme örnekleri |
| 4 | `## Pagination Response Format` bölümü eklendi | Spring Page → frontend dönüşüm standardı |
| 5 | `## Performance Considerations` bölümü eklendi (index stratejisi, N+1 koruması) | Production performans gereksinimleri |
| 6 | `## Security Considerations` bölümü eklendi | Ownership enforcement pattern, input sanitization |
| 7 | `## Risk Assessment` bölümü eklendi | Race condition, veri tutarsızlığı riskleri |
| 8 | `@Version` (optimistic locking) Goal entity'ye eklendi | Concurrent update koruması |
| 9 | Durum geçiş matrisi eklendi (tüm geçerli/geçersiz geçişler tablo olarak) | Belirsizliği gidermek |
| 10 | Full-text search sorgusu eklendi | Kullanıcı arama deneyimi |
| 11 | `GoalCategory` enum'una `CAREER`, `HOBBY`, `SOCIAL` eklenmesi `[DECISION REQUIRED]` | Genişletilebilirlik |
| 12 | Goal duplication feature notu eklendi | Kullanıcı deneyimi |
| 13 | Sorting seçenekleri dokümante edildi | API kullanım kolaylığı |
| 14 | Swagger/OpenAPI annotation notu eklendi | API dokümantasyonu |

---

## 📋 Görev Listesi

### Backend — Entity & Enum'lar
- [ ] `GoalType.java` enum — `DAILY`, `CUMULATIVE`, `RATE`
- [ ] `GoalFrequency.java` enum — `DAILY`, `WEEKLY`, `MONTHLY`
- [ ] `GoalCategory.java` enum — `EDUCATION`, `HEALTH`, `FITNESS`, `FINANCE`, `PERSONAL`
  > **[DECISION REQUIRED: `CAREER`, `HOBBY`, `SOCIAL` eklensin mi? Kullanıcı custom kategori oluşturabilsin mi (enum yerine DB tablosu)?]**
- [ ] `GoalStatus.java` enum — `ACTIVE`, `PAUSED`, `COMPLETED`, `ARCHIVED`
- [ ] `Goal.java` — Entity:
  - `@Entity`, `@Table(name = "goals")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `User`
  - `@Enumerated(EnumType.STRING)` — tüm enum alanları
  - `target_value` → `BigDecimal` (double/float kullanma!)
  - `start_date` / `end_date` → `LocalDate`
  - `@Version` → `Long version` — optimistic locking (concurrent update koruması)
  - `@OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)` → entries
  - `@CreationTimestamp` / `@UpdateTimestamp`
  - DB constraint: `chk_goals_dates` (end_date > start_date), `chk_goals_target` (target_value > 0)

### Backend — Repository
- [ ] `GoalRepository.java`:
  - `Page<Goal> findByUserIdAndStatus(Long userId, GoalStatus status, Pageable pageable)`
  - `Page<Goal> findByUserId(Long userId, Pageable pageable)`
  - `List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, GoalStatus status)`
  - Custom JPQL — çoklu filtre desteği:
    ```java
    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId " +
           "AND (:status IS NULL OR g.status = :status) " +
           "AND (:category IS NULL OR g.category = :category) " +
           "AND (:goalType IS NULL OR g.goalType = :goalType) " +
           "AND (:query IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Goal> findByFilters(@Param("userId") Long userId,
                             @Param("status") GoalStatus status,
                             @Param("category") GoalCategory category,
                             @Param("goalType") GoalType goalType,
                             @Param("query") String query,
                             Pageable pageable);
    ```
  - `long countByUserIdAndStatus(Long userId, GoalStatus status)` — Dashboard için
  - `List<Goal> findByUserIdAndStatus(Long userId, GoalStatus status)` — Dashboard toplu çekme

### Backend — Service
- [ ] `GoalService.java`:
  - `getGoals(Long userId, GoalStatus, GoalCategory, GoalType, String query, Pageable)` — çoklu filtre + text search
  - `getGoalById(Long goalId, Long userId)` — ownership kontrolü + 404/403
  - `createGoal(Long userId, CreateGoalRequest)`:
    - `startDate` ≤ `endDate` kontrolü
    - `targetValue` > 0 kontrolü
    - Kullanıcıya ait aktif hedef sayısı kontrolü (opsiyonel üst limit)
      > **[DECISION REQUIRED: Aktif hedef üst limiti olacak mı? Kaç? 50 önerilir.]**
  - `updateGoal(Long goalId, Long userId, UpdateGoalRequest)` — ownership + partial update
    - `@Version` sayesinde concurrent update → `OptimisticLockingFailureException` → 409 Conflict
  - `deleteGoal(Long goalId, Long userId)` — ownership kontrolü, CASCADE ile entry'ler de silinir
  - `updateStatus(Long goalId, Long userId, GoalStatus newStatus)`:
    - Geçersiz durum geçişlerini engelle (aşağıdaki state machine'e göre)
    - COMPLETED → hedefi tamamla
    - Bildirim event'i publish et (Faz 6 için hazırlık): `GoalStatusChangedEvent`
  - `duplicateGoal(Long goalId, Long userId)` → mevcut hedefi kopyala (yeni tarihlerle)
    > **[DECISION REQUIRED: Duplicate feature bu fazda mı yapılacak yoksa backlog'a mı bırakılacak?]**
  - `getGoalStats(Long goalId, Long userId)` — `GoalCalculator` kullanarak (Faz 3'te tamamlanacak)
  - `getChartData(Long goalId, Long userId)` — günlük planlanan vs gerçekleşen (Faz 3'te tamamlanacak)
  - Tüm public metotlarda: `@Transactional` (readOnly = true for queries, default for CUD)

### Backend — Controller
- [ ] `GoalController.java`:
  - `GET /api/goals` — `@RequestParam` ile filtre + `Pageable`
  - `POST /api/goals` — `@Valid @RequestBody CreateGoalRequest`
  - `GET /api/goals/{id}` — `@PathVariable`
  - `PUT /api/goals/{id}` — `@Valid @RequestBody UpdateGoalRequest`
  - `DELETE /api/goals/{id}`
  - `PATCH /api/goals/{id}/status` — `@Valid @RequestBody StatusUpdateRequest`
  - `GET /api/goals/{id}/stats` — (Faz 3'te tamamlanacak)
  - `GET /api/goals/{id}/chart-data` — (Faz 3'te tamamlanacak)
  - Her endpoint'te: `@AuthenticationPrincipal` ile `currentUserId` al
  - Swagger annotations: `@Operation`, `@ApiResponse`, `@Parameter`

### Backend — DTO'lar
- [ ] `CreateGoalRequest.java`:
  ```java
  public record CreateGoalRequest(
      @NotBlank @Size(max = 200) String title,
      @Size(max = 1000) String description,
      @NotBlank @Size(max = 50) String unit,
      @NotNull GoalType goalType,
      GoalFrequency frequency,                    // default: DAILY
      @NotNull @Positive BigDecimal targetValue,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      GoalCategory category,
      @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color
  ) {}
  // Class-level: @EndDateAfterStartDate
  ```
- [ ] `UpdateGoalRequest.java`:
  ```java
  public record UpdateGoalRequest(
      @Size(max = 200) String title,
      @Size(max = 1000) String description,
      @Size(max = 50) String unit,
      GoalType goalType,
      GoalFrequency frequency,
      @Positive BigDecimal targetValue,
      LocalDate startDate,
      LocalDate endDate,
      GoalCategory category,
      @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color
  ) {}
  // Tüm alanlar opsiyonel — partial update
  // Class-level: @EndDateAfterStartDate (her iki tarih de verilmişse)
  ```
- [ ] `StatusUpdateRequest.java`:
  ```java
  public record StatusUpdateRequest(
      @NotNull GoalStatus newStatus
  ) {}
  ```
- [ ] `GoalResponse.java`:
  ```java
  public record GoalResponse(
      Long id,
      String title,
      String description,
      String unit,
      GoalType goalType,
      GoalFrequency frequency,
      BigDecimal targetValue,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
      GoalCategory category,
      String color,
      GoalStatus status,
      BigDecimal completionPct,          // hesaplanan
      BigDecimal currentProgress,        // hesaplanan
      int daysLeft,                      // hesaplanan
      int currentStreak,                 // Faz 5'te doldurulacak, şimdi 0
      Long version,                      // optimistic locking
      @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Instant createdAt,
      @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Instant updatedAt
  ) {}
  ```
- [ ] `GoalSummaryResponse.java`:
  ```java
  public record GoalSummaryResponse(
      Long id,
      String title,
      String unit,
      GoalStatus status,
      BigDecimal completionPct,
      int currentStreak,
      GoalCategory category,
      String color,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
      int daysLeft
  ) {}
  ```

### Backend — Exception'lar
- [ ] `GoalNotFoundException.java` → 404
- [ ] `GoalAccessDeniedException.java` → 403
- [ ] `InvalidStatusTransitionException.java` → 400
- [ ] `GoalLimitExceededException.java` → 400 (opsiyonel — aktif hedef limit)

### Backend — Validator
- [ ] `EndDateAfterStartDate.java` — custom constraint annotation
- [ ] `EndDateAfterStartDateValidator.java` — `@EndDateAfterStartDate` custom constraint validator

### Backend — Mapper
- [ ] `GoalMapper.java` — `Goal` → `GoalResponse`, `Goal` → `GoalSummaryResponse`, `CreateGoalRequest` → `Goal`

### Frontend (Thymeleaf Templates)
- [ ] `templates/goals/list.html` — Hedef listesi:
  - Bootstrap card grid layout
  - Filtre formu: Status tab'ları (`th:each`), Kategori dropdown, Hedef Tipi dropdown
  - Text search input (form submit ile)
  - Sıralama: `th:href` ile URL parametreli link'ler
  - `th:replace` ile `fragments/pagination.html` entegrasyonu
  - Boş durum: `th:if="${goals.empty}"` → Bootstrap alert
  - "Yeni Hedef" butonu
- [ ] `templates/goals/create.html` — Yeni hedef formu:
  - `th:action`, `th:object="${goalForm}"` binding
  - Tarih picker (`<input type="date">`)
  - Renk seçici (Bootstrap color input)
  - Kategori ve hedef tipi `<select>` alanları
  - `th:errors` ile alan bazlı validasyon hataları
- [ ] `templates/goals/edit.html` — Hedef düzenleme formu:
  - `th:field` ile mevcut değerleri göster
  - Aynı validasyonlar
- [ ] `templates/goals/detail.html` — Hedef detay iskelet sayfası:
  - Başlık, açıklama, tarih aralığı, birim
  - İstatistik kartları (Phase 3'te doldurulacak)
  - "Entry Ekle" butonu (Phase 3)
  - Grafik alanı (Phase 4)
  - Durum değiştirme butonları (form submit)
- [ ] `GoalController.java` (MVC Controller):
  - `GET /goals` → goals/list.html + Model(goals Page, filters)
  - `GET /goals/new` → goals/create.html + Model(goalForm)
  - `POST /goals` → oluştur, başarıda redirect `/goals/{id}`
  - `GET /goals/{id}` → goals/detail.html
  - `GET /goals/{id}/edit` → goals/edit.html
  - `POST /goals/{id}/edit` → güncelle, redirect
  - `POST /goals/{id}/delete` → sil (form submit), redirect `/goals`
  - `POST /goals/{id}/status` → durum değiştir, redirect

---

## 🗃️ SQL DDL — Flyway Migration

### V2__create_goals.sql
```sql
CREATE TABLE goals (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200)    NOT NULL,
    description     VARCHAR(1000),
    unit            VARCHAR(50)     NOT NULL,
    goal_type       VARCHAR(20)     NOT NULL
                                    CHECK (goal_type IN ('DAILY', 'CUMULATIVE', 'RATE')),
    frequency       VARCHAR(20)     NOT NULL DEFAULT 'DAILY'
                                    CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    target_value    NUMERIC(12,2)   NOT NULL CHECK (target_value > 0),
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    category        VARCHAR(30)     CHECK (category IN ('EDUCATION', 'HEALTH', 'FITNESS', 'FINANCE', 'PERSONAL')),
    color           VARCHAR(7)      CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                                    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')),
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_goals_dates CHECK (end_date > start_date)
);

-- Performance indexes
CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_user_status ON goals (user_id, status);
CREATE INDEX idx_goals_user_category ON goals (user_id, category);
CREATE INDEX idx_goals_user_type ON goals (user_id, goal_type);
CREATE INDEX idx_goals_end_date ON goals (end_date);
CREATE INDEX idx_goals_title_search ON goals USING gin (to_tsvector('simple', title));

COMMENT ON TABLE goals IS 'Kullanıcı hedef tanımları';
COMMENT ON COLUMN goals.target_value IS 'Hedef değeri — NUMERIC(12,2) hassasiyet, asla float kullanma';
COMMENT ON COLUMN goals.version IS 'Optimistic locking — @Version JPA annotation';
COMMENT ON COLUMN goals.color IS 'Hex renk kodu: #RRGGBB formatı';
```

---

## 📐 Thymeleaf Model Attributes

Thymeleaf template'lerinde kullanılan Controller model objeleri:

| Model Attribute | Java Tipi | Kullanım |
|-----------------|-----------|----------|
| `goals` | `Page<GoalSummaryResponse>` | `th:each="goal : ${goals.content}"` |
| `goalForm` | `CreateGoalRequest` | `th:object="${goalForm}"` |
| `goal` | `GoalResponse` | `th:text="${goal.title}"` |
| `statuses` | `GoalStatus[]` | `th:each="s : ${statuses}"` |
| `categories` | `GoalCategory[]` | `th:each="c : ${categories}"` |
| `goalTypes` | `GoalType[]` | `th:each="t : ${goalTypes}"` |

---

## 🔌 API Endpoint'leri

### GET `/api/goals`
```
?page=0&size=10&sort=createdAt,desc
&status=ACTIVE&category=EDUCATION&goalType=DAILY&query=kitap
Authorization: Bearer <token>
→ 200 ApiResponse<Page<GoalSummaryResponse>>
```

**Desteklenen Sort Parametreleri:**
| Sort | Açıklama |
|------|----------|
| `createdAt,desc` | En yeni önce (varsayılan) |
| `createdAt,asc` | En eski önce |
| `endDate,asc` | Bitiş tarihi yakın olan önce |
| `endDate,desc` | Bitiş tarihi uzak olan önce |
| `title,asc` | Alfabetik A-Z |
| `title,desc` | Alfabetik Z-A |

### POST `/api/goals`
```
Body: CreateGoalRequest
→ 201 ApiResponse<GoalResponse>
```

### GET `/api/goals/{id}`
```
→ 200 ApiResponse<GoalResponse>
→ 404 (bulunamadı) | 403 (başkasının hedefi)
```

### PUT `/api/goals/{id}`
```
Body: UpdateGoalRequest
→ 200 ApiResponse<GoalResponse>
→ 409 Conflict (optimistic locking — başka biri daha önce güncellemiş)
```

### DELETE `/api/goals/{id}`
```
→ 204 No Content
```

### PATCH `/api/goals/{id}/status`
```
Body: { "newStatus": "PAUSED" }
→ 200 ApiResponse<GoalResponse>
→ 400 (geçersiz durum geçişi)
```

---

## 📦 JSON Request/Response Examples

### Create Goal — Success (201)
```json
// POST /api/goals
// Request:
{
  "title": "Aylık Kitap Okuma",
  "description": "Her ay 300 sayfa kitap okumak",
  "unit": "sayfa",
  "goalType": "CUMULATIVE",
  "frequency": "DAILY",
  "targetValue": 300,
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "category": "EDUCATION",
  "color": "#3B82F6"
}

// Response (201):
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Aylık Kitap Okuma",
    "description": "Her ay 300 sayfa kitap okumak",
    "unit": "sayfa",
    "goalType": "CUMULATIVE",
    "frequency": "DAILY",
    "targetValue": 300.00,
    "startDate": "2026-03-01",
    "endDate": "2026-03-31",
    "category": "EDUCATION",
    "color": "#3B82F6",
    "status": "ACTIVE",
    "completionPct": 0.00,
    "currentProgress": 0.00,
    "daysLeft": 31,
    "currentStreak": 0,
    "version": 0,
    "createdAt": "2026-02-28T14:00:00Z",
    "updatedAt": "2026-02-28T14:00:00Z"
  },
  "message": "Hedef başarıyla oluşturuldu.",
  "errorCode": null,
  "timestamp": "2026-02-28T14:00:00Z"
}
```

### Create Goal — Validation Error (400)
```json
{
  "success": false,
  "data": null,
  "message": "Doğrulama hatası.",
  "errorCode": "VALIDATION_ERROR",
  "fieldErrors": [
    { "field": "targetValue", "message": "Hedef değeri sıfırdan büyük olmalıdır.", "rejectedValue": 0 },
    { "field": "endDate", "message": "Bitiş tarihi başlangıç tarihinden sonra olmalıdır.", "rejectedValue": "2026-02-01" }
  ],
  "timestamp": "2026-02-28T14:00:00Z"
}
```

### List Goals — Paginated (200)
```json
// GET /api/goals?status=ACTIVE&page=0&size=10&sort=createdAt,desc
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Aylık Kitap Okuma",
        "unit": "sayfa",
        "status": "ACTIVE",
        "completionPct": 45.50,
        "currentStreak": 0,
        "category": "EDUCATION",
        "color": "#3B82F6",
        "endDate": "2026-03-31",
        "daysLeft": 31
      },
      {
        "id": 2,
        "title": "Haftalık Koşu",
        "unit": "km",
        "status": "ACTIVE",
        "completionPct": 72.00,
        "currentStreak": 0,
        "category": "FITNESS",
        "color": "#10B981",
        "endDate": "2026-04-30",
        "daysLeft": 61
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-02-28T14:00:00Z"
}
```

### Update Status — Success (200)
```json
// PATCH /api/goals/1/status
// Request:
{ "newStatus": "PAUSED" }

// Response (200):
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Aylık Kitap Okuma",
    "status": "PAUSED",
    "version": 1,
    "...": "diğer alanlar"
  },
  "message": "Hedef durumu güncellendi.",
  "errorCode": null,
  "timestamp": "2026-02-28T14:05:00Z"
}
```

### Update Status — Invalid Transition (400)
```json
// PATCH /api/goals/1/status
// Request: { "newStatus": "ACTIVE" }  (COMPLETED → ACTIVE geçersiz)
{
  "success": false,
  "data": null,
  "message": "Geçersiz durum geçişi: COMPLETED → ACTIVE",
  "errorCode": "INVALID_STATUS_TRANSITION",
  "timestamp": "2026-02-28T14:05:00Z"
}
```

### Access Denied (403)
```json
{
  "success": false,
  "data": null,
  "message": "Bu hedefe erişim yetkiniz yok.",
  "errorCode": "GOAL_ACCESS_DENIED",
  "timestamp": "2026-02-28T14:05:00Z"
}
```

### Not Found (404)
```json
{
  "success": false,
  "data": null,
  "message": "Hedef bulunamadı: id=999",
  "errorCode": "GOAL_NOT_FOUND",
  "timestamp": "2026-02-28T14:05:00Z"
}
```

### Optimistic Lock Conflict (409)
```json
{
  "success": false,
  "data": null,
  "message": "Bu hedef başka bir işlem tarafından güncellenmiş. Lütfen sayfayı yenileyip tekrar deneyin.",
  "errorCode": "CONCURRENT_UPDATE_CONFLICT",
  "timestamp": "2026-02-28T14:05:00Z"
}
```

---

## 📊 Pagination Response Format

Spring `Page<T>` → Frontend `PaginatedResponse<T>` dönüşümü:

```java
// Backend — Spring Page wrapper
@GetMapping
public ResponseEntity<ApiResponse<Map<String, Object>>> getGoals(...) {
    Page<GoalSummaryResponse> page = goalService.getGoals(...);
    Map<String, Object> response = Map.of(
        "content", page.getContent(),
        "page", page.getNumber(),
        "size", page.getSize(),
        "totalElements", page.getTotalElements(),
        "totalPages", page.getTotalPages(),
        "first", page.isFirst(),
        "last", page.isLast()
    );
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── model/
│   ├── Goal.java
│   └── enums/
│       ├── GoalType.java
│       ├── GoalFrequency.java
│       ├── GoalCategory.java
│       └── GoalStatus.java
├── repository/
│   └── GoalRepository.java
├── service/
│   └── GoalService.java
├── controller/
│   └── GoalController.java
├── dto/
│   ├── request/
│   │   ├── CreateGoalRequest.java
│   │   ├── UpdateGoalRequest.java
│   │   └── StatusUpdateRequest.java
│   └── response/
│       ├── GoalResponse.java
│       └── GoalSummaryResponse.java
├── mapper/
│   └── GoalMapper.java
├── exception/
│   ├── GoalNotFoundException.java
│   ├── GoalAccessDeniedException.java
│   ├── InvalidStatusTransitionException.java
│   └── GoalLimitExceededException.java
└── validation/
    ├── EndDateAfterStartDate.java       (annotation)
    └── EndDateAfterStartDateValidator.java
```

### Flyway Migration
```
src/main/resources/db/migration/
└── V2__create_goals.sql
```

### Thymeleaf Templates
```
src/main/resources/templates/
└── goals/
    ├── list.html
    ├── create.html
    ├── edit.html
    └── detail.html    (iskelet — Phase 3'te doldurulacak)
```

---

## 💡 İş Kuralları

### Hedef Tipleri
| Tip | Açıklama | Örnek | İlerleme Hesabı |
|-----|----------|-------|-----------------|
| `DAILY` | Her gün belirli miktar yapılmalı | Günde 1 saat yazılım | Günlük entry >= targetValue/totalDays |
| `CUMULATIVE` | Dönem sonuna kadar toplam | Bu ay 300 sayfa | SUM(entries) vs targetValue |
| `RATE` | Belirli periyotta belirli sayıda | Haftada 3 antrenman | Haftalık entry sayısı vs targetValue |

### Durum Geçiş Makinesi

```
            ┌──────────────────────────┐
            ▼                          │
ACTIVE ──→ PAUSED ─────────────────→ ACTIVE (resume)
  │                                    
  ├──→ COMPLETED
  │
  └──→ ARCHIVED
       ▲
PAUSED ─┘
COMPLETED ─→ ARCHIVED
```

### Durum Geçiş Matrisi (Tüm Kombinasyonlar)

| From \ To | ACTIVE | PAUSED | COMPLETED | ARCHIVED |
|-----------|--------|--------|-----------|----------|
| **ACTIVE** | ❌ | ✅ | ✅ | ✅ |
| **PAUSED** | ✅ (resume) | ❌ | ❌ | ✅ |
| **COMPLETED** | ❌ | ❌ | ❌ | ✅ |
| **ARCHIVED** | ❌ | ❌ | ❌ | ❌ |

```java
// GoalStatus enum içinde veya ayrı utility'de:
public static boolean isValidTransition(GoalStatus from, GoalStatus to) {
    return switch (from) {
        case ACTIVE -> to == PAUSED || to == COMPLETED || to == ARCHIVED;
        case PAUSED -> to == ACTIVE || to == ARCHIVED;
        case COMPLETED -> to == ARCHIVED;
        case ARCHIVED -> false; // terminal state
    };
}
```

### Ownership Kontrolü (Her service metodunda zorunlu)
```java
private Goal getGoalWithOwnershipCheck(Long goalId, Long userId) {
    Goal goal = goalRepository.findById(goalId)
        .orElseThrow(() -> new GoalNotFoundException(goalId));
    if (!goal.getUser().getId().equals(userId)) {
        throw new GoalAccessDeniedException(goalId);
    }
    return goal;
}
```

### Validasyon Kuralları
| Alan | Kural | Hata Mesajı |
|------|-------|-------------|
| `title` | zorunlu, max 200 karakter | "Başlık zorunludur ve en fazla 200 karakter olabilir." |
| `description` | opsiyonel, max 1000 karakter | "Açıklama en fazla 1000 karakter olabilir." |
| `unit` | zorunlu, max 50 karakter | "Birim zorunludur." |
| `targetValue` | BigDecimal, > 0 | "Hedef değeri sıfırdan büyük olmalıdır." |
| `startDate` | bugün veya gelecek olabilir (esnek) | — |
| `endDate` | startDate'ten kesinlikle sonra olmalı | "Bitiş tarihi başlangıç tarihinden sonra olmalıdır." |
| `color` | opsiyonel, `#RRGGBB` formatı | "Geçersiz renk formatı. #RRGGBB bekleniyor." |
| `category` | opsiyonel enum değeri | "Geçersiz kategori." |
| `frequency` | default DAILY | — |

---

## 🛡️ Security Considerations

### Ownership Enforcement
- Tüm goal endpoint'leri userId bazlı filtreleme yapar
- Controller'da **ASLA** ownership kontrolü yapılmaz → sadece Service katmanında
- `findById` + userId karşılaştırması tek atomik metot içinde yapılır
- IDOR (Insecure Direct Object Reference) koruması: URL'deki goal ID diğer kullanıcının hedefine erişim sağlamamalı

### Input Sanitization
- `title` ve `description` alanları HTML sanitize edilmeli (XSS koruması)
- `color` alanı strict regex ile sınırlandırılmış (`^#[0-9A-Fa-f]{6}$`)
- `targetValue` üst limit: NUMERIC(12,2) → max 9999999999.99

### API Rate Limiting
- Goal CRUD endpoint'leri kullanıcı bazlı rate limit: 100 req/dakika
  > **[DECISION REQUIRED: Goal endpoint'lerine rate limit uygulanacak mı?]**

---

## ⚡ Performance Considerations

### Database Indexes
- `idx_goals_user_status` → en sık kullanılan sorgu: kullanıcının aktif hedefleri
- `idx_goals_user_category` → kategori bazlı filtreleme
- `idx_goals_end_date` → bitiş tarihi sıralama
- Full-text search: `gin` index `title` alanında (PostgreSQL)

### N+1 Query Prevention
```java
// ❌ Kötü — her goal için ayrı entry count sorgusu
goals.forEach(g -> g.setCompletionPct(calculator.calculate(g));

// ✅ İyi — tek sorguda tüm hesaplamaları çek
@Query("SELECT g.id, SUM(e.actualValue) FROM Goal g LEFT JOIN g.entries e " +
       "WHERE g.user.id = :userId AND g.status = :status GROUP BY g.id")
List<Object[]> findGoalProgressSummaries(Long userId, GoalStatus status);
```

### Pagination
- Default: `page=0, size=10`
- Max size: 50 (aşılırsa 50'ye düşür)
- Sort: `createdAt,desc` (varsayılan)

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ User ilişkisini EAGER fetch yapma → N+1 sorgu
//    @ManyToOne(fetch = FetchType.LAZY)

// ❌ Enum'u ORDINAL persist etme → veri bozulur
//    @Enumerated(EnumType.STRING) tüm enum alanlarında

// ❌ double/float kullanma hassas değerler için
//    → BigDecimal (target_value, actual_value her yerde)

// ❌ Controller'da ownership kontrolü yapmak
//    → Sadece Service katmanında yap

// ❌ findById sonucunu .get() ile almak → NoSuchElementException
//    → .orElseThrow() kullan

// ❌ COMPLETED veya ARCHIVED hedefin status'unu geriye almak
//    → InvalidStatusTransitionException fırlat

// ❌ Silinen hedefin entry'lerini orphan bırakmak
//    → Goal entity'de @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)

// ❌ @Transactional annotation eksik → partial update riski
//    → Service metotlarında @Transactional zorunlu

// ❌ Optimistic locking yoksa concurrent update → veri kaybı
//    → @Version alanı Goal entity'de zorunlu
```

```typescript
// ❌ Mutation sonrası liste cache'ini manuel güncelleme
//    → queryClient.invalidateQueries({ queryKey: ['goals'] }) kullan

// ❌ Optimistic update yaparken hata rollback'i unutmak
//    → onError callback'inde queryClient.setQueryData ile eski state'i geri yükle

// ❌ GoalCard'da tüm alanları göstermek → liste sayfası yavaşlar
//    → GoalSummaryResponse (hafif DTO) kullan

// ❌ Search input'ta her tuşta API çağrısı
//    → debounce(300ms) kullan
```

---

## ⚡ Risk Assessment

| Risk | Olasılık | Etki | Azaltma |
|------|----------|------|---------|
| Concurrent status update → race condition | Orta | Orta | `@Version` optimistic locking, 409 Conflict response |
| Hard delete sonrası veri kaybı | Düşük | Yüksek | Soft delete veya CASCADE ile entry'leri de sil, onay dialogu |
| Büyük hedef setlerinde performans düşüşü | Orta | Orta | DB index'ler, pagination zorunlu, max size=50 limiti |
| Enum extensibility (yeni kategori ekleme) | Yüksek | Düşük | Flyway migration ile ALTER TABLE, frontend sync |
| IDOR (başka kullanıcının hedefine erişim) | Düşük | Kritik | Ownership check her metotta zorunlu, penetration test |

---

## 🧪 Test Senaryoları

### Backend Unit (`GoalService`)
- [ ] Başarılı hedef oluşturma → goal DB'ye kaydedilir
- [ ] `endDate` < `startDate` → 400 Bad Request
- [ ] `targetValue` ≤ 0 → 400 Bad Request
- [ ] Geçersiz renk formatı (`#XYZ123`) → 400 Bad Request
- [ ] Başka kullanıcının hedefine erişim → 403 Forbidden
- [ ] Var olmayan hedef → 404 Not Found
- [ ] ACTIVE → PAUSED geçişi → 200 OK
- [ ] COMPLETED → ACTIVE geçişi → 400 (geçersiz geçiş)
- [ ] ARCHIVED → ACTIVE geçişi → 400 (geçersiz geçiş)
- [ ] Hedef silinince entry'ler de CASCADE silinir
- [ ] Concurrent update → `OptimisticLockingFailureException` → 409

### Backend Entegrasyon (`GoalController` MockMvc)
- [ ] `GET /api/goals` → sayfalı liste döner
- [ ] `GET /api/goals?status=ACTIVE&category=EDUCATION` → filtreli liste döner
- [ ] `GET /api/goals?query=kitap` → text search çalışır
- [ ] `POST /api/goals` → 201 + `GoalResponse`
- [ ] `PUT /api/goals/{id}` başka kullanıcı tokeniyle → 403
- [ ] `DELETE /api/goals/{id}` → 204
- [ ] `PATCH /api/goals/{id}/status` geçersiz geçiş → 400

### Frontend
- [ ] `GoalsPage`: filtreler değişince sorgu güncellenir
- [ ] `GoalCard`: ProgressBar rengi doğru (arkada/ileride/yolunda)
- [ ] `CreateGoalPage`: Zod validasyonu tarih ve renk formatını kontrol eder
- [ ] `useMutateGoal`: başarılı mutation sonrası cache invalidate edilir
- [ ] Search input debounce çalışıyor (300ms)
- [ ] Boş state gösteriliyor (hedef yokken)
- [ ] Loading skeleton kartları gösteriliyor

---

## ✅ Kabul Kriterleri

### CRUD & Güvenlik
- [ ] Kullanıcı hedef oluşturabilir → 201, `GoalResponse` dönülür
- [ ] Kullanıcı sadece kendi hedeflerini listeleyebilir (diğer kullanıcı hedefleri görünmez)
- [ ] Başka kullanıcının hedefini görme/düzenleme/silme denemesi → 403 Forbidden
- [ ] Var olmayan hedef → 404, `errorCode = "GOAL_NOT_FOUND"`
- [ ] `targetValue = 0` ile hedef oluşturma → 400 Bad Request
- [ ] `endDate <= startDate` ile hedef oluşturma → 400 Bad Request
- [ ] Geçersiz renk formatı → 400 Bad Request, alan bazlı hata mesajı
- [ ] Concurrent update → 409 Conflict (optimistic locking)

### Durum Yönetimi
- [ ] ACTIVE → PAUSED → ACTIVE (resume) döngüsü çalışır
- [ ] ACTIVE → COMPLETED geçişi çalışır
- [ ] ACTIVE → ARCHIVED geçişi çalışır
- [ ] COMPLETED → ACTIVE geçişi → 400 `INVALID_STATUS_TRANSITION`
- [ ] ARCHIVED → herhangi bir durum → 400 `INVALID_STATUS_TRANSITION`

### Listeleme & Filtreleme
- [ ] `GET /api/goals` sayfalama çalışır (page, size, sort parametreleri)
- [ ] `?status=ACTIVE` filtresi sadece aktif hedefleri döndürür
- [ ] `?category=EDUCATION` filtresi kategori bazlı filtreler
- [ ] `?goalType=DAILY` filtresi çalışır
- [ ] `?query=kitap` text search çalışır
- [ ] Filtreler birleştirilebilir (status + category + goalType + query aynı anda)
- [ ] Varsayılan sıralama: `createdAt,desc`

### Frontend
- [ ] Hedef listesi `GoalCard` bileşenleriyle gösteriliyor
- [ ] GoalCard'da: başlık, birim, ProgressBar, durum badge, kategori, streak, kalan gün görünüyor
- [ ] "Yeni Hedef" butonu `CreateGoalPage`'e yönlendiriyor
- [ ] Oluşturma formu tüm Zod validasyonlarını gösteriyor
- [ ] Başarılı oluşturma sonrası liste güncelleniyor (cache invalidate)
- [ ] GoalCard üç nokta menüsünden durum değişikliği yapılabiliyor
- [ ] Silme işlemi onay dialogu gösteriyor
- [ ] Boş durum (hedef yokken) `EmptyState` bileşeni gösteriliyor
- [ ] Loading state'te skeleton kartları gösteriliyor
- [ ] Tüm metinler Türkçe
