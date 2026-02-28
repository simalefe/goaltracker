# 🔥 Faz 5 — Streak & Rozetler (Gamification)

> **Süre:** 1 hafta  
> **Bağımlılık:** Faz 3 (`GoalEntryCreatedEvent` publish edilmeli), Faz 4 (Dashboard streak iskelet hazır olmalı)  
> **Hedef:** Streak hesaplama motoru, rozet sistemi, Spring Event dinleyici, gece scheduler'ı, frontend gamification gösterimi; Dashboard'daki iskelet bileşenlerini gerçek veriyle doldur.

---

## 📋 Görev Listesi

### Backend — Entity & Repository
- [ ] `Streak.java` — Entity:
  - `@Entity`, `@Table(name = "streaks")`
  - `@OneToOne(fetch = FetchType.LAZY)` → `Goal` (UNIQUE goal_id)
  - `currentStreak`: int (default 0)
  - `longestStreak`: int (default 0)
  - `lastEntryDate`: `LocalDate` (null = hiç entry yok)
  - `@UpdateTimestamp`
- [ ] `Badge.java` — Entity (seed data ile önceden dolu):
  - `code`, `name`, `description`, `icon` (emoji)
  - `conditionType`: VARCHAR (STREAK, ENTRY_COUNT, COMPLETION, ACTIVE_GOALS, PACE_PCT)
  - `conditionValue`: int
- [ ] `UserBadge.java` — Entity:
  - `@ManyToOne` → `User`
  - `@ManyToOne` → `Badge`
  - `earnedAt`: `Instant`
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "badge_id"}))` — idempotency
- [ ] `StreakRepository.java`:
  - `Optional<Streak> findByGoalId(Long goalId)`
  - `List<Streak> findByGoal_User_Id(Long userId)` — kullanıcının tüm streak'leri
  - `List<Streak> findByGoal_StatusAndLastEntryDateBefore(GoalStatus status, LocalDate date)` — scheduler için
- [ ] `BadgeRepository.java`:
  - `List<Badge> findByConditionType(String conditionType)`
  - `Optional<Badge> findByCode(String code)`
- [ ] `UserBadgeRepository.java`:
  - `boolean existsByUserIdAndBadgeId(Long userId, Long badgeId)`
  - `List<UserBadge> findByUserId(Long userId)`
  - `long countByUserId(Long userId)`

### Backend — StreakService
- [ ] `StreakService.java`:
  - `updateStreak(Long goalId, LocalDate entryDate)`:
    - Streak kaydını getir (yoksa oluştur)
    - `lastEntryDate` null veya dün → streak++, `longestStreak` güncelle
    - `lastEntryDate` bugün → zaten güncellendi (idempotent)
    - `lastEntryDate` 2+ gün önce → streak = 1 (sıfırla ve yeniden başlat)
    - `lastEntryDate` güncelle, kaydet
    - `@Transactional` zorunlu
  - `getStreak(Long goalId)` → `StreakResponse`
  - `getUserStreaks(Long userId)` → `List<StreakResponse>` (dashboard için)
  - `getTotalStreakDays(Long userId)` → tüm aktif hedeflerin `currentStreak` toplamı
  - `resetStaleStreaks(LocalDate date)`:
    - `lastEntryDate < date - 1` (dün) VE goal ACTIVE olan streak'leri sıfırla
    - PAUSED hedeflerin streak'i **sıfırlanmaz** (dondurulmuş)
    - Batch işlem — tek sorguda tüm stale streak'leri getir

### Backend — BadgeService
- [ ] `BadgeService.java`:
  - `checkAndAwardBadges(Long userId, Long goalId)` — tüm koşulları kontrol et:
    - `checkStreakBadges(userId, maxStreakForGoal)` → STREAK koşullu badge'ler
    - `checkEntryCountBadges(userId)` → kullanıcının toplam entry sayısı
    - `checkCompletionBadges(userId)` → tamamlanan hedef sayısı
    - `checkActiveGoalsBadges(userId)` → aktif hedef sayısı
    - `checkPaceBadges(userId, goalId)` → pace % ≥ conditionValue
  - `awardBadge(Long userId, Long badgeId)`:
    - `existsByUserIdAndBadgeId` kontrolü → zaten kazanıldıysa skip (idempotent!)
    - `UserBadge` kaydet
    - Bildirim event'i publish et (Faz 6 için: `BadgeEarnedEvent`)
  - `getUserBadges(Long userId)` → `List<UserBadgeResponse>`
  - Her `awardBadge` çağrısı `@Transactional` ile korunmalı

### Backend — Scheduler
- [ ] `StreakScheduler.java`:
  - `@Scheduled(cron = "0 1 0 * * *")` — Her gece 00:01
  - `resetStaleStreaks(LocalDate.now())` çağır
  - Sıfırlanan streakler için `BadgeService.checkStreakBadges()` HAYIR (streak düştü, badge kontrol etme)
  - Log: kaç streak sıfırlandı
  - `@Async` veya transaction yönetimi — büyük kullanıcı tabanı için batch işlem

### Backend — Spring Event Sistemi
- [ ] `GoalEntryCreatedEvent.java` (Faz 3'te oluşturulmuştu) — kullanılmaya başlanır
- [ ] `BadgeEarnedEvent.java` — `ApplicationEvent` (`userId`, `badgeId`, `badgeCode`) — Faz 6 için hazırlık
- [ ] `StreakBadgeEventListener.java` — `@EventListener`:
  - `GoalEntryCreatedEvent` dinle
  - `streakService.updateStreak(goalId, entryDate)` çağır
  - `badgeService.checkAndAwardBadges(userId, goalId)` çağır
  - `@Async` ile ana transaction'ı bloke etmeden yap (fire-and-forget)
  - Exception → log at, rethrow etme (badge/streak hatası entry'yi etkilemez)

### Backend — Controller & DTO Güncellemeleri
- [ ] `GoalController.java` güncelleme:
  - `GET /api/goals/{id}/streak` → `StreakResponse`
- [ ] `UserController.java` güncelleme:
  - `GET /api/users/me/badges` → `List<UserBadgeResponse>`
  - `GET /api/users/me/stats` → kullanıcı genel istatistikleri (toplam entry, tamamlanan hedef, toplam streak)
- [ ] `DashboardService.java` güncelleme — `totalStreakDays` artık gerçek veriyle
- [ ] DTO'lar:
  - `StreakResponse.java` (`goalId`, `currentStreak`, `longestStreak`, `lastEntryDate`)
  - `BadgeResponse.java` (`id`, `code`, `name`, `description`, `icon`, `conditionType`, `conditionValue`)
  - `UserBadgeResponse.java` (`badge: BadgeResponse`, `earnedAt`)
  - `UserStatsResponse.java` (`totalEntries`, `completedGoals`, `totalStreakDays`, `earnedBadgeCount`)
  - `GoalStatsResponse.java` güncelleme — `currentStreak` ve `longestStreak` artık gerçek

### Frontend (Thymeleaf Templates)
- [ ] `templates/goals/detail.html` güncelleme — streak widget'ı (🔥 + sayı)
- [ ] `templates/goals/list.html` güncelleme — GoalCard'da streak gösterimi
- [ ] `templates/profile/index.html` — Profil sayfası:
  - Kullanıcı bilgileri + avatar (th:text, th:if)
  - UserStatsResponse özet kartları (Bootstrap card grid)
  - Kazanılan rozetler grid'i (`th:each="badge : ${badges}"`)
  - Kilitli rozetler (gri + kilit ikonu)
- [ ] `ProfileController.java` (MVC):
  - `GET /profile` → profil + rozetler + istatistikler
  - `model.addAttribute("badges", badgeService.getUserBadges(userId))`
  - `model.addAttribute("stats", userStatsService.getUserStats(userId))`

---

## 🔌 API Endpoint'leri

```
GET    /api/goals/{id}/streak
       → 200 ApiResponse<StreakResponse>
       Örnek: { goalId: 1, currentStreak: 7, longestStreak: 14, lastEntryDate: "2026-02-27" }

GET    /api/users/me/badges
       → 200 ApiResponse<List<UserBadgeResponse>>
       Örnek: [
         { badge: { code: "WEEK_WARRIOR", name: "Hafta Savaşçısı", icon: "🔥", ... }, earnedAt: "2026-02-15T..." }
       ]

GET    /api/users/me/stats
       → 200 ApiResponse<UserStatsResponse>
       Örnek: { totalEntries: 45, completedGoals: 2, totalStreakDays: 28, earnedBadgeCount: 4 }
```

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── model/
│   ├── Streak.java
│   ├── Badge.java
│   ├── UserBadge.java
│   └── event/
│       └── BadgeEarnedEvent.java
├── repository/
│   ├── StreakRepository.java
│   ├── BadgeRepository.java
│   └── UserBadgeRepository.java
├── service/
│   ├── StreakService.java
│   ├── BadgeService.java
│   └── listener/
│       └── StreakBadgeEventListener.java
├── scheduler/
│   └── StreakScheduler.java
├── controller/
│   ├── GoalController.java              (güncelleme — /streak)
│   └── UserController.java              (güncelleme — /badges, /stats)
└── dto/response/
    ├── StreakResponse.java
    ├── BadgeResponse.java
    ├── UserBadgeResponse.java
    └── UserStatsResponse.java
```

### Frontend
```
src/
├── pages/
│   ├── Profile/
│   │   └── ProfilePage.tsx
│   ├── Dashboard/
│   │   ├── DashboardPage.tsx            (güncelleme)
│   │   └── components/
│   │       └── ActiveStreaks.tsx         (güncelleme)
│   └── Goals/
│       ├── GoalDetailPage.tsx           (güncelleme — streak gerçek)
│       └── components/
│           └── GoalCard.tsx             (güncelleme — streak gerçek)
├── components/
│   ├── ui/
│   │   └── StreakCounter.tsx
│   └── badges/
│       ├── BadgeCard.tsx
│       └── BadgeGrid.tsx
├── services/
│   └── badgeService.ts
├── hooks/
│   ├── useStreak.ts
│   ├── useBadges.ts
│   └── useUserStats.ts
└── router.tsx                            (güncelleme — /profile)
```

---

## 💡 İş Kuralları

### Streak Hesaplama Mantığı
```
Entry girildiğinde (GoalEntryCreatedEvent):
  Son entry tarihi = null        → currentStreak = 1
  Son entry tarihi = dün         → currentStreak++
  Son entry tarihi = bugün       → değişme (idempotent)
  Son entry tarihi < dün - 1     → currentStreak = 1 (koptu, yeniden başladı)

longestStreak = max(longestStreak, currentStreak)

Scheduler (her gece 00:01):
  ACTIVE hedefler için:
    lastEntryDate < dün → currentStreak = 0 (dün entry girilmedi, koptu)
  PAUSED hedefler:
    streak DEĞİŞMEZ (dondurulmuş)
```

### Badge Koşulları (V8 Seed Data)
| Code | ConditionType | ConditionValue | Kontrol Zamanı |
|------|--------------|----------------|----------------|
| FIRST_STEP | ENTRY_COUNT | 1 | Entry yaratılınca |
| WEEK_WARRIOR | STREAK | 7 | Streak güncellenince |
| MONTH_CHAMPION | STREAK | 30 | Streak güncellenince |
| SPEED_DEMON | PACE_PCT | 150 | Entry yaratılınca (pace hesapla) |
| GOAL_HUNTER | COMPLETIONS | 1 | Status→COMPLETED olunca |
| MULTI_TASKER | ACTIVE_GOALS | 5 | Yeni hedef yaratılınca |
| CENTURY | STREAK | 100 | Streak güncellenince |

### Badge İdempotency
```java
// Her awardBadge çağrısında kontrol — aynı badge iki kez kazanılamaz
if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
    return; // Zaten kazanılmış, sessizce dön
}
// UNIQUE constraint DB'de de var — race condition koruması
```

### Pace Hesabı (SPEED_DEMON badge için)
```java
BigDecimal pace = currentProgress
    .divide(plannedProgress, 4, HALF_UP)
    .multiply(BigDecimal.valueOf(100));
// pace >= 150 ise SPEED_DEMON badge kontrolü yap
```

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ Badge idempotency kontrolünü atlamak → kullanıcı aynı rozeti birden fazla kazanır
//    → existsByUserIdAndBadgeId kontrolü + DB UNIQUE constraint (her ikisi)

// ❌ StreakBadgeEventListener'da exception fırlatmak
//    → Rozet/streak hatası entry kaydını etkilememeli
//    → @Async + try/catch + log

// ❌ StreakScheduler'ı @Transactional olmadan çalıştırmak
//    → Toplu güncelleme atomik olmalı

// ❌ Paused hedeflerin streak'ini sıfırlamak
//    → Scheduler'da sadece ACTIVE hedefleri filtrele

// ❌ Streak güncellemesinde timezone göz ardı etmek
//    → "dün" = kullanıcının timezone'una göre dün (şimdilik UTC kabul edilebilir)

// ❌ Race condition: 2 eşzamanlı entry → streak 2 kez artırılır
//    → @Transactional(isolation = READ_COMMITTED) + optimistic locking @Version
```

---

## 🧪 Test Senaryoları

### Backend Unit (`StreakService`)
- [ ] İlk entry → currentStreak = 1
- [ ] Dün entry girildi, bugün de giriliyor → currentStreak = 2
- [ ] 2 gün önce entry girildi, bugün giriliyor → currentStreak = 1 (sıfırlandı)
- [ ] Bugün zaten entry var (aynı gün 2. event) → streak değişmez (idempotent)
- [ ] `longestStreak` doğru güncelleniyor (her zaman max alınıyor)
- [ ] Scheduler: ACTIVE hedef dünden eski entry → streak = 0
- [ ] Scheduler: PAUSED hedef → streak değişmez

### Backend Unit (`BadgeService`)
- [ ] FIRST_STEP: 1. entry sonrası kazanılıyor
- [ ] WEEK_WARRIOR: streak 7'ye ulaşınca kazanılıyor
- [ ] WEEK_WARRIOR: zaten kazanılmışsa tekrar `awardBadge` çağrısı → DB'ye yeni kayıt eklenmez
- [ ] SPEED_DEMON: pace >= 150 kontrolü doğru çalışıyor
- [ ] MULTI_TASKER: 5. aktif hedef oluşturulunca kazanılıyor

### Frontend
- [ ] GoalCard'da streak sayısı gerçek değer gösteriyor
- [ ] BadgeGrid: kazanılan rozetler doğru gösteriliyor
- [ ] Yeni rozet kazanılınca toast bildirimi gösteriliyor (Faz 6'ya bağımlı, şimdi opsiyonel)

---

## ✅ Kabul Kriterleri

### Streak
- [ ] Entry girildiğinde streak güncelleniyor (GoalEntryCreatedEvent → StreakBadgeEventListener)
- [ ] Dün entry girilmişse bugün girince streak artar
- [ ] Gün atlanmışsa (dünden daha eski son entry) streak sıfırlanır
- [ ] PAUSED hedeflerde streak scheduler tarafından sıfırlanmaz
- [ ] `longestStreak` her zaman `currentStreak`'ten büyük veya eşit
- [ ] Gece 00:01 scheduler'ı çalışır, ACTIVE hedeflerin stale streak'lerini sıfırlar
- [ ] Streak hatası (exception) entry kaydını etkilemez (bağımsız @Async)

### Rozetler
- [ ] FIRST_STEP rozeti ilk entry sonrası kazanılıyor
- [ ] WEEK_WARRIOR rozeti 7 günlük streak sonrası kazanılıyor
- [ ] Aynı rozet hiçbir koşulda iki kez kazanılamıyor (DB UNIQUE + service kontrolü)
- [ ] `GET /api/users/me/badges` kazanılan rozetleri döndürüyor
- [ ] `GET /api/users/me/stats` doğru istatistikleri döndürüyor

### Frontend
- [ ] GoalCard'da streak sayısı doğru (API'den gelen gerçek veri)
- [ ] GoalDetailPage StatsPanel'inde currentStreak ve longestStreak gerçek
- [ ] Dashboard `ActiveStreaks` bölümü hedef bazlı streak sıralaması gösteriyor
- [ ] Dashboard `totalStreakDays` gerçek değer gösteriyor
- [ ] `BadgeGrid` kazanılan rozetleri icon, isim, açıklama ve tarihiyle gösteriyor
- [ ] ProfilePage kullanıcı istatistiklerini ve rozet koleksiyonunu gösteriyor
