# 👥 Faz 5 — Sosyal Özellikler E2E Testleri

| Alan | Değer |
|------|-------|
| Süre | 1-1.5 gün |
| Bağımlılık | Faz 0 (altyapı), Faz 1 (login) |
| Hedef | Arkadaşlık yönetimi, lider tablosu, aktivite akışı, kullanıcı arama |
| Test Sayısı | ~14 test case |
| Allure Epic | `Social Features` |
| Özel Not | Birçok test iki kullanıcı gerektirir — biri UI'dan, diğeri DB helper ile setup edilir |

---

## 📁 Oluşturulacak Dosyalar

### Page Object'ler

#### 1. `SocialPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/social/SocialPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Arkadaşlar tab | `a[href*='tab=friends']`, `.nav-link:contains('Arkadaşlar')` | Friends tab linki |
| Lider Tablosu tab | `a[href*='tab=leaderboard']`, `.nav-link:contains('Lider')` | Leaderboard tab |
| Aktivite Akışı tab | `a[href*='tab=activity']`, `.nav-link:contains('Aktivite')` | Feed tab |
| Aktif tab | `.nav-link.active` | Seçili tab |
| Kullanıcı Ara linki | `a[href='/social/search']` | Arama sayfasına link |
| Başarı mesajı | `.alert-success` | Flash success |
| Hata mesajı | `.alert-danger` | Flash error |

**Fluent Metotlar:**
```java
SocialPage selectFriendsTab()
SocialPage selectLeaderboardTab()
SocialPage selectActivityFeedTab()
String getActiveTabName()
UserSearchPage clickSearchUsers()
boolean isOnSocialPage()
String getSuccessMessage()
String getErrorMessage()
boolean isSuccessMessageVisible()
boolean isErrorMessageVisible()

// Delegasyon
FriendsTabComponent friendsTab()
LeaderboardTabComponent leaderboardTab()
ActivityFeedTabComponent activityFeedTab()
```

---

#### 2. `FriendsTabComponent.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/social/FriendsTabComponent.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Username input | `#receiverUsername`, `input[name='receiverUsername']` | Arkadaşlık isteği username |
| Gönder butonu | `.friend-request-form button[type='submit']` | İstek gönder |
| Gelen istekler bölümü | `.incoming-requests` | Gelen arkadaşlık istekleri |
| Giden istekler bölümü | `.outgoing-requests` | Gönderilen istekler |
| Arkadaş listesi | `.friend-list .friend-item` | Mevcut arkadaşlar |
| Kabul butonu | `.btn-accept`, `form[action*='accept'] button` | İsteği kabul et |
| Reddet butonu | `.btn-reject`, `form[action*='reject'] button` | İsteği reddet |
| Kaldır butonu | `.btn-remove`, `form[action*='remove'] button` | Arkadaşı kaldır |
| Boş arkadaş mesajı | `.empty-friends` | "Henüz arkadaşın yok" |

**Fluent Metotlar:**
```java
FriendsTabComponent sendFriendRequest(String username)
FriendsTabComponent acceptRequest(int index)
FriendsTabComponent acceptRequestByUsername(String username)
FriendsTabComponent rejectRequest(int index)
FriendsTabComponent rejectRequestByUsername(String username)
FriendsTabComponent removeFriend(int index)
FriendsTabComponent removeFriendByUsername(String username)

// Okuma
int getFriendCount()
int getIncomingRequestCount()
int getOutgoingRequestCount()
List<String> getFriendUsernames()
List<String> getIncomingRequestUsernames()
List<String> getOutgoingRequestUsernames()
boolean isFriendVisible(String username)
boolean isIncomingRequestVisible(String username)
boolean isFriendsListEmpty()
```

---

#### 3. `LeaderboardTabComponent.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/social/LeaderboardTabComponent.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Kategori filtresi | `select[name='category']` | Kategori dropdown |
| Lider tablosu satırları | `.leaderboard-table tbody tr`, `.leaderboard-entry` | Her kullanıcı satırı |
| Sıra numarası | `td:nth-child(1)` | Sıralama |
| Kullanıcı adı | `td:nth-child(2)` | Username |
| Tamamlama % | `td:nth-child(3)` | Yüzde |
| Boş mesaj | `.empty-leaderboard` | "Lider tablosu boş" |

**Fluent Metotlar:**
```java
LeaderboardTabComponent filterByCategory(String category)
List<String> getLeaderboardUsernames()
int getLeaderboardEntryCount()
String getRank(String username)
boolean isLeaderboardEmpty()
boolean isUserInLeaderboard(String username)
```

---

#### 4. `ActivityFeedTabComponent.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/social/ActivityFeedTabComponent.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Feed item'ları | `.feed-item`, `.activity-item` | Her aktivite satırı |
| Feed mesajı | `.feed-item .message` | Aktivite açıklaması |
| Feed tarihi | `.feed-item .date` | Aktivite tarihi |
| Boş mesaj | `.empty-feed` | "Aktivite yok" |

**Fluent Metotlar:**
```java
int getFeedItemCount()
List<String> getFeedMessages()
boolean isFeedEmpty()
String getFeedItemMessage(int index)
```

---

#### 5. `UserSearchPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/social/UserSearchPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Arama input | `input[name='query']` | Arama kutusu |
| Arama butonu | `button[type='submit']` | Ara butonu |
| Sonuç listesi | `.search-results .user-item` | Kullanıcı sonuçları |
| Kullanıcı adı | `.user-item .username` | Sonuçtaki username |
| Boş sonuç | `.empty-results` | "Sonuç bulunamadı" |

**Fluent Metotlar:**
```java
UserSearchPage search(String query)
List<String> getSearchResultUsernames()
int getSearchResultCount()
boolean isUserVisible(String username)
boolean isEmptyResultVisible()
boolean isOnSearchPage()
```

---

## 🧪 Test Sınıfı

### `SocialE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/social/SocialE2eTest.java`

**Allure Metadata:**
```java
@Epic("Social Features")
@Feature("Sosyal Özellikler")
```

**İki Kullanıcı Stratejisi:**
```java
// UserA: tarayıcıdan login eden kullanıcı
// UserB: DB helper ile oluşturulan karşı taraf
TestUser userA;
TestUser userB;

@BeforeEach
void setUp() {
    userA = helper.createVerifiedUser();
    userB = helper.createVerifiedUser();
}

// UserA login → istek gönder → logout → UserB login → kabul et
private void switchUser(TestUser user) {
    driver.manage().deleteAllCookies();
    loginAs(user.email(), user.password());
}
```

---

### Test Case'ler — Sayfa Görünümü

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 1 | `shouldShowSocialPageWithTabs` | Sosyal sayfası 3 tab ile yüklenir | 1. Login yap<br>2. `/social` sayfasına git | ✅ URL `/social` içerir<br>✅ Arkadaşlar, Lider Tablosu, Aktivite Akışı tab'ları mevcut<br>✅ Varsayılan olarak "Arkadaşlar" tab aktif |
| 2 | `shouldShowEmptyFriendsList` | Yeni kullanıcıda boş arkadaş listesi | 1. Yeni user ile social sayfasına git | ✅ "Henüz arkadaşın yok" veya boş liste<br>✅ Arkadaş sayısı 0 |

---

### Test Case'ler — Arkadaşlık İsteği Gönderme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 3 | `shouldSendFriendRequest` | Arkadaşlık isteği gönderir | 1. UserA ile login<br>2. Social sayfasına git<br>3. UserB'nin username'ini gir<br>4. "İstek Gönder" tıkla | ✅ "Arkadaşlık isteği gönderildi" success mesajı<br>✅ Giden isteklerde UserB görünür |
| 4 | `shouldShowErrorForSelfFriendRequest` | Kendine istek göndermeyi engeller | 1. UserA ile login<br>2. Kendi username'ini gir<br>3. Gönder tıkla | ✅ Hata mesajı görünür<br>✅ İstek oluşturulmaz |
| 5 | `shouldShowErrorForDuplicateFriendRequest` | Aynı kişiye tekrar istek engellenir | 1. UserA → UserB'ye istek gönder (başarılı)<br>2. Aynı UserB'ye tekrar istek gönder | ✅ Hata mesajı ("zaten gönderildi" veya "bekleyen istek var") |
| 6 | `shouldShowErrorForNonexistentUser` | Olmayan kullanıcıya istek → hata | 1. "olmayan_kullanici_xyz" username gir<br>2. Gönder tıkla | ✅ Hata mesajı görünür |

---

### Test Case'ler — Arkadaşlık İsteği Kabul / Reddetme

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 7 | `shouldShowIncomingFriendRequest` | Gelen istek görünür | 1. UserA → UserB'ye istek gönder (DB helper)<br>2. UserB ile login<br>3. Social sayfasına git | ✅ Gelen isteklerde UserA görünür<br>✅ Kabul ve Reddet butonları mevcut |
| 8 | `shouldAcceptFriendRequest` | İsteği kabul eder | 1. UserA → UserB'ye istek gönder (DB helper)<br>2. UserB login → kabul tıkla | ✅ "Arkadaşlık isteği kabul edildi" mesajı<br>✅ UserA arkadaş listesinde görünür<br>✅ Gelen isteklerde UserA artık yok |
| 9 | `shouldRejectFriendRequest` | İsteği reddeder | 1. UserA → UserB'ye istek gönder (DB helper)<br>2. UserB login → reddet tıkla | ✅ "Arkadaşlık isteği reddedildi" mesajı<br>✅ UserA arkadaş listesinde yok<br>✅ Gelen isteklerde UserA artık yok |

---

### Test Case'ler — Arkadaş Kaldırma

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 10 | `shouldRemoveFriend` | Arkadaşı kaldırır | 1. İki kullanıcıyı arkadaş yap (DB helper)<br>2. UserA login → "Kaldır" tıkla | ✅ "Arkadaş kaldırıldı" mesajı<br>✅ UserB arkadaş listesinden silindi |

---

### Test Case'ler — Lider Tablosu

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 11 | `shouldShowLeaderboard` | Lider tablosu görünür | 1. İki arkadaş kullanıcı oluştur, hedef ve entry'ler ekle (DB helper)<br>2. Login → Lider Tablosu tab | ✅ Tablo görünür<br>✅ En az 1 satır mevcut<br>✅ Kullanıcı adları ve yüzdeleri görünür |
| 12 | `shouldFilterLeaderboardByCategory` | Kategori filtresi çalışır | 1. Farklı kategorilerde hedefler oluştur<br>2. Lider tablosu tab → HEALTH filtrele | ✅ Sadece HEALTH kategorisindeki sıralama |

---

### Test Case'ler — Aktivite Akışı & Arama

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 13 | `shouldShowActivityFeed` | Aktivite akışı görünür | 1. İki arkadaş oluştur, entry'ler ekle (DB helper)<br>2. Login → Aktivite Akışı tab | ✅ Feed görünür (veya boş mesaj)<br>✅ Feed item'ları listelenir |
| 14 | `shouldSearchUsersSuccessfully` | Kullanıcı araması çalışır | 1. UserB oluştur (DB helper)<br>2. UserA login → `/social/search?query=UserB_username` | ✅ Arama sayfası yüklenir<br>✅ UserB arama sonuçlarında görünür |
| 15 | `shouldShowEmptySearchResults` | Olmayan kullanıcı araması boş sonuç | 1. "xyz_olmayan_user" ara | ✅ Boş sonuç mesajı veya 0 sonuç |

---

## 🛡️ Edge Case'ler

| # | Senaryo | Doğrulama |
|---|---------|-----------|
| 1 | Arkadaşlık isteği gönder → geri al özelliği var mı? | Giden istekler bölümünde iptal mekanizması test edilir |
| 2 | Bloklanmış kullanıcıya istek | Hata mesajı veya engelleme |
| 3 | Arkadaş olmadan lider tablosu | Sadece kendisi görünür veya boş |
| 4 | Boş aktivite akışı (hiç arkadaş yok) | Boş mesaj gösterilir |
| 5 | Username'de özel karakter | Doğru aranır, XSS engellenir |
| 6 | Çok fazla arkadaş (50+) | Performans sorunu yok, liste doğru render edilir |

---

## 🏷️ Allure Etiketleme

```java
@Epic("Social Features")
@Feature("Arkadaşlık Yönetimi")
@Story("Arkadaşlık İsteği Gönderme")

@Epic("Social Features")
@Feature("Arkadaşlık Yönetimi")
@Story("İsteği Kabul Etme")

@Epic("Social Features")
@Feature("Lider Tablosu")
@Story("Kategori Filtreleme")

@Epic("Social Features")
@Feature("Kullanıcı Arama")
@Story("Başarılı Arama")
```

---

## 🔧 İki Kullanıcı Test Stratejisi

Sosyal testlerde iki kullanıcı gerektiğinde:

**Yöntem 1 — DB Helper ile Arka Plan Setup:**
```java
// Arkadaşlık isteğini DB'den doğrudan oluştur
helper.createFriendshipRequest(userA.id(), userB.id());
// Veya zaten arkadaş yap
helper.createAcceptedFriendship(userA.id(), userB.id());
```

**Yöntem 2 — Cookie Switch (Aynı Tarayıcı):**
```java
// UserA olarak login → aksiyon → cookie temizle → UserB olarak login
loginAs(userA.email(), userA.password());
socialPage.friendsTab().sendFriendRequest(userB.username());
driver.manage().deleteAllCookies();
loginAs(userB.email(), userB.password());
socialPage.friendsTab().acceptRequestByUsername(userA.username());
```

**Önerilen:** Yöntem 1 (DB Helper) — daha hızlı, daha güvenilir, izolasyon daha iyi.

---

## ✅ Faz 5 Tamamlama Kriterleri

- [ ] SocialPage, FriendsTabComponent, LeaderboardTabComponent, ActivityFeedTabComponent, UserSearchPage oluşturuldu
- [ ] 15 test case yazıldı ve başarıyla çalışıyor
- [ ] Arkadaşlık isteği gönderme/kabul/red/kaldırma test edildi
- [ ] Lider tablosu ve kategori filtresi doğrulandı
- [ ] Kullanıcı arama test edildi
- [ ] İki kullanıcı senaryoları başarıyla çalışıyor
- [ ] Edge case'ler cover edildi
- [ ] Allure etiketleri eklendi

---

## 📁 Dosya Listesi

```
src/test/java/com/goaltracker/e2e/
    page/social/
        SocialPage.java
        FriendsTabComponent.java
        LeaderboardTabComponent.java
        ActivityFeedTabComponent.java
        UserSearchPage.java
    tests/social/
        SocialE2eTest.java
```

