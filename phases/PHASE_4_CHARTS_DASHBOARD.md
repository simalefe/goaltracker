# 📈 Faz 4 — Grafikler & Dashboard

| Field | Value |
|-------|-------|
| Version | 2.0.0 |
| Status | Enhanced — Architect Review Complete |
| Document Owner | Senior Architect |
| Last Updated | 2026-02-28 |
| Estimated Duration | 1 hafta (5 iş günü) |
| Dependency | Faz 3 (GoalEntry, GoalCalculator, `/api/goals/{id}/chart-data` ve `/api/goals/{id}/stats` endpoint'leri hazır olmalı) |
| Hedef | Recharts ile 4 görselleştirme bileşeni, Dashboard API ve sayfası, `GoalDetailPage` grafik entegrasyonu, responsive tasarım, caching stratejisi, erişilebilirlik. |

---

## 📝 Changes Made

| # | Change | Reason |
|---|--------|--------|
| 1 | `## SQL — Dashboard Optimization` bölümü eklendi (aggregate sorgu optimizasyonu) | N+1 sorgu önleme |
| 2 | `## TypeScript Interfaces` bölümü eklendi (12+ interface, chart props dahil) | Frontend tip güvenliği |
| 3 | `## JSON Response Examples` bölümü eklendi (dashboard tam response, boş dashboard) | API sözleşme örnekleri |
| 4 | `## Caching Strategy` bölümü eklendi (Caffeine, TTL, invalidation) | Dashboard performans optimizasyonu |
| 5 | `## Accessibility (a11y)` bölümü eklendi (ARIA labels, screen reader, keyboard nav) | WCAG 2.1 AA uyumluluk |
| 6 | `## Loading & Error States` bölümü eklendi (her bileşen için) | UX kalitesi |
| 7 | `## Dark Mode Support` bölümü notu eklendi | Gelecek uyumluluk |
| 8 | `## Performance Budget` bölümü eklendi (API response time, chart render budget) | Performans hedefleri |
| 9 | `## Security Considerations` bölümü eklendi | Dashboard veri izolasyonu |
| 10 | `## Risk Assessment` bölümü eklendi | N+1 query, bundle size, mobile rendering |
| 11 | Dashboard auto-refresh interval consideration eklendi | Real-time UX |
| 12 | Chart bileşenleri Props interface'leri eklendi | Tip güvenliği |
| 13 | Responsive breakpoint tablosu eklendi | Mobil uyumluluk standardı |
| 14 | DashboardService `@Transactional(readOnly = true)` notu eklendi | Query performansı |
| 15 | `@Cacheable` annotation detayı eklendi | Cache implementasyonu |

---

## 📋 Görev Listesi

### Backend — Dashboard API
- [ ] `DashboardService.java`:
  - `getDashboard(Long userId)` → tek akışta tüm özet veri:
    - Aktif hedef sayısı (`countByUserIdAndStatus(ACTIVE)`)
    - Bugün entry girilmiş hedef sayısı
    - Tüm aktif hedeflerin toplam streak (Faz 5'te gerçek değer, şimdilik 0)
    - `goalsOnTrack`: gap ≥ 0 olan aktif hedef sayısı
    - `goalsBehind`: gap < 0 olan aktif hedef sayısı
    - Son 5 entry (`recentEntries`)
    - En yüksek completionPct'ye sahip 5 aktif hedef (`topGoals`)
  - N+1 sorgusunu önle: aktif hedefleri tek sorguda çek, `GoalCalculator` ile belleğe hesapla
  - `@Transactional(readOnly = true)` — sadece okuma
  - `@Cacheable(value = "dashboard", key = "#userId")` — cache desteği (aşağıdaki cache stratejisi)
- [ ] `DashboardController.java` — `GET /api/dashboard`
  - `@AuthenticationPrincipal` ile userId al
  - Swagger: `@Operation(summary = "Kullanıcı dashboard özeti")`
- [ ] DTO'lar:
  - `DashboardResponse.java`:
    ```java
    public record DashboardResponse(
        int activeGoalCount,
        int todayEntryCount,
        int totalStreakDays,             // Faz 5'te gerçek değer
        int goalsOnTrack,
        int goalsBehind,
        List<DashboardGoalSummary> topGoals,
        List<RecentEntryResponse> recentEntries
    ) {}
    ```
  - `DashboardGoalSummary.java`:
    ```java
    public record DashboardGoalSummary(
        Long goalId,
        String title,
        String unit,
        BigDecimal completionPct,
        BigDecimal gap,
        String trackingStatus,          // "AHEAD" | "ON_TRACK" | "BEHIND"
        GoalStatus status,
        GoalCategory category,
        String color,
        int currentStreak,
        int daysLeft
    ) {}
    ```
  - `RecentEntryResponse.java`:
    ```java
    public record RecentEntryResponse(
        Long entryId,
        Long goalId,
        String goalTitle,
        String unit,
        BigDecimal actualValue,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Instant createdAt
    ) {}
    ```

### Backend — Dashboard Aggregate Query
```java
// DashboardService — N+1 sorgusunu önle
@Transactional(readOnly = true)
@Cacheable(value = "dashboard", key = "#userId")
public DashboardResponse getDashboard(Long userId) {
    // 1. Aktif hedefleri tek sorguda çek
    List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
    
    // 2. Tüm aktif hedeflerin progress'ini tek aggregate sorguda çek
    List<Object[]> progressData = goalEntryRepository.sumByGoalIds(
        activeGoals.stream().map(Goal::getId).toList()
    );
    // → Map<Long, BigDecimal> goalProgressMap
    
    // 3. Bugünün entry sayısı
    int todayEntryCount = goalEntryRepository.countByUserIdAndEntryDate(userId, LocalDate.now());
    
    // 4. GoalCalculator ile on-track/behind hesapla
    int onTrack = 0, behind = 0;
    List<DashboardGoalSummary> topGoals = new ArrayList<>();
    for (Goal goal : activeGoals) {
        BigDecimal progress = goalProgressMap.getOrDefault(goal.getId(), BigDecimal.ZERO);
        BigDecimal gap = calculator.calculateGap(goal, progress);
        if (gap.compareTo(BigDecimal.ZERO) >= 0) onTrack++;
        else behind++;
        // ... topGoals hesapla
    }
    
    // 5. Son 5 entry
    List<RecentEntryResponse> recentEntries = goalEntryRepository
        .findTop5ByGoalUserIdOrderByEntryDateDesc(userId);
    
    return new DashboardResponse(activeGoals.size(), todayEntryCount, 0, onTrack, behind, topGoals, recentEntries);
}
```

```sql
-- Batch aggregate sorgu — tüm aktif hedeflerin progress'i tek seferde
SELECT ge.goal_id, COALESCE(SUM(ge.actual_value), 0) as total_progress
FROM goal_entries ge
JOIN goals g ON ge.goal_id = g.id
WHERE g.user_id = :userId AND g.status = 'ACTIVE'
GROUP BY ge.goal_id;
```

### Frontend — Grafik Bileşenleri (Chart.js + Thymeleaf)

- [ ] `static/js/charts.js` — Chart.js entegrasyonu:
  - **PlannedVsActualChart:** `Chart.js` Line chart (planlanan + gerçekleşen)
    - Backend'den `/api/goals/{id}/chart-data` endpoint'i çağır (AJAX/Fetch)
    - Mavi dashed çizgi: planlanan kümülatif değer
    - Yeşil çizgi: gerçekleşen kümülatif değer
    - Responsive: `responsive: true, maintainAspectRatio: false`
  - **DailyBarChart:** `Chart.js` Bar chart (günlük entry'ler)
    - Yeşil bar: hedefi geçen günler, kırmızı bar: geçemeyen günler
    - Planlanan günlük değer için `annotation` plugin ile yatay çizgi
  - **CompletionDonut:** `Chart.js` Doughnut chart (tamamlanma %)
    - Merkeze yüzde yazısı: center text plugin
  - **ActivityHeatmap:** Saf CSS grid (12 ay × 7 gün, Bootstrap ve custom CSS)
    - Renk yoğunluğu: 5 Bootstrap class ile (bg-success-100 → bg-success-800)
    - Tooltip: Bootstrap tooltip ile

### Frontend — Dashboard Sayfası (Thymeleaf)

- [ ] `templates/dashboard/index.html` — Dashboard sayfası:
  - Bootstrap card grid: Aktif Hedefler, Bugün Giriş, Yolunda, Geride (4 kart)
  - `th:each="goal : ${topGoals}"` ile hedef kartları
  - Son aktiviteler listesi: `th:each="entry : ${recentEntries}"`
  - Chart.js grafikleri için `<canvas>` elementleri + data attribute'ları
  - Boş durum: `th:if="${activeGoalCount == 0}"` → "İlk hedefini oluştur!" CTA
- [ ] `DashboardController.java` (MVC):
  - `GET /dashboard` → `DashboardService.getDashboard(userId)` → model'e ekle
  - `model.addAttribute("topGoals", response.getTopGoals())`
  - `model.addAttribute("recentEntries", response.getRecentEntries())`
- [ ] `templates/goals/detail.html` güncelleme — grafik tab'ı:
  - Bootstrap Nav Tabs: "İstatistikler" | "Grafik" | "Kayıtlar"
  - Grafik tab'ında `<canvas id="plannedVsActualChart">` elementleri
  - `data-goal-id`, `data-chart-data` attribute'ları ile Chart.js'ye veri aktar
  - Özel `<Legend>`: "Planlanan" / "Gerçekleşen"
  - Null actual noktaları için `connectNulls={false}`
  - X ekseni: tarih etiketleri (çok kalabalıksa her N. gün)
  - Boş veri durumu → EmptyState bileşeni
  - **a11y:** `aria-label="Planlanan ve gerçekleşen ilerleme çizgi grafiği"`
  - **Loading:** Skeleton (gri dikdörtgen, pulse animasyon)
  - **Error:** "Grafik yüklenemedi" + retry butonu

- [ ] **`DailyBarChart.tsx`** — Recharts `<BarChart>`:
  - Her günün `dailyActual` değeri (bar)
  - Planlanan günlük değer `<ReferenceLine y={dailyTarget} stroke="#3B82F6" strokeDasharray="4 4">`
  - Renklendirme: `dailyActual >= dailyTarget` → yeşil (#10B981), düşükse → kırmızı (#EF4444)
  - Özel bar render (Cell ile renk mapping)
  - Tooltip: tarih, gerçekleşen, hedeflenen
  - Boş veri → EmptyState
  - **a11y:** `aria-label="Günlük ilerleme bar grafiği"`

- [ ] **`CompletionDonut.tsx`** — Recharts `<PieChart>`:
  - 2 dilim: `completionPct` (yeşil/mavi) + kalan (gri)
  - Merkeze büyük yüzde yazısı (SVG `<text>` veya absolute div):
    ```tsx
    <text x="50%" y="50%" textAnchor="middle" dominantBaseline="middle"
          className="text-2xl font-bold fill-current">
      {completionPct}%
    </text>
    ```
  - `innerRadius="70%"` / `outerRadius="90%"` — kalın halka
  - Animasyon: `isAnimationActive={true}`
  - Renk: %100+ → altın (#F59E0B), %75+ → yeşil, %50+ → mavi, %25+ → turuncu, < %25 → kırmızı
  - Props: `completionPct: number`, `size?: number`, `label?: string`
  - **a11y:** `aria-label="Tamamlanma oranı: {completionPct}%"`

- [ ] **`ActivityHeatmap.tsx`** — Custom SVG/div bileşeni (Recharts yoktur):
  - 52 hafta × 7 gün grid (GitHub tarzı)
  - Renk yoğunluğu: `dailyActual / dailyTarget` oranına göre 5 seviye
    - 0 (giriş yok): `#1F2937` (dark gri)
    - <50%: `#bbf7d0` (çok açık yeşil)
    - <100%: `#4ade80` (açık yeşil)
    - 100%: `#16a34a` (yeşil)
    - >150%: `#15803d` (koyu yeşil)
  - Tooltip: tarih + değer + yüzde
  - Ay etiketleri üstte (Oca, Şub, Mar...)
  - Gün etiketleri solda (Pzt, Çrş, Cmt)
  - Son 12 ay veya hedef süresi (daha kısaysa)
  - **Mobil:** Son 26 hafta (52 yerine), horizontal scroll
  - **a11y:** Her hücrede `aria-label="{tarih}: {değer} {unit} ({yüzde}%)"`, keyboard navigable
  - **Keyboard:** Arrow keys ile hücreler arası gezinme

### Frontend — Dashboard Sayfası

- [ ] **`DashboardPage.tsx`**:
  - Üst kısım: "Merhaba, {displayName}!" başlığı + bugünün tarihi
  - Özet kartlar (4'lü grid): Aktif Hedefler, Bugün Giriş, Yolunda, Geride
  - `GoalSummaryCard` listesi — aktif hedefler (max 5, "Tümünü gör" linki → `/goals`)
  - `ActiveStreaks` bölümü — (Faz 5'te gerçek veri, şimdi iskelet)
  - `RecentActivity` bölümü — son entry'ler
  - Loading state: Skeleton bileşenleri (her kart ve bölüm için)
  - Error state: "Dashboard yüklenemedi" + retry butonu
  - Boş state: "Henüz hedef yok, ilk hedefini oluştur!" CTA
  - Auto-refresh: `refetchInterval: 5 * 60 * 1000` (5 dakika) — **[DECISION REQUIRED: Auto-refresh aktif mi?]**

- [ ] **`SummaryStatCard.tsx`** — özet kart (ikon, başlık, değer, alt text, renk):
  - Aktif Hedefler: mavi, ikon: 🎯
  - Bugün Giriş: yeşil (bugün entry girilmiş / aktif hedef sayısı), ikon: ✅
  - Yolunda: yeşil (goalsOnTrack), ikon: 📈
  - Geride: kırmızı (goalsBehind), ikon: 📉
  - Hover effect: hafif scale + shadow
  - **a11y:** `role="status"`, `aria-label="{başlık}: {değer}"`

- [ ] **`GoalSummaryCard.tsx`** — dashboard'daki mini hedef kartı:
  - Başlık + kategori + renk bar (sol kenar)
  - CompletionDonut (küçük, 60px) + tamamlanma %
  - Gap göstergesi (+ yeşil / - kırmızı)
  - Kalan gün + streak (iskelet şimdi)
  - Tıklanınca `/goals/{id}`'ye git
  - **a11y:** `<a>` veya `role="link"` + `aria-label="Hedef: {title}, {completionPct}% tamamlandı"`

- [ ] **`ActiveStreaks.tsx`** — Streak sıralaması (Faz 5'te doldurulacak, iskelet):
  - "Streak'ler" başlığı
  - Placeholder listesi veya "Streak verileri yakında" mesajı
  - Faz 5 entegrasyon notu: `// TODO: Phase 5 — StreakService.getActiveStreaks(userId)`

- [ ] **`RecentActivity.tsx`** — Son entry'ler:
  - Tarih + hedef adı + girilen değer + birim
  - Tıklanınca ilgili hedefe git (`/goals/{goalId}`)
  - Boş state: "Henüz ilerleme kaydı yok"
  - Max 5 kayıt

- [ ] **`GoalDetailPage.tsx`** güncelleme — grafik entegrasyonu:
  - Tab'lar: "İstatistikler" | "Grafik" | "Günlük Kayıtlar"
  - İstatistikler tab: `StatsPanel` + `CompletionDonut`
  - Grafik tab: `PlannedVsActualChart` + `DailyBarChart`
  - Kayıtlar tab: `EntryLogTable`
  - Heatmap: alt bölümde veya ayrı tab'da
  - Tab state: URL'de saklansın (`/goals/1?tab=chart`)

### Frontend — Hook'lar & Servisler

- [ ] `useChartData.ts`:
  ```typescript
  const { data, isLoading, error } = useQuery({
    queryKey: ['chartData', goalId],
    queryFn: () => goalService.getChartData(goalId),
    staleTime: 5 * 60 * 1000, // 5 dakika cache
  });
  ```
- [ ] `useDashboard.ts`:
  ```typescript
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardService.getDashboard,
    staleTime: 2 * 60 * 1000, // 2 dakika cache
    refetchInterval: 5 * 60 * 1000, // auto-refresh [DECISION REQUIRED]
  });
  ```
- [ ] `dashboardService.ts` — `getDashboard()` → `DashboardResponse`
- [ ] `chartHelpers.ts`:
  ```typescript
  export const getDailyTarget = (targetValue: number, totalDays: number): number =>
    totalDays > 0 ? targetValue / totalDays : 0;

  export const getBarColor = (dailyActual: number, dailyTarget: number): string =>
    dailyActual >= dailyTarget ? '#10B981' : '#EF4444';

  export const getDonutColor = (pct: number): string => {
    if (pct >= 100) return '#F59E0B';   // Altın
    if (pct >= 75)  return '#10B981';   // Yeşil
    if (pct >= 50)  return '#3B82F6';   // Mavi
    if (pct >= 25)  return '#F97316';   // Turuncu
    return '#EF4444';                   // Kırmızı
  };

  export const getHeatmapIntensity = (dailyActual: number, dailyTarget: number): 0 | 1 | 2 | 3 | 4 => {
    if (dailyActual <= 0) return 0;
    const ratio = dailyActual / dailyTarget;
    if (ratio < 0.5)  return 1;
    if (ratio < 1.0)  return 2;
    if (ratio <= 1.5) return 3;
    return 4;
  };

  export const formatChartDate = (dateStr: string, index: number, total: number): string => {
    if (total <= 14) return dateStr; // kısa süre — her gün
    if (total <= 60) return index % 7 === 0 ? dateStr : ''; // haftalık
    return index % 30 === 0 ? dateStr : ''; // aylık
  };
  ```

### Responsive Tasarım

| Breakpoint | Width | Layout |
|------------|-------|--------|
| Mobile | < 640px | `grid-cols-1`, charts full width, heatmap 26 week |
| Tablet | 640px - 1024px | `grid-cols-2` stat cards, charts full width |
| Desktop | 1024px - 1280px | `grid-cols-4` stat cards, `grid-cols-2` goal cards |
| Wide | > 1280px | `grid-cols-4` stat cards, `grid-cols-3` goal cards |

- [ ] Dashboard: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-4` (özet kartlar)
- [ ] GoalSummaryCard listesi: `grid-cols-1 md:grid-cols-2 xl:grid-cols-3`
- [ ] Grafikler: `ResponsiveContainer` ile container'a göre boyutlanır
- [ ] ActivityHeatmap: mobilde son 26 hafta (52 yerine), `overflow-x-auto`
- [ ] Minimum grafik yüksekliği: 200px (mobil), 300px (desktop)

---

## 📐 Thymeleaf Model Attributes (Dashboard)

| Model Attribute | Java Tipi | Kullanım |
|-----------------|-----------|----------|
| `activeGoalCount` | `int` | `th:text="${activeGoalCount}"` |
| `todayEntryCount` | `int` | `th:text="${todayEntryCount}"` |
| `goalsOnTrack` | `int` | `th:text="${goalsOnTrack}"` |
| `goalsBehind` | `int` | `th:text="${goalsBehind}"` |
| `topGoals` | `List<DashboardGoalSummary>` | `th:each="goal : ${topGoals}"` |
| `recentEntries` | `List<RecentEntryResponse>` | `th:each="entry : ${recentEntries}"` |

---

## 🔌 API Endpoint'leri

### GET `/api/dashboard`
```
Authorization: Bearer <token>
→ 200 ApiResponse<DashboardResponse>
```

### GET `/api/goals/{id}/chart-data`  ← Faz 3'te oluşturuldu
### GET `/api/goals/{id}/stats`  ← Faz 3'te oluşturuldu

---

## 📦 JSON Response Examples

### Dashboard — Full Response (200)
```json
// GET /api/dashboard
{
  "success": true,
  "data": {
    "activeGoalCount": 5,
    "todayEntryCount": 3,
    "totalStreakDays": 0,
    "goalsOnTrack": 3,
    "goalsBehind": 2,
    "topGoals": [
      {
        "goalId": 1,
        "title": "Kitap Okuma",
        "unit": "sayfa",
        "completionPct": 85.50,
        "gap": 15.30,
        "trackingStatus": "AHEAD",
        "status": "ACTIVE",
        "category": "EDUCATION",
        "color": "#3B82F6",
        "currentStreak": 0,
        "daysLeft": 10
      },
      {
        "goalId": 2,
        "title": "Koşu",
        "unit": "km",
        "completionPct": 65.00,
        "gap": -5.00,
        "trackingStatus": "BEHIND",
        "status": "ACTIVE",
        "category": "FITNESS",
        "color": "#10B981",
        "currentStreak": 0,
        "daysLeft": 45
      }
    ],
    "recentEntries": [
      {
        "entryId": 10,
        "goalId": 1,
        "goalTitle": "Kitap Okuma",
        "unit": "sayfa",
        "actualValue": 25.00,
        "entryDate": "2026-02-28",
        "createdAt": "2026-02-28T20:30:00Z"
      },
      {
        "entryId": 9,
        "goalId": 2,
        "goalTitle": "Koşu",
        "unit": "km",
        "actualValue": 5.50,
        "entryDate": "2026-02-28",
        "createdAt": "2026-02-28T18:00:00Z"
      }
    ]
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-02-28T21:00:00Z"
}
```

### Dashboard — Empty State (200)
```json
{
  "success": true,
  "data": {
    "activeGoalCount": 0,
    "todayEntryCount": 0,
    "totalStreakDays": 0,
    "goalsOnTrack": 0,
    "goalsBehind": 0,
    "topGoals": [],
    "recentEntries": []
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-02-28T21:00:00Z"
}
```

---

## 🗄️ SQL — Dashboard Optimization

```sql
-- Batch aggregate sorgu: aktif hedeflerin progress'i tek seferde
SELECT 
    g.id AS goal_id,
    g.title,
    g.unit,
    g.target_value,
    g.start_date,
    g.end_date,
    g.category,
    g.color,
    g.status,
    COALESCE(SUM(ge.actual_value), 0) AS total_progress
FROM goals g
LEFT JOIN goal_entries ge ON g.id = ge.goal_id
WHERE g.user_id = :userId AND g.status = 'ACTIVE'
GROUP BY g.id;

-- Bugünün entry sayısı
SELECT COUNT(DISTINCT ge.goal_id) 
FROM goal_entries ge
JOIN goals g ON ge.goal_id = g.id
WHERE g.user_id = :userId AND ge.entry_date = CURRENT_DATE;

-- Son 5 entry (kullanıcının tüm hedeflerinden)
SELECT ge.id, ge.goal_id, g.title, g.unit, ge.actual_value, ge.entry_date, ge.created_at
FROM goal_entries ge
JOIN goals g ON ge.goal_id = g.id
WHERE g.user_id = :userId
ORDER BY ge.entry_date DESC, ge.created_at DESC
LIMIT 5;
```

> **[DECISION REQUIRED: Materialized view mi, yoksa service-level hesaplama mı? Önerilen: Service-level hesaplama + Caffeine cache (Redis yok). Aktif hedef sayısı az olduğu sürece yeterli.]**

---

## 🗂️ Caching Strategy

### Spring Cache + Caffeine Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats());
        return cacheManager;
    }
}
```

### Cache Policies
| Cache Name | TTL | Max Size | Invalidation |
|------------|-----|----------|--------------|
| `dashboard` | 5 dakika | 1000 (user bazlı) | Entry CUD, Goal status change |
| `goalStats` | 5 dakika | 5000 (goal bazlı) | Entry CUD for that goal |
| `chartData` | 10 dakika | 2000 (goal bazlı) | Entry CUD for that goal |

### Cache Invalidation
```java
// GoalEntryService — entry CUD sonrası cache temizle
@CacheEvict(value = {"dashboard", "goalStats", "chartData"}, allEntries = false, 
            key = "#userId") // veya conditional
public GoalEntryResponse createEntry(Long goalId, Long userId, CreateEntryRequest req) {
    // ... entry logic
}
```

> **[DECISION REQUIRED: Cache invalidation key stratejisi — userId bazlı mı, goalId bazlı mı? userId bazlı önerilir (dashboard userId ile cache'lenir)]**

---

## ♿ Accessibility (a11y)

### Chart Erişilebilirlik
- Tüm grafik container'larında `role="img"` ve açıklayıcı `aria-label`
- Grafiklerin altında veya tooltip'te screen reader-friendly data table fallback:
  ```tsx
  <div className="sr-only">
    <table>
      <caption>Planlanan ve gerçekleşen ilerleme verileri</caption>
      <thead><tr><th>Tarih</th><th>Planlanan</th><th>Gerçekleşen</th></tr></thead>
      <tbody>{dataPoints.map(dp => <tr>...</tr>)}</tbody>
    </table>
  </div>
  ```
- Renk kontrastı: WCAG 2.1 AA minimum (4.5:1 metin, 3:1 grafik öğeleri)
- Renk-körü dostu: Grafiklerde rengin yanı sıra dashed/solid çizgi stili farkı
- ActivityHeatmap: keyboard navigable (arrow keys), focus outline visible

### Dashboard Erişilebilirlik
- `SummaryStatCard`: `role="status"`, `aria-live="polite"` (değer değişikliklerinde screen reader bildirim)
- `GoalSummaryCard`: navigable link, focus management
- Tab navigation: doğru `tabIndex` sırası
- Skip link: "Ana içeriğe geç" linki sayfa başında

---

## ⏳ Loading & Error States

| Bileşen | Loading | Error | Empty |
|---------|---------|-------|-------|
| `DashboardPage` | Tüm kartlar Skeleton | "Dashboard yüklenemedi" + retry | "İlk hedefini oluştur!" CTA |
| `SummaryStatCard` | Animated pulse (gri bar) | N/A (dashboard error'dan gelir) | Değer: 0 |
| `GoalSummaryCard` | Skeleton kart (3 adet) | N/A | N/A (boş state DashboardPage'de) |
| `PlannedVsActualChart` | Gri dikdörtgen + pulse | "Grafik yüklenemedi" + retry | "Henüz veri yok" |
| `DailyBarChart` | Gri dikdörtgen + pulse | "Grafik yüklenemedi" + retry | "Henüz veri yok" |
| `CompletionDonut` | Gri halka animasyonu | N/A | 0% göster (gri halka) |
| `ActivityHeatmap` | Gri grid + pulse | "Heatmap yüklenemedi" | Tüm hücreler gri |
| `RecentActivity` | Skeleton satırları | N/A | "Henüz kayıt yok" |

---

## 🌙 Dark Mode Support

> **[DECISION REQUIRED: Dark mode bu fazda mı yoksa Faz 9'da mı implement edilecek? Önerilen: Faz 9]**

**Hazırlık notları (şimdi yapılacak):**
- Tüm renk değerleri `constants/colors.ts` içinde merkezi tanımlanmalı (hard-coded hex yok)
- Tailwind `dark:` prefix kullanımına hazır sınıf yapısı
- Recharts theme desteği hazırlığı: renkleri prop olarak alacak şekilde tasarla
- ActivityHeatmap renk paleti dark/light varyantları ayrı ayrı tanımlanmalı

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── service/
│   └── DashboardService.java
├── controller/
│   └── DashboardController.java
├── dto/response/
│   ├── DashboardResponse.java
│   ├── DashboardGoalSummary.java
│   └── RecentEntryResponse.java
└── config/
    └── CacheConfig.java               (güncelleme — dashboard cache)
```

### Thymeleaf Templates
```
src/main/resources/templates/
├── dashboard/
│   └── index.html          (Phase 0 iskelet → tam implementasyon)
└── goals/
    └── detail.html         (Phase 3 → grafik tab'ı eklendi)
```

### Static Files
```
src/main/resources/static/
└── js/
    └── charts.js           (Chart.js grafik entegrasyonu)
```

---

## 💡 İş Kuralları & Renk Sistemi

### Renk Paleti (Standart — Tüm Fazlarda Kullanılır)
```typescript
// constants/colors.ts
export const COLORS = {
  planned:  '#3B82F6',   // blue-500    — planlanan
  actual:   '#10B981',   // emerald-500 — gerçekleşen
  behind:   '#EF4444',   // red-500     — geride
  ahead:    '#10B981',   // emerald-500 — ileride
  onTrack:  '#3B82F6',   // blue-500    — yolunda
  streak:   '#F59E0B',   // amber-500   — streak/ateş
  badge:    '#8B5CF6',   // violet-500  — rozet
  neutral:  '#6B7280',   // gray-500    — nötr
  gold:     '#F59E0B',   // amber-500   — %100+ tamamlama
} as const;

export const HEATMAP_COLORS = {
  empty:    '#1F2937',   // gray-800    — giriş yok
  level1:   '#bbf7d0',   // green-200   — <50%
  level2:   '#4ade80',   // green-400   — <100%
  level3:   '#16a34a',   // green-600   — =100%
  level4:   '#15803d',   // green-700   — >150%
} as const;
```

### Dashboard Hesaplama Mantığı
- `goalsOnTrack`: aktif hedefler içinde `gap >= 0` olanlar
- `goalsBehind`: aktif hedefler içinde `gap < 0` olanlar
- `todayEntryCount`: bugün `entry_date = TODAY` olan **distinct goal sayısı** (kullanıcıya ait)
- `topGoals`: `completionPct DESC` sıralı ilk 5 aktif hedef
- `recentEntries`: `entryDate DESC, createdAt DESC` sıralı ilk 5 entry
- Tüm hesaplamalar `DashboardService`'te `GoalCalculator` kullanılarak yapılır
- Streak değerleri Faz 5'e kadar 0 olarak dönülür

### CompletionDonut Renk Mantığı
```typescript
const getDonutColor = (pct: number): string => {
  if (pct >= 100) return COLORS.gold;        // Altın — tamamlandı
  if (pct >= 75)  return COLORS.actual;      // Yeşil — neredeyse
  if (pct >= 50)  return COLORS.planned;     // Mavi  — yolunda
  if (pct >= 25)  return '#F97316';          // Turuncu — dikkat
  return COLORS.behind;                      // Kırmızı — tehlikede
};
```

### ActivityHeatmap Yoğunluk Seviyeleri
```
Seviye 0 (giriş yok):           HEATMAP_COLORS.empty   (#1F2937)
Seviye 1 (actualValue < 50%):   HEATMAP_COLORS.level1  (#bbf7d0)
Seviye 2 (50% <= actual < 100%):HEATMAP_COLORS.level2  (#4ade80)
Seviye 3 (actual = 100%):       HEATMAP_COLORS.level3  (#16a34a)
Seviye 4 (actual > 150%):       HEATMAP_COLORS.level4  (#15803d)
```

---

## 📊 Performance Budget

| Metrik | Hedef | Ölçüm |
|--------|-------|-------|
| Dashboard API response time | < 500ms (p95) | Backend latency |
| Dashboard page FCP | < 1.5s | Lighthouse |
| Chart render time | < 100ms | React Profiler |
| Recharts bundle size | ~150KB gzip | Bundle analyzer |
| Total dashboard JS | < 300KB gzip | Bundle analyzer |
| ActivityHeatmap render | < 50ms | Performance.now() |

### Optimizasyon Stratejileri
- Recharts'ı lazy load: `React.lazy(() => import('./charts/PlannedVsActualChart'))`
- Dashboard API single request (N+1 yok)
- Spring Cache (Caffeine) ile tekrarlanan istekleri cache'le
- Frontend: TanStack Query staleTime ile gereksiz refetch'leri önle

---

## 🛡️ Security Considerations

### Dashboard Data Isolation
- Dashboard API sadece authenticated kullanıcının verilerini döner
- `@AuthenticationPrincipal` ile userId al → service'e geç → tüm sorgularda userId filtresi
- Başka kullanıcının dashboard'una erişim **imkansız** (URL'de userId yok — token'dan çıkarılır)

### XSS on Chart Labels
- Goal title ve unit alanları chart tooltip'lerinde gösterilir → escape edilmeli
- Recharts default olarak dangerouslySetInnerHTML kullanmaz — güvenli
- Custom tooltip'lerde `{goal.title}` direkt JSX'te kullanılır — React otomatik escape eder

### Rate Limiting
- Dashboard endpoint: 30 req/dakika/kullanıcı (auto-refresh dahil)
  > **[DECISION REQUIRED: Dashboard rate limit gerekli mi? 5dk auto-refresh → 12 req/saat — düşük]**

---

## ⚠️ Dikkat Edilecek Noktalar

```typescript
// ❌ ResponsiveContainer kullanmamak → grafik container'dan taşar
//    → Her zaman <ResponsiveContainer width="100%" height={N}> ile sar

// ❌ Null actual değeri 0 olarak çizmek
//    → connectNulls={false} + null check

// ❌ Çok fazla veri noktası (180+ gün) → X ekseni karışık
//    → Akıllı etiket: chartHelpers.formatChartDate ile her N. günde etiket göster

// ❌ DashboardService'te her hedef için ayrı stats sorgusu → N+1
//    → Batch aggregate sorgu + belleğe hesaplama

// ❌ ActivityHeatmap'i Recharts ile yapmaya çalışmak → uygun değil
//    → Saf CSS grid veya SVG ile oluştur

// ❌ DashboardPage'de Faz 5 verisi olmadan crash → streak = 0 default kullan

// ❌ Renkleri hard-code etmek → dark mode'da sorun
//    → constants/colors.ts merkezi dosyadan import

// ❌ Chart bileşenlerini sync import etmek → bundle size
//    → React.lazy + Suspense ile lazy load

// ❌ Accessibility'yi ihmal etmek
//    → aria-label, role, keyboard navigation zorunlu
```

```java
// ❌ DashboardService'te her hedef için ayrı DB sorgusu
//    → Aggregate sorgularla tek seferde topla

// ❌ Bugünün entry sayısını Java'da filtrelemek → DB'de WHERE entry_date = CURRENT_DATE

// ❌ @Transactional(readOnly = true) eksik → gereksiz write lock

// ❌ Cache invalidation unutmak → stale dashboard verisi
//    → Entry CUD ve Goal status change'de @CacheEvict
```

---

## ⚡ Risk Assessment

| Risk | Olasılık | Etki | Azaltma |
|------|----------|------|---------|
| N+1 query on dashboard | Yüksek | Yüksek | Batch aggregate sorgu, JOIN FETCH, cache |
| Large dataset chart rendering freeze | Orta | Orta | Lazy load, veri noktası limiti (max 365 gün), virtualization |
| Recharts bundle size (~150KB) | Düşük | Orta | Code splitting, lazy import, tree-shaking |
| Mobile rendering performance | Orta | Orta | Heatmap 26 hafta limiti, ResponsiveContainer, CSS will-change |
| Stale cache → yanlış dashboard verisi | Orta | Düşük | @CacheEvict on CUD, TTL 5dk, frontend staleTime |
| a11y compliance failure | Düşük | Orta | Lighthouse audit, screen reader testing, WCAG checklist |

---

## 🧪 Test Senaryoları

### Backend Unit (`DashboardService`)
- [ ] Aktif hedef sayısı doğru hesaplanıyor
- [ ] `goalsOnTrack` / `goalsBehind` ayrımı doğru (gap >= 0 kritere göre)
- [ ] `todayEntryCount`: bugün entry olan + olmayan hedefler doğru sayılıyor
- [ ] Hiç aktif hedef yoksa → 0 değerleri döner, crash yok
- [ ] `topGoals` en fazla 5 hedef döner, completionPct DESC sıralı
- [ ] `recentEntries` en fazla 5 entry döner, entryDate DESC sıralı
- [ ] Cache hit: aynı userId ile 2. istek cache'den döner
- [ ] Cache eviction: entry eklendikten sonra cache temizlenir

### Frontend
- [ ] Dashboard yüklenirken Skeleton bileşenleri gösteriliyor
- [ ] Boş dashboard (0 aktif hedef) → CTA bileşeni gösteriliyor
- [ ] `PlannedVsActualChart`: planned ve actual çizgileri render ediliyor
- [ ] `PlannedVsActualChart`: null actual bölgesinde çizgi kesilir
- [ ] `DailyBarChart`: hedefi geçen günler yeşil, geçemeyen günler kırmızı
- [ ] `DailyBarChart`: ReferenceLine (planlanan) görünüyor
- [ ] `CompletionDonut`: %0, %25, %50, %75, %100 ve %110 için doğru renk
- [ ] `CompletionDonut`: merkeze yüzde yazısı görünüyor
- [ ] `ActivityHeatmap`: boş hücreler gri, dolu hücreler yoğunluğa göre yeşil
- [ ] `ActivityHeatmap`: tooltip tarih + değer + yüzde gösteriyor
- [ ] Responsive: 320px ekranda grafikler taşmıyor
- [ ] Responsive: 1280px+ ekranda 4 kolon stat kartları
- [ ] GoalDetailPage grafik tab'ı çalışıyor
- [ ] a11y: aria-label'lar mevcut, keyboard navigation çalışıyor

---

## ✅ Kabul Kriterleri

### Dashboard API
- [ ] `GET /api/dashboard` → 200, `DashboardResponse` formatında döner
- [ ] `activeGoalCount` doğru: sadece ACTIVE status'taki hedefler sayılır
- [ ] `goalsOnTrack` + `goalsBehind` toplamı ≤ `activeGoalCount`
- [ ] `topGoals` en fazla 5 hedef döner, `completionPct` DESC sıralı
- [ ] `recentEntries` en son 5 entry döner, `entryDate` DESC sıralı
- [ ] Dashboard API'si N+1 sorgu içermiyor (batch aggregate sorgu kullanılmalı)
- [ ] Cache çalışıyor: aynı istek 5 dakika içinde cache'den dönüyor

### Grafik Bileşenleri
- [ ] `PlannedVsActualChart`: mavi dashed planlanan + yeşil gerçekleşen çizgisi render ediliyor
- [ ] `PlannedVsActualChart`: geride kalınan bölgede kırmızı alan (`ReferenceArea`) gösteriliyor
- [ ] `PlannedVsActualChart`: null actual değerleri çizgide boşluk bırakıyor (0 olarak gösterilmiyor)
- [ ] `DailyBarChart`: günlük bar grafik doğru renkleniyor (hedefi geçen gün yeşil, geçemeyen kırmızı)
- [ ] `DailyBarChart`: planlanan günlük değer `ReferenceLine` olarak gösteriliyor
- [ ] `CompletionDonut`: merkeze doğru yüzde değeri yazılıyor
- [ ] `CompletionDonut`: renk `completionPct`'ye göre doğru (kırmızı→turuncu→mavi→yeşil→altın)
- [ ] `ActivityHeatmap`: son 12 ay veya hedef süresi için hücreler oluşturuluyor
- [ ] `ActivityHeatmap`: tooltip'te tarih + değer + yüzde gösteriliyor
- [ ] `ActivityHeatmap`: keyboard navigation çalışıyor
- [ ] Tüm grafikler `ResponsiveContainer` ile sarılmış, container boyutuna uyuyor
- [ ] Tüm grafiklerde `aria-label` mevcut

### GoalDetailPage Entegrasyonu
- [ ] GoalDetailPage'de "Grafik" tab'ı açılınca `PlannedVsActualChart` ve `DailyBarChart` görünüyor
- [ ] GoalDetailPage'de `CompletionDonut` `StatsPanel` içinde gösteriliyor
- [ ] Tab state URL'de saklanıyor (`?tab=chart`)

### Dashboard Sayfası
- [ ] Özet kartlar (4 adet) doğru değerleri gösteriyor
- [ ] GoalSummaryCard listesi tıklanınca GoalDetailPage'e yönlendiriyor
- [ ] Yükleme sırasında Skeleton gösteriliyor
- [ ] Aktif hedef yokken boş state + "Hedef Oluştur" CTA gösteriliyor
- [ ] Mobil, tablet ve desktop layoutları düzgün görünüyor
- [ ] Tüm metinler Türkçe
