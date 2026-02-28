# 🔔 Faz 6 — Bildirim Sistemi

> **Süre:** 1 hafta  
> **Bağımlılık:** Faz 5 (`BadgeEarnedEvent` publish edilmeli), Faz 1 (MailService hazır olmalı), Faz 0 (WebSocketConfig iskeleti hazır olmalı)  
> **Hedef:** E-posta, uygulama içi ve gerçek zamanlı WebSocket bildirimleri; scheduler'lar; bildirim ayarları; Navbar'daki bildirim zili.

---

## 📋 Görev Listesi

### Backend — Entity & Repository
- [ ] `Notification.java` — Entity:
  - `@Entity`, `@Table(name = "notifications")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `User`
  - `type`: VARCHAR(50) — NotificationType enum
  - `title`, `message`
  - `isRead`: boolean (default false)
  - `metadata`: JSONB — `Map<String, Object>` (@Type(JsonBinaryType) veya String)
  - `scheduledAt`, `sentAt`: `Instant`
  - `@CreationTimestamp`
- [ ] `NotificationSettings.java` — Entity:
  - `@OneToOne(fetch = FetchType.LAZY)` → `User` (UNIQUE user_id)
  - `emailEnabled`, `pushEnabled`: boolean
  - `dailyReminderTime`: `LocalTime` (default 20:00)
  - `weeklySummaryDay`: int (1=Pazartesi, default 1)
  - `weeklySummaryEnabled`: boolean (default true)
  - `streakDangerEnabled`: boolean (default true) — yeni eklenen alan
- [ ] `NotificationType.java` enum — `DAILY_REMINDER`, `STREAK_DANGER`, `STREAK_LOST`, `BADGE_EARNED`, `GOAL_COMPLETED`, `WEEKLY_SUMMARY`, `FRIEND_ACTIVITY`
- [ ] `NotificationRepository.java`:
  - `Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable)`
  - `long countByUserIdAndIsReadFalse(Long userId)` — unread count
  - `List<Notification> findByUserIdAndIsReadFalse(Long userId)`
  - `@Query("SELECT n FROM Notification n WHERE n.scheduledAt <= :now AND n.sentAt IS NULL") List<Notification> findPendingNotifications(Instant now)`
- [ ] `NotificationSettingsRepository.java` — `findByUserId`, `findOrCreateByUserId`

### Backend — NotificationService
- [ ] `NotificationService.java`:
  - `createNotification(Long userId, NotificationType type, String title, String message, Map<String,Object> metadata)`:
    - DB'ye kaydet
    - Kullanıcının ayarlarını kontrol et (email/push enabled?)
    - `sendWebSocketNotification(userId, notification)` — anlık
    - Gerekiyorsa `sendEmailNotification(notification)`
  - `sendWebSocketNotification(Long userId, Notification notification)`:
    - `messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", dto)`
  - `sendEmailNotification(Notification notification)`:
    - HTML e-posta şablonu render et (Thymeleaf veya string concat)
    - `javaMailSender.send(...)` — async ile gönder, hata logla
    - `sentAt` güncelle
  - `markAsRead(Long notificationId, Long userId)`:
    - Ownership kontrolü
    - `isRead = true`
  - `markAllAsRead(Long userId)` — toplu güncelleme
  - `getNotifications(Long userId, Pageable pageable)` → sayfalı liste
  - `getUnreadCount(Long userId)` → int
  - `getSettings(Long userId)` → `NotificationSettingsResponse`
  - `updateSettings(Long userId, NotificationSettingsRequest)` → `NotificationSettingsResponse`
  - `ensureSettingsExist(Long userId)` — user oluşturulunca default settings oluştur

### Backend — Scheduler
- [ ] `NotificationScheduler.java`:
  - `sendDailyReminders()`:
    - `@Scheduled(cron = "0 * * * * *")` — her dakika çalışır
    - Şu an `dailyReminderTime` olan kullanıcıları bul (dakika hassasiyetiyle)
    - Bugün henüz entry girilmemiş aktif hedefleri olan kullanıcılara bildirim
    - Aynı kullanıcıya bugün zaten gönderilmişse tekrar gönderme
  - `sendStreakDangerWarnings()`:
    - `@Scheduled(cron = "0 0 22 * * *")` — Saat 22:00
    - O gün entry girilmemiş, streak > 0 olan ACTIVE hedefler → uyarı gönder
    - `streakDangerEnabled = true` olan kullanıcılar için
  - `sendWeeklySummaries()`:
    - `@Scheduled(cron = "0 0 9 * * MON")` — Her Pazartesi 09:00
    - `weeklySummaryEnabled = true` olan kullanıcılar
    - Geçen haftanın özeti: entry sayısı, tamamlanan %, ortalama streak

### Backend — Event Dinleyiciler
- [ ] `NotificationEventListener.java`:
  - `@EventListener BadgeEarnedEvent` → `createNotification(BADGE_EARNED)`
  - `@EventListener GoalCompletedEvent` → `createNotification(GOAL_COMPLETED)` (Faz 2'de tanımla)
  - `@Async` ile çalışmalı
- [ ] `GoalCompletedEvent.java` — (Faz 2'de yayınlanan, burada dinleniyor)

### Backend — WebSocket Yapılandırması
- [ ] `WebSocketConfig.java` — tam implementasyon:
  - `@EnableWebSocketMessageBroker`
  - `registerStompEndpoints("/ws")` + SockJS fallback
  - `configureMessageBroker`: `/topic`, `/queue` prefix, `/app` destination prefix
  - CORS ayarı (`localhost:5173` izinli)
- [ ] `WebSocketAuthInterceptor.java` — STOMP CONNECT'te JWT doğrulama:
  - `StompHeaderAccessor`'dan `Authorization` header oku
  - Token doğrula → `Principal` set et
- [ ] `NotificationController.java`:
  - `GET /api/notifications` — sayfalı
  - `GET /api/notifications/unread-count`
  - `PUT /api/notifications/{id}/read`
  - `PUT /api/notifications/read-all`
  - `GET /api/notification-settings`
  - `PUT /api/notification-settings`

### Backend — E-posta Şablonları
- [ ] `templates/email/daily-reminder.html` — Günlük hatırlatıcı şablonu
- [ ] `templates/email/weekly-summary.html` — Haftalık özet şablonu
- [ ] `templates/email/badge-earned.html` — Rozet kazanımı şablonu
- [ ] `templates/email/goal-completed.html` — Hedef tamamlama şablonu

### Frontend (Thymeleaf Templates)
- [ ] `templates/layout/navbar.html` güncelleme — Bildirim zili:
  - `th:text="${unreadCount}"` ile badge sayısı
  - Dropdown: son 10 bildirim `th:each` ile listele
  - "Tümünü okundu işaretle" formu (POST submit)
  - "Tüm bildirimleri gör" linki
- [ ] `templates/notifications/list.html` — Tüm bildirimler sayfası:
  - Sayfalama (`fragments/pagination.html`)
  - Tip bazlı filtre (Bootstrap select → form submit)
  - Her bildirim: ikon, başlık, mesaj, zaman, okundu/okunmadı stili
- [ ] `templates/settings/notifications.html` — Bildirim ayarları:
  - E-posta bildirimleri toggle (Bootstrap switch)
  - Günlük hatırlatma saati (`<input type="time">`)
  - Streak uyarısı toggle
  - Haftalık özet toggle + gün seçimi
  - Kaydet butonu (form submit POST)
- [ ] `NotificationController.java` (MVC):
  - `GET /notifications` → notifications/list.html
  - `POST /notifications/{id}/read` → redirect back
  - `POST /notifications/read-all` → redirect back
  - `GET /settings/notifications` → settings/notifications.html
  - `POST /settings/notifications` → ayarları kaydet, redirect
- [ ] `static/js/notifications.js` — WebSocket/STOMP istemci:
  - Kimlik doğrulaması sonrası `/ws` endpoint'ine bağlan
  - `/user/queue/notifications` subscribe
  - Yeni bildirim gelince: navbar badge sayısını güncelle + Bootstrap toast göster

---

## 🔌 API Endpoint'leri

```
GET    /api/notifications?page=0&size=20
       → 200 ApiResponse<Page<NotificationResponse>>

GET    /api/notifications/unread-count
       → 200 ApiResponse<{ count: number }>

PUT    /api/notifications/{id}/read
       → 200 ApiResponse<null>

PUT    /api/notifications/read-all
       → 200 ApiResponse<null>

GET    /api/notification-settings
       → 200 ApiResponse<NotificationSettingsResponse>

PUT    /api/notification-settings
       Body: { emailEnabled, pushEnabled, dailyReminderTime, streakDangerEnabled, weeklySummaryEnabled, weeklySummaryDay }
       → 200 ApiResponse<NotificationSettingsResponse>

WebSocket: ws://localhost:8080/ws (STOMP over SockJS)
  Subscribe: /user/queue/notifications
  Receive: NotificationResponse JSON
```

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── model/
│   ├── Notification.java
│   ├── NotificationSettings.java
│   └── enums/NotificationType.java
├── repository/
│   ├── NotificationRepository.java
│   └── NotificationSettingsRepository.java
├── service/
│   ├── NotificationService.java
│   └── listener/
│       └── NotificationEventListener.java
├── scheduler/
│   └── NotificationScheduler.java
├── controller/
│   └── NotificationController.java
├── config/
│   ├── WebSocketConfig.java             (tam implementasyon)
│   └── WebSocketAuthInterceptor.java
├── dto/
│   ├── request/NotificationSettingsRequest.java
│   └── response/
│       ├── NotificationResponse.java
│       └── NotificationSettingsResponse.java
└── resources/templates/email/
    ├── daily-reminder.html
    ├── weekly-summary.html
    ├── badge-earned.html
    └── goal-completed.html
```

### Thymeleaf Templates
```
src/main/resources/templates/
├── notifications/
│   └── list.html
└── settings/
    └── notifications.html
```

### Static Files
```
src/main/resources/static/js/
└── notifications.js    (WebSocket/STOMP istemci)
```

---

## 💡 İş Kuralları

### Bildirim Tipleri & Tetikleyiciler
| Tip | Tetikleyici | Kanal | Koşul |
|-----|-------------|-------|-------|
| DAILY_REMINDER | Scheduler (kullanıcı saati) | E-posta + App | Bugün entry yok + aktif hedef var |
| STREAK_DANGER | Scheduler (22:00) | App | streak > 0, bugün entry yok |
| STREAK_LOST | StreakScheduler (00:01) | E-posta + App | streak 0'a düştü |
| BADGE_EARNED | BadgeEarnedEvent | App (WebSocket) | Rozet kazanıldı |
| GOAL_COMPLETED | GoalCompletedEvent | E-posta + App | Status → COMPLETED |
| WEEKLY_SUMMARY | Scheduler (Pazartesi 09:00) | E-posta | weeklySummaryEnabled |
| FRIEND_ACTIVITY | FriendshipService (Faz 7) | App | Arkadaş hedef paylaştı |

### Scheduler Optimizasyonu
- Daily reminder: Tüm kullanıcıları taramak yerine aynı saati seçenler grupla
- Streak danger: 22:00 tek bir batch — o gün entry girilmemiş hedefleri DB'den sorgula
- Aynı bildirim tipi için aynı gün 2. kez gönderme — `sentAt` tarih kontrolü

### WebSocket Bağlantı Yönetimi
```typescript
// Bağlantı stratejisi
const connect = () => {
  stompClient.activate();
  stompClient.onConnect = () => {
    stompClient.subscribe('/user/queue/notifications', onMessage);
  };
  stompClient.onDisconnect = () => scheduleReconnect();  // exponential backoff
};
// Sayfa görünür olunca (visibilitychange) → bağlantı kontrolü
```

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ E-posta gönderimini main thread'de yapmak → request yavaşlar
//    → @Async ile arka planda gönder

// ❌ WebSocket'te kullanıcı doğrulaması yapmamak → güvenlik açığı
//    → WebSocketAuthInterceptor ile JWT kontrolü

// ❌ Her scheduler'da tüm kullanıcıları yüklemek → OutOfMemoryError
//    → Pageable + batch processing

// ❌ Aynı bildirimi günde birden fazla gönderme
//    → sentAt date kontrol ile engelle

// ❌ NotificationSettings yokken NullPointerException
//    → User oluşturulduğunda default settings oluştur
```

---

## ✅ Kabul Kriterleri

### Backend
- [ ] `POST /api/auth/register` sonrası kullanıcı için `NotificationSettings` default olarak oluşturuluyor
- [ ] `GET /api/notifications` sayfalı bildirimler döndürüyor
- [ ] `GET /api/notifications/unread-count` doğru sayıyı döndürüyor
- [ ] `PUT /api/notifications/{id}/read` okundu işaretliyor (ownership kontrolü var)
- [ ] `PUT /api/notifications/read-all` tüm bildirimleri okundu yapıyor
- [ ] Bildirim ayarları güncellenebiliyor
- [ ] BADGE_EARNED event'i → WebSocket bildirimi anlık olarak frontend'e ulaşıyor
- [ ] GOAL_COMPLETED → e-posta gönderiliyor (settings.emailEnabled = true ise)
- [ ] Daily reminder scheduler: bugün entry girmeyen kullanıcılara gönderiliyor
- [ ] Streak danger: 22:00'de o gün entry girilmemiş, streak > 0 olan hedefler için uyarı
- [ ] Aynı bildirim aynı gün iki kez gönderilmiyor

### Frontend
- [ ] Navbar'daki bildirim zili unread count badge gösteriyor
- [ ] Zile tıklayınca dropdown açılır, son 10 bildirim listelenir
- [ ] Bildirimlere tıklayınca okundu işaretleniyor
- [ ] WebSocket bağlantısı kurulunca yeni bildirimler anlık toast ve count artışıyla gösteriliyor
- [ ] WebSocket bağlantısı kopunca otomatik yeniden bağlanıyor
- [ ] `NotificationsPage` sayfalama ile tüm bildirimleri gösteriyor
- [ ] Bildirim ayarları formu kaydedilebiliyor
- [ ] Tüm bildirim metinleri Türkçe
