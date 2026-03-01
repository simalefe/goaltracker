# 🔔 Faz 6 — Bildirimler & Profil E2E Testleri

| Alan | Değer |
|------|-------|
| Süre | 1-1.5 gün |
| Bağımlılık | Faz 0 (altyapı), Faz 1 (login) |
| Hedef | Bildirim listesi, okundu işaretleme, tür filtreleme, bildirim ayarları, profil sayfası, rozet görünümü |
| Test Sayısı | ~15 test case |
| Allure Epic | `Notifications`, `Profile` |

---

## 📁 Oluşturulacak Dosyalar

### Page Object'ler

#### 1. `NotificationListPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/notification/NotificationListPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Bildirim item'ları | `.notification-item`, `table tbody tr`, `.list-group-item` | Her bildirim satırı |
| Okunmamış sayacı | `.unread-count`, `.badge` | Okunmamış bildirim sayısı |
| Tümünü Okundu İşaretle | `form[action*='read-all'] button`, `.mark-all-read` | Toplu okundu butonu |
| Tek okundu butonu | `form[action*='/read'] button`, `.mark-read` | Tek bildirim okundu |
| Tür filtre chip'leri | `a[href*='type=']`, `.filter-chip` | Bildirim türü filtreleri |
| Bildirim başlığı | `.notification-title` | Her bildirimin başlığı |
| Bildirim mesajı | `.notification-message` | Her bildirimin mesajı |
| Bildirim tarihi | `.notification-date` | Tarih |
| Okunmamış badge | `.unread`, `.badge-primary` | Okunmamış işareti |
| Pagination | `.pagination` | Sayfalama |
| Boş mesaj | `.empty-state`, `.empty-notifications` | "Bildirim yok" |
| Sayfa başlığı | `h1`, `h2` | "Bildirimler" |

**Fluent Metotlar:**
```java
// Okuma
int getNotificationCount()
int getUnreadCount()
List<String> getNotificationTitles()
String getNotificationTitle(int index)
String getNotificationMessage(int index)
boolean isNotificationUnread(int index)
boolean isNotificationListEmpty()
boolean isUnreadCountVisible()

// Aksiyon
NotificationListPage markAsRead(int index)
NotificationListPage markAllAsRead()
NotificationListPage filterByType(String type)
NotificationListPage clearFilter()

// Navigasyon
NotificationSettingsPage clickSettings()

// Mesajlar
String getSuccessMessage()
boolean isSuccessMessageVisible()
boolean isOnNotificationPage()
```

---

#### 2. `NotificationSettingsPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/notification/NotificationSettingsPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Email bildirimleri toggle | `#emailEnabled`, `input[name='emailEnabled']` | E-posta bildirimi aç/kapat |
| Push bildirimleri toggle | `#pushEnabled`, `input[name='pushEnabled']` | Push bildirimi aç/kapat |
| Streak uyarı toggle | `#streakDangerEnabled`, `input[name='streakDangerEnabled']` | Streak tehlike uyarısı |
| Haftalık özet toggle | `#weeklySummaryEnabled`, `input[name='weeklySummaryEnabled']` | Haftalık özet e-postası |
| Günlük hatırlatıcı saat | `#dailyReminderTime`, `input[name='dailyReminderTime']` | Hatırlatıcı saati |
| Kaydet butonu | `button[type='submit']` | Ayarları kaydet |
| Başarı mesajı | `.alert-success` | "Ayarlar güncellendi" |
| Hata mesajı | `.alert-danger` | Hata |

**Fluent Metotlar:**
```java
// Okuma
boolean isEmailEnabled()
boolean isPushEnabled()
boolean isStreakDangerEnabled()
boolean isWeeklySummaryEnabled()
String getDailyReminderTime()

// Aksiyon
NotificationSettingsPage toggleEmail(boolean enabled)
NotificationSettingsPage togglePush(boolean enabled)
NotificationSettingsPage toggleStreakDanger(boolean enabled)
NotificationSettingsPage toggleWeeklySummary(boolean enabled)
NotificationSettingsPage setDailyReminderTime(String time)
NotificationSettingsPage save()

// Toggle yardımcı
private NotificationSettingsPage setCheckbox(By locator, boolean desired)

// Mesajlar
String getSuccessMessage()
boolean isSuccessMessageVisible()
boolean isOnSettingsPage()
```

---

#### 3. `ProfilePage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/profile/ProfilePage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Kullanıcı adı | `.username`, `h1`, `h2` | Kullanıcı adı/display name |
| Email | `.user-email` | E-posta |
| Toplam giriş istatistik | `.stat-total-entries .stat-value` | Toplam entry sayısı |
| Tamamlanan hedef istatistik | `.stat-completed-goals .stat-value` | Tamamlanan hedef |
| Streak gün istatistik | `.stat-streak-days .stat-value` | Toplam streak gün |
| Rozet sayısı istatistik | `.stat-badge-count .stat-value` | Kazanılan rozet sayısı |
| Kazanılan rozetler | `.earned-badges .badge-item` | Kazanılmış rozet kartları |
| Kilitli rozetler | `.locked-badges .badge-item` | Kazanılmamış rozetler |
| Rozet adı | `.badge-name` | Rozet başlığı |
| Rozet icon | `.badge-icon` | Emoji icon |
| Sayfa başlığı | `h1`, `.page-title` | "Profil" |

**Fluent Metotlar:**
```java
// Okuma
String getDisplayName()
String getUsername()
String getTotalEntries()
String getCompletedGoals()
String getTotalStreakDays()
String getEarnedBadgeCount()

// Rozetler
List<String> getEarnedBadgeNames()
List<String> getLockedBadgeNames()
int getEarnedBadgeDisplayCount()
int getLockedBadgeDisplayCount()
boolean isBadgeEarned(String badgeName)

// Doğrulama
boolean isOnProfilePage()
boolean isStatsCardVisible()
```

---

## 🧪 Test Sınıfları

### `NotificationE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/notification/NotificationE2eTest.java`

**Allure Metadata:**
```java
@Epic("Notifications")
@Feature("Bildirim Yönetimi")
```

---

### Test Case'ler — Bildirim Listesi

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 1 | `shouldShowNotificationListPage` | Bildirim sayfası doğru render edilir | 1. Login yap<br>2. `/notifications` sayfasına git | ✅ URL `/notifications` içerir<br>✅ Sayfa başlığı "Bildirimler" |
| 2 | `shouldShowEmptyNotificationList` | Yeni kullanıcıda bildirim yok | 1. Yeni user ile login<br>2. Bildirimler sayfasına git | ✅ Boş durum mesajı veya 0 bildirim<br>✅ "Tümünü Okundu İşaretle" butonu gizli/devre dışı |
| 3 | `shouldShowNotificationsWithUnreadCount` | Bildirimler ve okunmamış sayaç | 1. DB helper ile 3 bildirim oluştur (2 okunmamış)<br>2. Login → bildirimler | ✅ Bildirim sayısı ≥ 3<br>✅ Okunmamış sayacı 2 |

---

### Test Case'ler — Okundu İşaretleme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 4 | `shouldMarkSingleNotificationAsRead` | Tek bildirimi okundu işaretler | 1. 2 okunmamış bildirim oluştur (DB)<br>2. Login → bildirimler<br>3. İlk bildirimi okundu işaretle | ✅ "Bildirim okundu olarak işaretlendi" mesajı<br>✅ Okunmamış sayaç 1'e düşer |
| 5 | `shouldMarkAllNotificationsAsRead` | Tüm bildirimleri okundu işaretler | 1. 3 okunmamış bildirim oluştur (DB)<br>2. Login → bildirimler<br>3. "Tümünü Okundu İşaretle" tıkla | ✅ "Tüm bildirimler okundu" mesajı<br>✅ Okunmamış sayaç 0 veya gizli<br>✅ Tüm bildirimler okunmuş görünümde |

---

### Test Case'ler — Tür Filtreleme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 6 | `shouldFilterNotificationsByType` | Tür filtreleme çalışır | 1. Farklı türlerde bildirim oluştur (BADGE_EARNED, DAILY_REMINDER)<br>2. BADGE_EARNED filtrele | ✅ Sadece badge bildirimleri görünür<br>✅ Diğer türler gizli |

---

### Test Case'ler — Bildirim Ayarları

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 7 | `shouldShowNotificationSettingsPage` | Ayarlar sayfası doğru render edilir | 1. Login yap<br>2. `/settings/notifications` git | ✅ URL `/settings/notifications` içerir<br>✅ Toggle'lar mevcut<br>✅ Varsayılan değerler yüklü |
| 8 | `shouldUpdateNotificationSettings` | Ayarları değiştirir ve kaydeder | 1. Ayarlar sayfasına git<br>2. Email toggle'ı kapat<br>3. Kaydet | ✅ "Bildirim ayarları güncellendi" success mesajı<br>✅ Sayfa yenilendiğinde email toggle hâlâ kapalı |
| 9 | `shouldToggleAllSettingsOffAndOn` | Tüm toggle'ları aç/kapat | 1. Tüm toggle'ları kapat → kaydet<br>2. Tüm toggle'ları aç → kaydet | ✅ Her iki durumda da success mesajı<br>✅ Toggle değerleri doğru persist ediliyor |
| 10 | `shouldPreserveSettingsAfterPageRefresh` | Ayarlar sayfa yenilemesinde korunur | 1. Ayarları değiştir → kaydet<br>2. Sayfayı yenile (F5) | ✅ Yenilemeden sonra toggle'lar son kayıtlı değerlerle |

---

### `ProfileE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/notification/ProfileE2eTest.java`

**Allure Metadata:**
```java
@Epic("Profile")
@Feature("Kullanıcı Profili")
```

---

### Test Case'ler — Profil Sayfası

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 11 | `shouldShowProfilePage` | Profil sayfası doğru render edilir | 1. Login yap<br>2. `/profile` sayfasına git | ✅ URL `/profile` içerir<br>✅ Kullanıcı adı doğru<br>✅ Stats kartları mevcut |
| 12 | `shouldShowZeroStatsForNewUser` | Yeni kullanıcıda tüm istatistikler 0 | 1. Yeni user ile login<br>2. Profil sayfasına git | ✅ Toplam giriş: 0<br>✅ Tamamlanan hedef: 0<br>✅ Streak gün: 0<br>✅ Rozet sayısı: 0 |
| 13 | `shouldShowCorrectStatsForActiveUser` | Aktif kullanıcının istatistikleri doğru | 1. User oluştur, 2 hedef + 5 entry + 1 completed goal (DB helper)<br>2. Login → profil | ✅ Toplam giriş ≥ 5<br>✅ Tamamlanan hedef ≥ 1<br>✅ Değerler mantıklı aralıkta |
| 14 | `shouldShowEarnedBadges` | Kazanılan rozetler görünür | 1. User oluştur, badge kazan (DB helper ile UserBadge oluştur)<br>2. Profil sayfasına git | ✅ Kazanılan rozetler bölümü görünür<br>✅ Rozet adı ve ikonu doğru<br>✅ Kazanılan rozet sayısı stats'la uyumlu |
| 15 | `shouldShowLockedBadges` | Kilitli rozetler görünür | 1. Login → profil | ✅ Kilitli rozetler bölümü görünür<br>✅ Kazanılmamış rozetler farklı stil/opacity ile |

---

## 🛡️ Edge Case'ler

| # | Senaryo | Doğrulama |
|---|---------|-----------|
| 1 | Bildirim yok iken "Tümünü Okundu" butonu | Buton gizli veya devre dışı |
| 2 | Tüm bildirimler zaten okunmuş | "Tümünü Okundu" işlemi idempotent |
| 3 | 20+ bildirim (pagination) | Pagination kontrolleri görünür, sayfa geçişleri çalışır |
| 4 | Checkbox unchecked = null → false | Backend null'u false olarak işler (HTML checkbox davranışı) |
| 5 | Profilde tüm badge'ler kazanılmışsa | "Kilitli rozetler" bölümü boş veya gizli |
| 6 | Profilde hiç badge kazanılmamışsa | "Kazanılan rozetler" bölümü boş, tüm badge'ler kilitli |
| 7 | Stats sayıları çok büyük (1000+ entry) | Sayılar doğru formatlanır |
| 8 | Bildirim türü filtresi uygulandığında sonuç yoksa | Boş liste veya "Bu türde bildirim yok" mesajı |

---

## 🏷️ Allure Etiketleme

```java
// Bildirim testleri
@Epic("Notifications")
@Feature("Bildirim Listesi")
@Story("Okundu İşaretleme")

@Epic("Notifications")
@Feature("Bildirim Ayarları")
@Story("Toggle Güncelleme")

// Profil testleri
@Epic("Profile")
@Feature("İstatistikler")
@Story("Yeni Kullanıcı İstatistikleri")

@Epic("Profile")
@Feature("Rozetler")
@Story("Kazanılan Rozetler")
```

---

## 🔧 Test Data Setup — Bildirim Oluşturma

Bildirim testleri için DB helper metodu gerekir:

```java
// UserRegistrationHelper'a ekleme
Long createNotification(Long userId, NotificationType type, String title, String message, boolean isRead)

// Kullanım
helper.createNotification(user.id(), NotificationType.DAILY_REMINDER,
    "Günlük Hatırlatıcı", "Bugün entry girmeyi unutma!", false);
helper.createNotification(user.id(), NotificationType.BADGE_EARNED,
    "Yeni Rozet! 🎉", "İlk Adım rozetini kazandınız!", false);
helper.createNotification(user.id(), NotificationType.STREAK_DANGER,
    "Streak Tehlikede! ⚠️", "Bugün entry girmezsen streak'in kırılacak!", true); // okunmuş
```

Rozet testleri için:
```java
// Badge ve UserBadge DB helper
Long createUserBadge(Long userId, String badgeCode)

// Kullanım
helper.createUserBadge(user.id(), "FIRST_ENTRY");    // İlk giriş rozeti
helper.createUserBadge(user.id(), "STREAK_7");        // 7 gün streak rozeti
```

---

## ✅ Faz 6 Tamamlama Kriterleri

- [ ] NotificationListPage, NotificationSettingsPage, ProfilePage oluşturuldu
- [ ] 15 test case yazıldı ve başarıyla çalışıyor
- [ ] Bildirim listeleme, okundu işaretleme, filtreleme test edildi
- [ ] Bildirim ayarları toggle güncelleme ve persist doğrulandı
- [ ] Profil istatistikleri doğru değer gösteriyor
- [ ] Kazanılan ve kilitli rozetler doğrulandı
- [ ] Edge case'ler cover edildi
- [ ] Allure etiketleri eklendi
- [ ] Tüm testler birbirinden izole çalışıyor

---

## 📁 Dosya Listesi

```
src/test/java/com/goaltracker/e2e/
    page/notification/
        NotificationListPage.java
        NotificationSettingsPage.java
    page/profile/
        ProfilePage.java
    tests/notification/
        NotificationE2eTest.java
        ProfileE2eTest.java
```

---

## 🎉 Tüm Fazlar Tamamlandığında

Tüm 7 faz (0-6) tamamlandığında:

| Metrik | Hedef |
|--------|-------|
| Toplam Test Sayısı | ~85 E2E test |
| Page Object Sayısı | ~18 page object / component |
| Kapsanan Sayfa | Login, Register, ForgotPassword, ResetPassword, Dashboard, Goal List, Goal Create, Goal Detail, Goal Edit, Social, User Search, Notifications, Notification Settings, Profile |
| Kapsanan Akış | Auth, CRUD Goals, CRUD Entries, Status Updates, Filtering/Searching, Pagination, Export, Friendships, Leaderboard, Activity Feed, Notification Management, Settings, Badge Display |
| Allure Raporu | Epic → Feature → Story hiyerarşisi, failure screenshot, step-by-step raporlama |
| CI Uyumluluğu | Headless Chrome, Maven profil, paralel çalıştırma hazırlığı |

