# Faz 9 — Polish & Uretim Hazirligii

> **Sure:** 1 hafta | **Bagimlilik:** Faz 0-8 tamamlanmis olmali

---

## Gorev Listesi

### Backend — Unit Testler (JUnit 5 + Mockito) — Hedef yuzde 90 coverage
- [ ] `GoalCalculatorTest` — edge caseler: daysLeft=0, completionPct>100, bos entry, kismi entry
- [ ] `AuthServiceTest` — register, login, refresh, duplicate email, yanlis sifre
- [ ] `GoalServiceTest` — CRUD, ownership, tum status geciisleri
- [ ] `GoalEntryServiceTest` — duplicate entry, out of range, ARCHIVED hedefe entry
- [ ] `StreakServiceTest` — ilk entry, dun, gun atla, idempotent, PAUSED hedef
- [ ] `BadgeServiceTest` — tum badge kosullari, idempotency (ayni rozet 2 kez kazanilmaz)
- [ ] `DashboardServiceTest` — goalsOnTrack/Behind, todayEntryCount, bos hedef listesi
- [ ] `ExportServiceTest` — Excel 3 sayfa, PDF, CSV UTF-8 BOM, ownership
- [ ] `NotificationServiceTest` — createNotification, markAsRead, settings
- [ ] `FriendshipServiceTest` — kendine istek, duplicate, status gecisleri

### Backend — Entegrasyon Testleri (MockMvc + H2 in-memory) — Hedef yuzde 75 coverage
- [ ] `AuthControllerTest` — tum auth endpointleri
- [ ] `GoalControllerTest` — CRUD + filtre + export
- [ ] `GoalEntryControllerTest` — entry CRUD, 409 conflict
- [ ] Her controller icin: 200/201/204/400/401/403/404/409 senaryolari
- [ ] H2 in-memory (test profili) — TestContainers gerekmez

### Thymeleaf View Testleri (MockMvc + HTML)
- [ ] `AuthControllerMvcTest` — login.html, register.html render
- [ ] `GoalControllerMvcTest` — goals/list.html, goals/detail.html render
- [ ] `DashboardControllerMvcTest` — dashboard/index.html render
- [ ] Form submit → flash mesajı kontrolü
- [ ] Unauthorized → /auth/login redirect kontrolü

### Performans Optimizasyonu — Backend
- [ ] N+1 sorgu taramasi: `show-sql: true` ile tum liste endpointleri logla, JOIN FETCH ekle
- [ ] `EXPLAIN ANALYZE` ile yavas sorgular — `goal_entries (goal_id, entry_date)` composite index
- [ ] Dashboard endpoint tek sorguda mi? dogrula
- [ ] Tum liste endpointleri Pageable kullaniyor mu? kontrol et

### Performans Optimizasyonu — Frontend (Thymeleaf)
- [ ] Thymeleaf template cache aktif mi? (`spring.thymeleaf.cache=true` prod'da)
- [ ] Statik dosyalar (CSS/JS) versiyonlama: `spring.web.resources.chain.strategy.content.enabled=true`
- [ ] Bootstrap CDN yerine local static dosya kullan (prod için)
- [ ] Kritik CSS inline ile sayfa yüklenme hızı artırılabilir

### Guvenlik Audit (OWASP Top 10)
- [ ] **A01** — Her endpointte ownership kontrolu var mi? Manuel test ile dogrula
- [ ] **A01** — URL parametresi manipulasyonu (baska user ID'si) testle
- [ ] **A02** — BCrypt strength = 12, JWT secret >= 32 karakter
- [ ] **A02** — application.yml'de hardcoded secret yok
- [ ] **A03** — Tum SQL JPA parameterized queries (@Query string concat yok)
- [ ] **A05** — CORS whitelist, show-sql: false prod, stack trace response'da yok
- [ ] **A05** — CSRF koruması Thymeleaf form'larında aktif (`_csrf` token)
- [ ] **A07** — Rate limiting aktif (/api/auth/**), refresh token tek kullanimlik
- [ ] **A09** — Basarisiz giris ve 403 loglaniyor, sifre/token loglara yazilmiyor

### UX Polish (Thymeleaf)
- [ ] Tum 5 ana sayfa: loading spinner + error mesaji + empty state mevcut
- [ ] `templates/error/404.html` — "Ana Sayfaya Don" butonu
- [ ] `templates/error/500.html` — genel hata sayfasi, "Yenile" butonu
- [ ] Tum CRUD aksiyonlari: basarili → Flash mesajı (yeşil), hata → Flash mesajı (kırmızı) (Türkçe)
- [ ] Form submit sirasinda: buton disabled + "Kaydediliyor..." metin (minimal JS)

### Production Hazirligii
- [ ] `application-prod.yml`: show-sql: false, ddl-auto: validate, logging.level.com.goaltracker: WARN
- [ ] `spring.thymeleaf.cache=true` (prod profili)
- [ ] `spring.h2.console.enabled=false` (prod profili)
- [ ] Actuator: sadece /health ve /info expose ediliyor
- [ ] `mvn clean package` basarili — tum testler geciyor
- [ ] `README.md` tamamlandi: kurulum adimlari, env degiskenleri, test komutu
- [ ] `.env.example` tum zorunlu degiskenleri aciklamalarla listeli
- [ ] GitHub Actions CI pipeline main branchte yesil
- [ ] main branch korumasi aktif (PR zorunlu, review zorunlu)

---

## Dosyalar

### Backend
```
src/test/java/com/goaltracker/
  util/GoalCalculatorTest.java
  service/AuthServiceTest.java
  service/GoalServiceTest.java
  service/GoalEntryServiceTest.java
  service/StreakServiceTest.java
  service/BadgeServiceTest.java
  service/DashboardServiceTest.java
  service/ExportServiceTest.java
  service/NotificationServiceTest.java
  service/FriendshipServiceTest.java
  controller/AuthControllerTest.java
  controller/AuthControllerMvcTest.java
  controller/GoalControllerTest.java
  controller/GoalControllerMvcTest.java
  controller/GoalEntryControllerTest.java
src/main/resources/application-prod.yml  (tamamla)
```

### Thymeleaf Templates
```
src/main/resources/templates/error/
  404.html    (Ana Sayfaya Don butonu)
  500.html    (Yenile butonu)
```

---

## Test Stratejisi

### Backend Coverage

| Katman | Arac | Hedef | Oncelik |
|--------|------|-------|---------|
| GoalCalculator (util) | JUnit 5 | %100 | Kritik |
| Service siniflar | JUnit 5 + Mockito | %90 | Yuksek |
| Controller (entegrasyon) | MockMvc | %75 | Orta |
| Repository | JPA Test | %60 | Dusuk |

### Playwright E2E Test Izolasyon Paterni
```typescript
test.beforeEach(async ({ page }) => {
  await seedTestUser();  // test kullanicisi olustur
  await page.goto('/login');
  await login(page, testUser.email, testUser.password);
});
test.afterEach(async () => {
  await cleanupTestData();  // test verilerini temizle
});
```

### Frontend Test Oncelikleri
1. goalCalculator utility — is kurallari (en kritik)
2. authStore + apiClient interceptor — guvenlik akisi
3. PrivateRoute — yetkilendirme
4. GoalCard + StatsPanel — en cok kullanilan bilesenleri

---

## Son Kontrol Listesi

### Backend
- Her service metodunda ownership kontrolu var mi?
- BigDecimal kullaniliyor (double/float yok)?
- @Transactional eksik metodlar var mi?
- Tum enumlar @Enumerated(EnumType.STRING)?
- EAGER fetch yok (N+1 sorunu)?
- GlobalExceptionHandler stack trace dondurmuyor mu (prod)?
- application.yml'de hardcoded secret yok mu?
- findById().get() yok mu? (orElseThrow kullan)
- Sifre hashi response'a siziyor mu?

### Frontend (Thymeleaf)
- any tipi kullanilan yer var mi? (Java DTO'lar tip-güvenli mi?)
- Thymeleaf expression null check yapiliyor mu? (`th:if="${obj != null}"`)
- ErrorBoundary: Spring `error/404.html` ve `error/500.html` var mi?
- Empty state tum sayfalarda var mi?
- Tum UI metinleri Turkce mi?
- H2 console prod'da kapali mi? (`spring.h2.console.enabled=false`)
- Thymeleaf cache prod'da aktif mi? (`spring.thymeleaf.cache=true`)

---

## Kabul Kriterleri

### Test Coverage
- [ ] Backend service: %90 coverage (mvn verify jacoco:report ile dogrula)
- [ ] Backend controller: %75 coverage
- [ ] Thymeleaf MVC test: %70 coverage
- [ ] mvn test — sifir failure

### Performans
- [ ] Dashboard endpoint < 200ms (local)
- [ ] N+1 sorgu yok (Hibernate log analizi tamamlandi)
- [ ] Thymeleaf template cache aktif (prod profili)
- [ ] Lighthouse >= 85 (desktop)
- [ ] 365 gunluk chart < 1s render

### Guvenlik
- [ ] BCrypt strength = 12 dogrulandi
- [ ] JWT secret >= 32 karakter random (.env, kaynak kodda degil)
- [ ] 11 ownership-critical endpoint baska kullanici tokeniyla test edildi -> 403
- [ ] Rate limiting aktif (11. istek bloke edildi)
- [ ] Stack trace response'da gorunmuyor
- [ ] show-sql: false application-prod.yml'de
- [ ] H2 console prod'da kapali
- [ ] Hicbir secret kaynak koduna commit edilmemis

### UX
- [ ] 5 ana sayfa loading + error + empty state
- [ ] 400/403/404/500 Turkce mesajlarla
- [ ] 375px / 768px / 1280px layout bozulmuyor
- [ ] 404.html calisiyor
- [ ] 500.html calisiyor
- [ ] Flash mesaj sistemi tum CRUD aksiyonlarda calisiyor

### Production
- [ ] mvn clean package basarili, testler geciyor
- [ ] application-prod.yml tamamlanmis
- [ ] README.md fresh clonedan `mvn spring-boot:run` ile calistirilabiliyor
- [ ] .env.example tum degiskenleri listeli
- [ ] CI pipeline yesil
- [ ] main branch korumasi aktif

