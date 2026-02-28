# 🎯 GoalTracker Pro — Faz Genel Bakış

> **Toplam Süre:** ~8-10 hafta  
> **Proje Kararları:**  
> - Backend: **Spring Boot + Maven**  
> - Frontend: **Thymeleaf + Bootstrap 5** (Server-Side Rendering)  
> - IDE: **IntelliJ IDEA**  
> - Veritabanı: **H2 in-memory** (Docker yok, PostgreSQL yok)  
> - Git: İlk adımda oluşturulacak  

---

## 📊 Faz Haritası

```
Faz 0 ─── Proje İskeleti (3-5 gün)
  │        Spring Boot + Maven + Thymeleaf + Bootstrap 5 + H2 + Flyway + Git
  │        ApiResponse<T>, GlobalExceptionHandler, Thymeleaf layout, CI/CD
  ▼
Faz 1 ─── Kimlik Doğrulama (1 hafta)
  │        User entity, JWT access/refresh, BCrypt(12), e-posta doğrulama,
  │        şifre sıfırlama, Rate Limiting, Thymeleaf login/register sayfaları
  ▼
Faz 2 ─── Hedef Yönetimi (1 hafta)
  │        Goal CRUD, durum makinesi, custom validator, ownership,
  │        filtreleme/sayfalama, Thymeleaf goals sayfaları
  ▼
Faz 3 ─── İlerleme Takibi (1 hafta)
  │        GoalEntry CRUD, GoalCalculator, GoalEntryCreatedEvent,
  │        Stats endpoint, ChartData endpoint, Thymeleaf detail sayfası
  ▼
Faz 4 ─── Grafikler & Dashboard (1 hafta)
  │        Chart.js entegrasyonu, DashboardPage (Thymeleaf),
  │        SummaryStatCard, GoalSummaryCard
  ▼
Faz 5 ─── Streak & Rozetler (1 hafta)
  │        StreakService, BadgeService, StreakBadgeEventListener, StreakScheduler,
  │        BadgeEarnedEvent, Profil sayfası (Thymeleaf), Rozet grid'i
  ▼
Faz 6 ─── Bildirim Sistemi (1 hafta)
  │        NotificationService, WebSocket STOMP, Scheduler'lar,
  │        Bildirim zili (Thymeleaf), WebSocketAuthInterceptor, e-posta şablonları
  ▼
Faz 7 ─── Sosyal Özellikler (1 hafta)
  │        Arkadaşlık, GoalShare, Liderboard, ActivityFeed,
  │        Thymeleaf sosyal sayfalar
  ▼
Faz 8 ─── Export (3-5 gün)
  │        Excel (POI/SXSSFWorkbook), PDF (iText 7/Türkçe), CSV (UTF-8 BOM),
  │        ExportButtons, triggerDownload helper, MonthlyReport
  ▼
Faz 9 ─── Polish & Production (1 hafta)
           Testler (%90/%75/%70), OWASP audit, N+1 tarama, bundle analizi,
           Empty/Error/Loading states, NotFoundPage, README, CI final
```

---

## 📁 Faz Dosyaları

| Dosya | Süre | Açıklama |
|-------|------|----------|
| [`PHASE_0_PROJECT_SKELETON.md`](./PHASE_0_PROJECT_SKELETON.md) | 3-5 gün | Maven, Thymeleaf, H2, Flyway, CI/CD, Bootstrap layout |
| [`PHASE_1_AUTHENTICATION.md`](./PHASE_1_AUTHENTICATION.md) | 1 hafta | JWT, BCrypt, e-posta doğrulama, rate limiting, Thymeleaf login/register |
| [`PHASE_2_GOAL_MANAGEMENT.md`](./PHASE_2_GOAL_MANAGEMENT.md) | 1 hafta | Goal CRUD, durum makinesi, filtreleme, Thymeleaf goal sayfaları |
| [`PHASE_3_PROGRESS_TRACKING.md`](./PHASE_3_PROGRESS_TRACKING.md) | 1 hafta | Entry CRUD, GoalCalculator, stats/chart, Thymeleaf detail sayfası |
| [`PHASE_4_CHARTS_DASHBOARD.md`](./PHASE_4_CHARTS_DASHBOARD.md) | 1 hafta | Chart.js grafikleri, Thymeleaf Dashboard |
| [`PHASE_5_STREAKS_BADGES.md`](./PHASE_5_STREAKS_BADGES.md) | 1 hafta | Gamification, Spring Events, scheduler, Thymeleaf profil |
| [`PHASE_6_NOTIFICATIONS.md`](./PHASE_6_NOTIFICATIONS.md) | 1 hafta | WebSocket, e-posta, bildirim scheduler'ları, Thymeleaf bildirim sayfaları |
| [`PHASE_7_SOCIAL.md`](./PHASE_7_SOCIAL.md) | 1 hafta | Arkadaşlık, paylaşım, liderboard, Thymeleaf sosyal sayfa |
| [`PHASE_8_EXPORT.md`](./PHASE_8_EXPORT.md) | 3-5 gün | Excel/PDF/CSV, Türkçe karakter, streaming, Thymeleaf download linkleri |
| [`PHASE_9_POLISH_PRODUCTION.md`](./PHASE_9_POLISH_PRODUCTION.md) | 1 hafta | Test, OWASP, performans, production hazırlığı |

---

## 🔗 Bağımlılık Grafiği

```
Faz 0 ← Tüm fazlar buna bağımlı (Maven, H2 DB, Thymeleaf layout)
Faz 1 ← Faz 0 (ApiResponse, GlobalExceptionHandler, SecurityConfig)
Faz 2 ← Faz 1 (User entity, SecurityContext, getCurrentUserId)
Faz 3 ← Faz 2 (Goal entity, ownership pattern)
Faz 4 ← Faz 3 (chart-data ve stats endpoint'leri)
Faz 5 ← Faz 3 (GoalEntryCreatedEvent)
Faz 6 ← Faz 5 (BadgeEarnedEvent), Faz 1 (MailService)
Faz 7 ← Faz 2 (Goal), Faz 6 (NotificationService), Faz 1 (/users/search)
Faz 8 ← Faz 3 (GoalEntry, GoalCalculator)
Faz 9 ← Faz 0-8 (tümü)
```

---

## 🔑 Kritik Tasarım Kararları

| Karar | Seçim | Neden |
|-------|-------|-------|
| Hassas sayılar | `BigDecimal` | double/float hassasiyet kaybı var |
| Enum persist | `@Enumerated(STRING)` | Ordinal kırılgan |
| Fetch | `LAZY` everywhere | N+1 önleme |
| Token storage | Session (form login) + HttpOnly Cookie (JWT refresh) | XSS koruması |
| Event bus | Spring ApplicationEvent | Loose coupling (streak/badge) |
| Frontend | Thymeleaf + Bootstrap 5 | Server-side rendering, sıfır JS framework |
| Build Tool | Maven | `pom.xml`, Spring Boot Maven Plugin, standart Java proje yapısı |
| Chart lib | Chart.js | Thymeleaf ile uyumlu, vanilla JS |
| Heatmap | Custom CSS Grid | Bootstrap uyumlu |
| Export streaming | SXSSFWorkbook | 1000+ satır bellek dostu |
| PDF Türkçe | IDENTITY_H encoding | Standart font Türkçe sorunu |
| CSV Türkçe | UTF-8 BOM | Excel uyumluluğu |
| Veritabanı | H2 in-memory | Docker gerektirmez, geliştirme kolaylığı |

---

## 🛠️ Geliştirme Akışı

Her faz için:
1. İlgili `PHASE_X_*.md` dosyasını aç, okuma önceliği: **Dikkat Noktaları → İş Kuralları → Görev Listesi**
2. Backend → Thymeleaf templates sırasıyla ilerle
3. Her görev tamamlandığında checkbox'ı işaretle `[x]`
4. **Kabul Kriterleri**'ni test et (hepsini geç)
5. Commit: `feat(faz{N}-kisa-isim): açıklama`
6. Bir sonraki faza geç

---

*Vibe coding başlangıcı: **`PHASE_0_PROJECT_SKELETON.md`** ile başla!*
