# 👥 Faz 7 — Sosyal Özellikler

> **Süre:** 1 hafta  
> **Bağımlılık:** Faz 2 (Goal, GoalService), Faz 6 (NotificationService — arkadaş aktivite bildirimi), Faz 1 (UserController — `/api/users/search`)  
> **Hedef:** Arkadaşlık sistemi, hedef paylaşımı, kategori bazlı liderboard, aktivite akışı.

---

## 📋 Görev Listesi

### Backend — Entity & Repository
- [ ] `Friendship.java` — Entity:
  - `@Entity`, `@Table(name = "friendships")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `requester: User`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `receiver: User`
  - `status`: `FriendshipStatus` enum (PENDING, ACCEPTED, BLOCKED)
  - `@CreationTimestamp`
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "receiver_id"}))` — duplicate istek engeli
  - DB constraint: `chk_no_self_friend` (requester_id != receiver_id) — Flyway'de
- [ ] `GoalShare.java` — Entity:
  - `@ManyToOne(fetch = FetchType.LAZY)` → `goal: Goal`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `sharedWithUser: User`
  - `permission`: `SharePermission` enum (READ, COMMENT)
  - `@CreationTimestamp`
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "shared_with_user_id"}))`
- [ ] `FriendshipStatus.java` enum — `PENDING`, `ACCEPTED`, `BLOCKED`
- [ ] `SharePermission.java` enum — `READ`, `COMMENT`
- [ ] `FriendshipRepository.java`:
  - `Optional<Friendship> findByRequesterIdAndReceiverId(Long rid, Long rcvId)`
  - `List<Friendship> findByReceiverIdAndStatus(Long receiverId, FriendshipStatus status)` — gelen istekler
  - `List<Friendship> findByRequesterIdAndStatus(Long requesterId, FriendshipStatus status)` — gönderilen istekler
  - Custom JPQL: `findAcceptedFriends(Long userId)` → her iki yönde ACCEPTED friendship
  - `boolean areFriends(Long userId1, Long userId2)` — friendship durumu kontrol
- [ ] `GoalShareRepository.java`:
  - `List<GoalShare> findBySharedWithUserId(Long userId)` — bana paylaşılanlar
  - `Optional<GoalShare> findByGoalIdAndSharedWithUserId(Long goalId, Long userId)`
  - `boolean existsByGoalIdAndSharedWithUserId(Long goalId, Long userId)`

### Backend — FriendshipService
- [ ] `FriendshipService.java`:
  - `sendRequest(Long requesterId, String receiverUsername)`:
    - Kendine istek engeli
    - Zaten arkadaş mı? → 409 `ALREADY_FRIENDS`
    - Zaten bekleyen istek var mı? → 409 `REQUEST_ALREADY_SENT`
    - `Friendship` kaydı (PENDING)
    - Karşı tarafa bildirim gönder (`NotificationService.createNotification`)
  - `acceptRequest(Long friendshipId, Long currentUserId)`:
    - Ownership: sadece `receiver` kabul edebilir
    - Status PENDING'den ACCEPTED'a geçiş
    - `requester`'a bildirim gönder
  - `rejectRequest(Long friendshipId, Long currentUserId)`:
    - Sadece `receiver` reddedebilir
    - Kaydı sil (veya REJECTED enum'u ekle)
  - `removeFriend(Long friendshipId, Long currentUserId)`:
    - Her iki taraf kaldırabilir
    - Kaydı sil
  - `blockUser(Long friendshipId, Long currentUserId)`:
    - Status → BLOCKED
    - Bloke eden kişi blokeyi kaldırabilir
  - `getFriends(Long userId)` → `List<FriendResponse>`
  - `getPendingRequests(Long userId)` → gelen + giden istekler
  - `getFriendStatus(Long userId, Long otherUserId)` → `FriendshipStatus | null`

### Backend — GoalShare & Leaderboard
- [ ] `GoalService.java` güncelleme — paylaşım metodları:
  - `shareGoal(Long goalId, Long ownerUserId, Long targetUserId, SharePermission permission)`:
    - Ownership kontrolü (goal.user == ownerUserId)
    - `areFriends` kontrolü — sadece arkadaşlarla paylaş
    - Duplicate share kontrolü → 409
    - `GoalShare` kaydet
    - Karşı tarafa FRIEND_ACTIVITY bildirimi
  - `removeShare(Long goalId, Long ownerUserId, Long targetUserId)`
  - `getSharedGoals(Long userId)` → arkadaşların paylaştığı hedefler
  - `getSharedWithUsers(Long goalId, Long ownerUserId)` → bu hedefi kimlerle paylaştım
- [ ] `LeaderboardService.java`:
  - `getLeaderboard(Long userId, GoalCategory category)`:
    - Kullanıcı + arkadaşları arasından sıralama
    - Kriter: completionPct DESC, currentStreak DESC
    - Her kullanıcı için en iyi hedefini al (belirtilen kategoride)
    - Mevcut kullanıcının sıralaması highlighted

### Backend — Aktivite Akışı
- [ ] `ActivityFeedService.java`:
  - `getFeed(Long userId)`:
    - Arkadaşların son 7 günlük hareketleri
    - Olaylar: entry girme, hedef tamamlama, rozet kazanma
    - Son 20 aktivite, tarih DESC
  - Performans: özel JPQL sorgusu, tek sorguda tüm aktiviteler (UNION değil, son N'i döndürecek şekilde tasarla)

### Backend — Controller
- [ ] `FriendshipController.java`:
  - `GET /api/friends` — arkadaş listesi
  - `POST /api/friends/request` — istek gönder
  - `PUT /api/friends/{id}/accept` — kabul et
  - `DELETE /api/friends/{id}` — reddet / kaldır
  - `GET /api/friends/pending` — bekleyen istekler
  - `GET /api/friends/{userId}/status` — iki kullanıcı arasındaki ilişki
- [ ] `GoalController.java` güncelleme — paylaşım endpoint'leri:
  - `POST /api/goals/{id}/share`
  - `DELETE /api/goals/{id}/share/{userId}`
  - `GET /api/goals/{id}/shared-with`
  - `GET /api/goals/shared-with-me`
- [ ] `LeaderboardController.java` — `GET /api/leaderboard`
- [ ] `ActivityFeedController.java` — `GET /api/social/activity-feed`

### Backend — DTO'lar
- [ ] `FriendRequestRequest.java` (`receiverUsername`)
- [ ] `FriendResponse.java` (`userId`, `username`, `displayName`, `avatarUrl`, `friendsSince: Instant`)
- [ ] `FriendRequestResponse.java` (`id`, `requester/receiver`, `status`, `createdAt`)
- [ ] `ShareGoalRequest.java` (`userId: Long`, `permission: SharePermission`)
- [ ] `LeaderboardEntryResponse.java` (`rank`, `userId`, `username`, `displayName`, `avatarUrl`, `completionPct`, `currentStreak`, `goalTitle`, `isCurrentUser`)
- [ ] `ActivityFeedItemResponse.java` (`type`, `userId`, `username`, `avatarUrl`, `goalTitle`, `value`, `unit`, `timestamp`)

### Frontend (Thymeleaf Templates)
- [ ] `templates/social/index.html` — Sosyal ana sayfa (Bootstrap Nav Tabs):
  - Tab 1: Arkadaşlar — `th:each="friend : ${friends}"` + istek gönderme formu
  - Tab 2: Liderboard — kategori filtresi + `th:each="entry : ${leaderboard}"` tablosu
  - Tab 3: Aktivite Akışı — `th:each="item : ${feedItems}"` listesi
- [ ] `templates/social/search.html` — Kullanıcı arama sonuçları (HTMX veya form submit)
- [ ] `templates/goals/detail.html` güncelleme — hedef sahibi için paylaşım formu
- [ ] `SocialController.java` (MVC):
  - `GET /social` → social/index.html
  - `POST /social/friends/request` → istek gönder, redirect
  - `POST /social/friends/{id}/accept` → kabul et, redirect
  - `DELETE /social/friends/{id}` → kaldır, redirect
  - `GET /social/leaderboard?category=...` → liderboard tablosu
  - `GET /social/activity-feed` → aktivite akışı

---

## 🔌 API Endpoint'leri

```
GET    /api/friends                               → Arkadaş listesi
POST   /api/friends/request                       → { receiverUsername }
PUT    /api/friends/{id}/accept                   → İsteği kabul et
DELETE /api/friends/{id}                          → Reddet / Kaldır
GET    /api/friends/pending                       → { incoming: [...], outgoing: [...] }
GET    /api/friends/{userId}/status               → { status: "ACCEPTED" | "PENDING" | null }

POST   /api/goals/{id}/share                      → { userId, permission: "READ" }
DELETE /api/goals/{id}/share/{userId}             → Paylaşımı kaldır
GET    /api/goals/{id}/shared-with                → Bu hedefi kimlerle paylaştım
GET    /api/goals/shared-with-me                  → Arkadaşların bana paylaştığı hedefler

GET    /api/leaderboard?category=EDUCATION        → Sıralama listesi

GET    /api/social/activity-feed                  → Son aktiviteler
```

---

## 📁 Dosyalar

### Backend
```
model/Friendship.java, GoalShare.java, enums/FriendshipStatus.java, enums/SharePermission.java
repository/FriendshipRepository.java, GoalShareRepository.java
service/FriendshipService.java, LeaderboardService.java, ActivityFeedService.java
controller/FriendshipController.java, LeaderboardController.java, ActivityFeedController.java
GoalController.java (güncelleme), GoalService.java (güncelleme)
dto/request/FriendRequestRequest.java, ShareGoalRequest.java
dto/response/FriendResponse.java, FriendRequestResponse.java, LeaderboardEntryResponse.java, ActivityFeedItemResponse.java
```

### Thymeleaf Templates
```
src/main/resources/templates/
└── social/
    ├── index.html       (arkadaşlar + liderboard + aktivite tab'ları)
    └── search.html      (kullanıcı arama sonuçları fragment'i)
```

---

## 💡 İş Kuralları

### Arkadaşlık Durumu Geçişleri
```
A → sendRequest → B : PENDING
B → acceptRequest   : ACCEPTED (her iki taraf arkadaş olur)
B → rejectRequest   : Kayıt silinir
A veya B → remove   : Kayıt silinir
A → blockUser       : BLOCKED (bloke eden kaldırabilir)
```

### Paylaşım Kısıtlamaları
- Sadece **arkadaşlarla** paylaşılabilir
- Hedef sahibi kendi hedefini paylaşabilir
- Paylaşılan hedef silinirse → `GoalShare` CASCADE ile silinir
- BLOCKED kullanıcılar birbirinin paylaşımını göremez

### Liderboard Algoritması
```
1. userId'nin arkadaşlarını getir (+ kendisi)
2. Belirtilen kategoride ACTIVE hedefleri olan kullanıcıları filtrele
3. Her kullanıcının en yüksek completionPct'li hedefini al
4. completionPct DESC, currentStreak DESC ile sırala
5. rank numaralandır (1'den başla)
6. currentUserId olan kayıt → isCurrentUser: true
```

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ Kendine arkadaşlık isteği → 400 Bad Request (DB constraint + service kontrolü)
// ❌ BLOCKED kullanıcının hedeflerini/profilini göstermek → güvenlik açığı
// ❌ Liderboard'da arkadaş olmayan kullanıcıları göstermek → privacy ihlali
// ❌ Paylaşılmış hedefi sadece sahip görmeli diye kontrol yapmamak
//    → Hem hedef sahibi hem paylaşılan kullanıcı görebilir
// ❌ Aktivite akışı N+1 sorgusu → tek sorguda users JOIN goals JOIN entries
```

---

## ✅ Kabul Kriterleri

### Arkadaşlık
- [ ] Kullanıcı username ile arkadaşlık isteği gönderebilir
- [ ] Kendine istek gönderme → 400 Bad Request
- [ ] Gelen istek kabul edilince her iki taraf arkadaş görünür
- [ ] Reddedilen veya kaldırılan arkadaşlık silinir
- [ ] BLOCKED kullanıcılar birbirini arama sonuçlarında göremez
- [ ] Bekleyen istekler (gelen/giden) listeleniyor

### Hedef Paylaşımı
- [ ] Hedef sadece arkadaşlarla paylaşılabilir (arkadaş olmayan → 403)
- [ ] Paylaşılan hedef paylaşılan kullanıcı tarafından görülebilir (READ)
- [ ] Paylaşım kaldırılınca hedef artık görünmüyor
- [ ] Hedef silinince paylaşım kayıtları da cascade silinir

### Liderboard & Feed
- [ ] Liderboard sadece kullanıcı + arkadaşlarını gösteriyor
- [ ] Kategori filtresi çalışıyor
- [ ] Mevcut kullanıcının sıralaması vurgulanmış
- [ ] Aktivite akışı arkadaşların son 7 günlük hareketlerini gösteriyor

### Frontend
- [ ] SocialPage tab'ları çalışıyor (Arkadaşlar, Liderboard, Aktivite)
- [ ] Arkadaş ekleme: username ile arama, istek gönderme çalışıyor
- [ ] Liderboard tablosunda sıralama ve altın/gümüş/bronz ikonlar var
- [ ] Aktivite feed infinite scroll çalışıyor
