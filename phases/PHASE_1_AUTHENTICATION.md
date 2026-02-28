# 🔐 Faz 1 — Kimlik Doğrulama (Authentication)

| Field | Value |
|-------|-------|
| Version | 2.0.0 |
| Status | Enhanced — Architect Review Complete |
| Document Owner | Senior Architect |
| Last Updated | 2026-02-28 |
| Estimated Duration | 1 hafta (5 iş günü) |
| Dependency | Faz 0 (proje iskeleti hazır olmalı, `GlobalExceptionHandler` ve `ApiResponse<T>` mevcut olmalı) |
| Hedef | Kullanıcı kayıt, giriş, JWT tabanlı kimlik doğrulama, e-posta doğrulama, şifre sıfırlama, korunan route yapısı, hesap güvenlik mekanizmaları. |

---

## 📝 Changes Made

| # | Change | Reason |
|---|--------|--------|
| 1 | `## SQL DDL — Flyway Migrations` bölümü eklendi (users, refresh_tokens, email_verification_tokens, password_reset_tokens) | Geliştiricilerin DDL'siz çalışmasını engellemek; şema belirsizliğini gidermek |
| 2 | `## TypeScript Interfaces` bölümü eklendi (10+ interface) | Frontend geliştiricilerin tip güvenliğiyle çalışabilmesi |
| 3 | `## JSON Request/Response Examples` bölümü eklendi (tüm endpointler) | API kullanıcılarına sıfır belirsizlik sağlamak |
| 4 | `## Sequence Diagrams` bölümü eklendi (Register, Login, Token Refresh, Password Reset) | Akış mantığını görselleştirme |
| 5 | `## Security Considerations` bölümü eklendi (account lockout, brute-force koruması, security headers) | Production-grade güvenlik gereksinimleri |
| 6 | `## Error Response Format` bölümü eklendi (standart hata formatı + tüm error code'lar) | Tutarlı hata işleme standardı |
| 7 | `## Testing Strategy` bölümü eklendi (unit, integration, E2E) | Test kapsamını belgelemek |
| 8 | `## Risk Assessment` bölümü eklendi | Proaktif risk yönetimi |
| 9 | `RefreshToken`, `EmailVerificationToken`, `PasswordResetToken` entity'leri eklendi | Refresh token DB storage, token yönetimi |
| 10 | Account lockout mekanizması eklendi (5 başarısız giriş → 15dk kilitlenme) | Brute-force koruması |
| 11 | Audit logging eklendi (login başarılı/başarısız) | Güvenlik denetimi |
| 12 | OpenAPI/Swagger annotation'ları notu eklendi | API dokümantasyonu standartı |
| 13 | `## Acceptance Criteria` bölümü eklendi | Kalite güvencesi |
| 14 | Refresh token rotation zorunlu yapıldı | Token çalınma koruması |
| 15 | CORS konfigürasyon detayı eklendi | Güvenli cross-origin politikası |

---

## 📋 Görev Listesi

### Backend — Entity & Repository

- [ ] `User.java` — Entity:
  - `@Entity`, `@Table(name = "users")`
  - Alanlar: `id`, `email`, `username`, `passwordHash`, `displayName`, `avatarUrl`, `timezone`, `role` (enum), `isActive`, `emailVerified`, `failedLoginCount`, `lockedUntil`, `createdAt`, `updatedAt`
  - `@Enumerated(EnumType.STRING)` — `role` alanı
  - `@Column(nullable = false, unique = true)` — email ve username
  - `@CreationTimestamp` / `@UpdateTimestamp`
  - `@JsonIgnore` — `passwordHash`, `failedLoginCount`, `lockedUntil` alanları DTO'ya sızmaz
- [ ] `Role.java` enum — `USER`, `ADMIN`
- [ ] `RefreshToken.java` — Entity:
  - `@Entity`, `@Table(name = "refresh_tokens")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `User`
  - Alanlar: `id`, `tokenHash`, `expiresAt`, `revoked`, `revokedAt`, `createdAt`
  - Token DB'de **hash** olarak saklanır (SHA-256), plaintext asla DB'ye yazılmaz
- [ ] `EmailVerificationToken.java` — Entity:
  - `@Entity`, `@Table(name = "email_verification_tokens")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `User`
  - Alanlar: `id`, `tokenHash`, `expiresAt`, `usedAt`, `createdAt`
- [ ] `PasswordResetToken.java` — Entity:
  - `@Entity`, `@Table(name = "password_reset_tokens")`
  - `@ManyToOne(fetch = FetchType.LAZY)` → `User`
  - Alanlar: `id`, `tokenHash`, `expiresAt`, `usedAt`, `createdAt`
- [ ] `UserRepository.java`:
  - `Optional<User> findByEmail(String email)`
  - `Optional<User> findByUsername(String username)`
  - `boolean existsByEmail(String email)`
  - `boolean existsByUsername(String username)`
  - `@Modifying @Query("UPDATE User u SET u.failedLoginCount = 0, u.lockedUntil = null WHERE u.id = :userId") void resetLoginAttempts(Long userId)`
  - `@Modifying @Query("UPDATE User u SET u.failedLoginCount = u.failedLoginCount + 1 WHERE u.id = :userId") void incrementFailedLoginAttempts(Long userId)`
- [ ] `RefreshTokenRepository.java`:
  - `Optional<RefreshToken> findByTokenHash(String tokenHash)`
  - `@Modifying @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = NOW() WHERE rt.user.id = :userId") void revokeAllByUserId(Long userId)`
  - `void deleteByExpiresAtBefore(Instant expiry)` — expired token temizliği
- [ ] `EmailVerificationTokenRepository.java`:
  - `Optional<EmailVerificationToken> findByTokenHashAndUsedAtIsNull(String tokenHash)`
- [ ] `PasswordResetTokenRepository.java`:
  - `Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash)`

### Backend — Security Altyapısı

- [ ] `JwtConfig.java` — `@ConfigurationProperties(prefix = "jwt")`:
  ```java
  @ConfigurationProperties(prefix = "jwt")
  public record JwtConfig(
      String secret,           // min 256-bit (32 byte) HS256 key
      long accessExpiration,   // 900000 (15 dakika, milisaniye)
      long refreshExpiration   // 604800000 (7 gün, milisaniye)
  ) {}
  ```
- [ ] `JwtService.java`:
  - `generateAccessToken(UserDetails user)` → 15 dk ömürlü
  - `generateRefreshToken(String email)` → 7 gün ömürlü, UUID-based random token (JWT değil)
  - `extractEmail(String token)` → claim'den email çıkar
  - `extractAllClaims(String token)` → tüm claim'ler
  - `isTokenValid(String token, UserDetails user)` → imza + expiry kontrolü
  - `isTokenExpired(String token)`
  - `hashToken(String rawToken)` → SHA-256 hash (DB storage için)
- [ ] `JwtAuthFilter.java` — `OncePerRequestFilter`:
  - `Authorization: Bearer <token>` header'ı oku
  - Token geçerliyse `SecurityContextHolder`'a `UsernamePasswordAuthenticationToken` set et
  - Hata durumunda `FilterChain` üzerinden `GlobalExceptionHandler`'a ilet
  - Skip paths: `/api/auth/**` (public endpoints)
- [ ] `UserDetailsServiceImpl.java` — `loadUserByUsername(email)` → `UserDetails`
- [ ] `SecurityConfig.java` — `SecurityFilterChain`:
  - `sessionManagement(STATELESS)` — JWT token tabanlı session yönetimi
  - `cors(corsConfigurationSource())` — aşağıdaki CORS config
  - `csrf(disabled)` — stateless API, CSRF gereksiz
  - **Form Login (Thymeleaf):**
    - `formLogin().loginPage("/auth/login").permitAll()`
    - `logout().logoutUrl("/auth/logout").logoutSuccessUrl("/auth/login?logout")`
  - `authorizeHttpRequests`:
    - `/auth/**` → `permitAll()`
    - `/actuator/health` → `permitAll()`
    - `/h2-console/**` → `permitAll()` (dev ortamı)
    - `/v3/api-docs/**`, `/swagger-ui/**` → `permitAll()`
    - Diğerleri → `authenticated()`
  - `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`
  - `passwordEncoder()` → `BCryptPasswordEncoder(12)` bean
  - Security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN` (H2 Console için), `X-XSS-Protection: 0`
  - `@EnableMethodSecurity(prePostEnabled = true)` — method-level güvenlik

#### CORS Configuration
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173")); // dev
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
    config.setExposedHeaders(List.of("X-Total-Count"));
    config.setAllowCredentials(true); // cookie gönderimi için zorunlu
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

> **[DECISION REQUIRED: Production'da `allowedOrigins` environment variable'dan mı okunacak yoksa `application.yml` property'sinden mi?]**

### Backend — Auth İş Mantığı

- [ ] `AuthService.java`:
  - `register(RegisterRequest)`:
    - Email ve username unique kontrolü → `EmailAlreadyExistsException`, `UsernameAlreadyExistsException`
    - BCrypt ile şifre hash'leme (strength: 12)
    - `User` kaydı (emailVerified = false, isActive = true)
    - `EmailVerificationToken` oluştur → UUID + SHA-256 hash → DB kaydet
    - E-posta doğrulama maili gönder (Spring Mail, async)
    - Access token + Refresh token üret ve dön
    - Refresh token SHA-256 hash'i → `refresh_tokens` tablosuna kaydet
    - Refresh token plaintext'i → `HttpOnly` cookie olarak set et
  - `login(LoginRequest)`:
    - Email'e göre user bul → `UserNotFoundException`
    - Account lockout kontrolü: `lockedUntil != null && lockedUntil.isAfter(now)` → `AccountLockedException`
    - `isActive` kontrolü → `AccountDisabledException`
    - BCrypt şifre karşılaştırması:
      - **Başarısız:** `failedLoginCount++`, 5. başarısız denemede `lockedUntil = now + 15 dakika` → `InvalidCredentialsException`
      - **Başarılı:** `failedLoginCount = 0`, `lockedUntil = null` sıfırla
    - **Audit log:** Login başarılı/başarısız → `log.info("Login success/failure for email={}", email)`
    - Token çifti üret ve dön
  - `refreshToken(HttpServletRequest request)`:
    - Refresh token cookie'den oku
    - SHA-256 hash → DB'den bul
    - Token expired mı? Revoked mı? → `InvalidTokenException`
    - **Refresh Token Rotation:** Eski token'ı revoke et → Yeni refresh token üret → yeni cookie set et
    - Yeni access token üret
    - **Token Çalınma Tespiti:** Eğer revoke edilmiş bir token tekrar kullanılırsa → o kullanıcının TÜM refresh token'larını revoke et (tüm session'lar sonlandırılır)
  - `logout(HttpServletRequest request)`:
    - Refresh token cookie'den oku → SHA-256 hash → DB'de revoke et
    - Response'ta cookie'yi temizle (`Max-Age=0`)
  - `verifyEmail(String token)`:
    - Token SHA-256 hash → DB'den bul
    - Expired mı? → `TokenExpiredException`
    - Kullanılmış mı? → `InvalidTokenException`
    - `user.emailVerified = true` yap, `usedAt = now` yaz
  - `forgotPassword(String email)`:
    - Email'e göre user bul
    - **Email yoksa da 200 dön!** (email enumeration koruması)
    - Email bulunursa: `PasswordResetToken` oluştur → UUID + SHA-256 hash → DB kaydet → mail gönder
    - Log: `log.info("Password reset requested for email={}", email)` (bulunsun/bulunmasın)
  - `resetPassword(String token, String newPassword)`:
    - Token SHA-256 hash → DB'den bul
    - Expired mı? → `TokenExpiredException`
    - Kullanılmış mı? → `InvalidTokenException`
    - Şifreyi BCrypt ile hash'le → güncelle
    - Token'ı kullanılmış olarak işaretle (`usedAt = now`)
    - Kullanıcının tüm refresh token'larını revoke et (güvenlik — tüm session'lar sonlanır)
- [ ] `MailService.java`:
  - `sendVerificationEmail(String to, String token)` — Async (`@Async`)
  - `sendPasswordResetEmail(String to, String token)` — Async
  - HTML şablon desteği (Thymeleaf template)
  - Retry mekanizması: `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))`
  - Hata durumunda: mail gönderim başarısızlığı kullanıcı flow'unu bloklamamalı → log + metric
- [ ] Rate Limiting (Bucket4j):
  - `/api/auth/login` → 10 istek/dakika/IP
  - `/api/auth/register` → 5 istek/dakika/IP
  - `/api/auth/forgot-password` → 3 istek/dakika/IP
  - `RateLimitFilter.java` veya `@RateLimiter` AOP yaklaşımı
  - 429 Too Many Requests response formatı (aşağıda)

### Backend — User Profil

- [ ] `UserController.java`:
  - `GET /api/users/me` → Kimlik doğrulanmış kullanıcının profili
  - `PUT /api/users/me` → Profil güncelleme (displayName, timezone, avatarUrl)
  - `PUT /api/users/me/password` → Şifre değiştirme (currentPassword doğrulama + newPassword)
  - `DELETE /api/users/me` → Soft delete (`is_active = false`)
  - `GET /api/users/search?q={username}` → Kullanıcı arama (Faz 7 için hazırlık)
  - Her endpoint: `@AuthenticationPrincipal` ile currentUserId al
  - OpenAPI annotations: `@Operation(summary = "...")`, `@ApiResponse(...)`
- [ ] `UserService.java` — profil iş mantığı, soft delete, password change
- [ ] `UserMapper.java` — `User` → `UserResponse` dönüşümü (MapStruct veya manual)

### Backend — DTO'lar

- [ ] `RegisterRequest.java`:
  ```java
  public record RegisterRequest(
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9_]+$") String username,
      @NotBlank @Size(min = 8, max = 128) @StrongPassword String password,
      @Size(max = 100) String displayName
  ) {}
  ```
- [ ] `LoginRequest.java`:
  ```java
  public record LoginRequest(
      @NotBlank @Email String email,
      @NotBlank String password
  ) {}
  ```
- [ ] `AuthResponse.java`:
  ```java
  public record AuthResponse(
      String accessToken,
      String tokenType,  // always "Bearer"
      UserResponse user
  ) {}
  ```
- [ ] `UserResponse.java`:
  ```java
  public record UserResponse(
      Long id,
      String email,
      String username,
      String displayName,
      String avatarUrl,
      String timezone,
      String role,
      boolean emailVerified,
      @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Instant createdAt
  ) {}
  ```
- [ ] `UpdateProfileRequest.java` (`displayName`, `timezone`, `avatarUrl`)
- [ ] `ForgotPasswordRequest.java` (`@NotBlank @Email email`)
- [ ] `ResetPasswordRequest.java` (`@NotBlank token`, `@NotBlank @Size(min=8) @StrongPassword newPassword`)
- [ ] `ChangePasswordRequest.java` (`@NotBlank currentPassword`, `@NotBlank @Size(min=8) @StrongPassword newPassword`)

### Backend — Custom Validator

- [ ] `@StrongPassword` — Custom constraint annotation:
  ```java
  // En az 1 büyük harf, 1 küçük harf, 1 rakam, min 8 karakter
  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")
  ```
- [ ] `StrongPasswordValidator.java` — ConstraintValidator implementasyonu

### Backend — Exception Sınıfları

- [ ] `EmailAlreadyExistsException.java` → 409 Conflict
- [ ] `UsernameAlreadyExistsException.java` → 409 Conflict
- [ ] `InvalidCredentialsException.java` → 401 Unauthorized
- [ ] `AccountDisabledException.java` → 403 Forbidden
- [ ] `AccountLockedException.java` → 423 Locked
- [ ] `InvalidTokenException.java` → 400 Bad Request (doğrulama/sıfırlama tokeni geçersiz)
- [ ] `TokenExpiredException.java` → 400 Bad Request
- [ ] `RateLimitExceededException.java` → 429 Too Many Requests

### Backend — Scheduled Tasks

- [ ] `TokenCleanupScheduler.java` — `@Scheduled(cron = "0 0 3 * * *")`:
  - Expired refresh token'ları sil
  - Kullanılmış email verification token'ları sil (24+ saat)
  - Kullanılmış password reset token'ları sil (1+ saat)

### Frontend

- [ ] `LoginPage.tsx`:
  - React Hook Form + Zod schema (email validasyonu, şifre min 8 karakter)
  - E-posta / şifre alanları, "Beni hatırla" checkbox
  - Hata mesajları (yanlış şifre, hesap bulunamadı, hesap kilitli)
  - "Şifremi Unuttum" linki
  - Başarılı → Dashboard'a yönlendirme
  - Loading state: buton disabled + spinner
- [ ] `RegisterPage.tsx`:
  - Zod schema (email, username min 3/max 30/alphanumeric+underscore, password min 8/büyük+küçük+rakam, displayName)
  - Şifre tekrar doğrulama alanı
  - Şifre gücü göstergesi (weak/medium/strong)
  - Kayıt sonrası "E-posta doğrulama gönderildi" mesajı
- [ ] `ForgotPasswordPage.tsx` — Email form, gönderim sonrası bilgilendirme
- [ ] `ResetPasswordPage.tsx` — URL'den token al, yeni şifre form, başarı/hata gösterimi
- [ ] `VerifyEmailPage.tsx` — URL'den token al, backend'e doğrula, sonuç göster (başarı/expired/invalid)
- [ ] `authService.ts`:
  - `login(email, password)` → `AuthResponse`
  - `register(data)` → `AuthResponse`
  - `refreshToken()` → `{ accessToken }`
  - `logout()` → void
  - `forgotPassword(email)` → void
  - `resetPassword(token, newPassword)` → void
  - `verifyEmail(token)` → void
- [ ] `authStore.ts` — Zustand:
  - `state: { user: UserResponse | null, accessToken: string | null, isAuthenticated: boolean, isLoading: boolean }`
  - `actions: { login, logout, setAccessToken, setUser, setLoading }`
  - Access token **memory'de** tutulur (localStorage değil!)
  - `persist` middleware **KULLANILMAZ** — accessToken bellekte, user bilgisi refresh ile yüklenir
- [ ] `apiClient.ts` güncelleme:
  - Request interceptor: `authStore.getState().accessToken` → `Authorization: Bearer` header
  - Response interceptor: 401 gelirse → `authService.refreshToken()` → yeni token set → orijinal isteği tekrar gönder
  - **Concurrent 401 handling:** Birden fazla istek aynı anda 401 alırsa, sadece 1 refresh isteği gönder, diğerleri beklesin
  - Refresh da 401 verirse → `authStore.logout()` → `/login`'e yönlendir
- [ ] `PrivateRoute.tsx` — `isAuthenticated` false ise `/login`'e redirect
- [ ] `PublicRoute.tsx` — `isAuthenticated` true ise `/dashboard`'a redirect (login/register sayfaları için)
- [ ] `useAuth.ts` hook — `authStore`'u sarmalayan, `user`, `isAuthenticated`, `login`, `logout` expose eden hook
- [ ] `router.tsx` güncelleme:
  - Public routes: `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email`
  - Private routes: `PrivateRoute` ile sarılı tüm uygulama sayfaları

---

## 🗃️ SQL DDL — Flyway Migrations

### V1__create_users.sql
```sql
CREATE TABLE users (
    id                  BIGSERIAL       PRIMARY KEY,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    username            VARCHAR(30)     NOT NULL UNIQUE,
    password_hash       VARCHAR(255)    NOT NULL,
    display_name        VARCHAR(100),
    avatar_url          VARCHAR(500),
    timezone            VARCHAR(50)     NOT NULL DEFAULT 'Europe/Istanbul',
    role                VARCHAR(20)     NOT NULL DEFAULT 'USER'
                                        CHECK (role IN ('USER', 'ADMIN')),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_count  INT             NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_is_active ON users (is_active) WHERE is_active = TRUE;

COMMENT ON TABLE users IS 'Kullanıcı hesapları';
COMMENT ON COLUMN users.password_hash IS 'BCrypt(12) hash — asla plaintext saklanmaz';
COMMENT ON COLUMN users.failed_login_count IS 'Brute-force koruması: 5 başarısız → 15dk kilit';
COMMENT ON COLUMN users.locked_until IS 'Hesap kilitlenme bitiş zamanı — null ise kilitli değil';
```

### V1_1__create_refresh_tokens.sql
```sql
CREATE TABLE refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)     NOT NULL UNIQUE,  -- SHA-256 hex (64 char)
    expires_at  TIMESTAMPTZ     NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash) WHERE revoked = FALSE;
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh token storage — token SHA-256 hash olarak saklanır';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Plaintext token asla DB''ye yazılmaz';
```

### V1_2__create_email_verification_tokens.sql
```sql
CREATE TABLE email_verification_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)     NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ     NOT NULL,          -- 24 saat geçerli
    used_at     TIMESTAMPTZ,                       -- null ise henüz kullanılmamış
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verif_tokens_user ON email_verification_tokens (user_id);
CREATE INDEX idx_email_verif_tokens_hash ON email_verification_tokens (token_hash) WHERE used_at IS NULL;

COMMENT ON TABLE email_verification_tokens IS 'E-posta doğrulama tokenleri — 24 saat ömürlü, tek kullanımlık';
```

### V1_3__create_password_reset_tokens.sql
```sql
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)     NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ     NOT NULL,          -- 1 saat geçerli
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pwd_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX idx_pwd_reset_tokens_hash ON password_reset_tokens (token_hash) WHERE used_at IS NULL;

COMMENT ON TABLE password_reset_tokens IS 'Şifre sıfırlama tokenleri — 1 saat ömürlü, tek kullanımlık';
```

---

## 📐 Thymeleaf Model Attributes

Thymeleaf SSR kullandığımız için TypeScript interface yerine Thymeleaf template'lerinde kullanılan Java model nesneleri:

| Model Attribute | Java Tipi | Template'de Kullanım |
|-----------------|-----------|---------------------|
| `user` | `UserResponse` | `th:text="${user.displayName}"` |
| `registerForm` | `RegisterRequest` | `th:object="${registerForm}"` |
| `loginForm` | `LoginRequest` | `th:object="${loginForm}"` |
| `errorMessage` | `String` | `th:if="${errorMessage}"` |
| `successMessage` | `String` | `th:if="${successMessage}"` |

### Örnek Form Binding (register.html)
```html
<form th:action="@{/auth/register}" th:object="${registerForm}" method="post">
    <div class="mb-3">
        <label for="email" class="form-label">E-posta</label>
        <input type="email" th:field="*{email}" class="form-control"
               th:classappend="${#fields.hasErrors('email')} ? 'is-invalid'">
        <div class="invalid-feedback" th:errors="*{email}">E-posta hatası</div>
    </div>
    <div class="mb-3">
        <label for="password" class="form-label">Şifre</label>
        <input type="password" th:field="*{password}" class="form-control"
               th:classappend="${#fields.hasErrors('password')} ? 'is-invalid'">
        <div class="invalid-feedback" th:errors="*{password}">Şifre hatası</div>
    </div>
    <button type="submit" class="btn btn-primary">Kayıt Ol</button>
</form>
```

---

## 🔌 API Endpoint'leri (REST — JWT Token İçin)

> **Not:** Thymeleaf form-based akış için Spring MVC `AuthController` kullanılır (POST form submit).
> Aşağıdaki REST endpoint'ler mobil istemci veya harici API erişimi için tutulmuştur.

### POST `/api/auth/register`
```
Body: { email, username, password, displayName }
→ 201 ApiResponse<AuthResponse>   (access token + user)
Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

### POST `/api/auth/login`
```
Body: { email, password }
→ 200 ApiResponse<AuthResponse>
Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

### POST `/api/auth/refresh`
```
Cookie: refreshToken
→ 200 ApiResponse<{ accessToken: string }>
Set-Cookie: refreshToken=<NEW_TOKEN>; ...  (rotation — yeni token)
```

### POST `/api/auth/logout`
```
Cookie: refreshToken
→ 200 ApiResponse<null>
Set-Cookie: refreshToken=; Max-Age=0  (cookie temizle)
```

### POST `/api/auth/verify-email`
```
Body: { token }
→ 200 ApiResponse<null>
```

### POST `/api/auth/forgot-password`
```
Body: { email }
→ 200 ApiResponse<null>  (email yoksa da 200!)
```

### POST `/api/auth/reset-password`
```
Body: { token, newPassword }
→ 200 ApiResponse<null>
```

### GET `/api/users/me`
```
Authorization: Bearer <accessToken>
→ 200 ApiResponse<UserResponse>
```

### PUT `/api/users/me`
```
Authorization: Bearer <accessToken>
Body: { displayName, timezone, avatarUrl }
→ 200 ApiResponse<UserResponse>
```

### PUT `/api/users/me/password`
```
Authorization: Bearer <accessToken>
Body: { currentPassword, newPassword }
→ 200 ApiResponse<null>
```

### DELETE `/api/users/me`
```
Authorization: Bearer <accessToken>
→ 200 ApiResponse<null>  (soft delete)
```

### GET `/api/users/search?q=username`
```
Authorization: Bearer <accessToken>
→ 200 ApiResponse<List<UserResponse>>  (Faz 7 için hazırlık)
```

---

## 📦 JSON Request/Response Examples

### Register — Success (201)
```json
// POST /api/auth/register
// Request:
{
  "email": "ali@example.com",
  "username": "alidev",
  "password": "MyStr0ngPass",
  "displayName": "Ali Yılmaz"
}

// Response (201):
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "email": "ali@example.com",
      "username": "alidev",
      "displayName": "Ali Yılmaz",
      "avatarUrl": null,
      "timezone": "Europe/Istanbul",
      "role": "USER",
      "emailVerified": false,
      "createdAt": "2026-02-28T10:30:00Z"
    }
  },
  "message": "Kayıt başarılı. E-posta doğrulama linki gönderildi.",
  "errorCode": null,
  "timestamp": "2026-02-28T10:30:00Z"
}
// Set-Cookie: refreshToken=abc123...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

### Register — Email Already Exists (409)
```json
{
  "success": false,
  "data": null,
  "message": "Bu e-posta adresi zaten kayıtlı.",
  "errorCode": "EMAIL_ALREADY_EXISTS",
  "timestamp": "2026-02-28T10:30:00Z"
}
```

### Register — Validation Error (400)
```json
{
  "success": false,
  "data": null,
  "message": "Doğrulama hatası.",
  "errorCode": "VALIDATION_ERROR",
  "fieldErrors": [
    { "field": "password", "message": "Şifre en az 8 karakter, 1 büyük harf, 1 küçük harf ve 1 rakam içermelidir.", "rejectedValue": "weak" },
    { "field": "username", "message": "Kullanıcı adı en az 3 karakter olmalıdır.", "rejectedValue": "ab" }
  ],
  "timestamp": "2026-02-28T10:30:00Z"
}
```

### Login — Success (200)
```json
// POST /api/auth/login
// Request:
{
  "email": "ali@example.com",
  "password": "MyStr0ngPass"
}

// Response (200):
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "email": "ali@example.com",
      "username": "alidev",
      "displayName": "Ali Yılmaz",
      "avatarUrl": null,
      "timezone": "Europe/Istanbul",
      "role": "USER",
      "emailVerified": true,
      "createdAt": "2026-02-28T10:30:00Z"
    }
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-02-28T10:30:01Z"
}
```

### Login — Invalid Credentials (401)
```json
{
  "success": false,
  "data": null,
  "message": "E-posta veya şifre hatalı.",
  "errorCode": "INVALID_CREDENTIALS",
  "timestamp": "2026-02-28T10:30:01Z"
}
```

### Login — Account Locked (423)
```json
{
  "success": false,
  "data": null,
  "message": "Hesabınız çok fazla başarısız deneme nedeniyle kilitlendi. 15 dakika sonra tekrar deneyin.",
  "errorCode": "ACCOUNT_LOCKED",
  "timestamp": "2026-02-28T10:30:01Z"
}
```

### Login — Account Disabled (403)
```json
{
  "success": false,
  "data": null,
  "message": "Hesabınız devre dışı bırakılmıştır.",
  "errorCode": "ACCOUNT_DISABLED",
  "timestamp": "2026-02-28T10:30:01Z"
}
```

### Token Refresh — Success (200)
```json
// POST /api/auth/refresh (Cookie: refreshToken=abc123...)
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.NEW..."
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-02-28T10:45:00Z"
}
// Set-Cookie: refreshToken=NEW_TOKEN; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

### Rate Limit Exceeded (429)
```json
{
  "success": false,
  "data": null,
  "message": "Çok fazla istek gönderdiniz. Lütfen 60 saniye sonra tekrar deneyin.",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2026-02-28T10:30:05Z"
}
// Header: Retry-After: 60
```

### Verify Email — Success (200)
```json
// POST /api/auth/verify-email
// Request: { "token": "abc123-uuid-token" }
{
  "success": true,
  "data": null,
  "message": "E-posta adresiniz başarıyla doğrulandı.",
  "errorCode": null,
  "timestamp": "2026-02-28T11:00:00Z"
}
```

### Verify Email — Token Expired (400)
```json
{
  "success": false,
  "data": null,
  "message": "Doğrulama tokeninin süresi dolmuş. Lütfen yeni bir doğrulama e-postası isteyin.",
  "errorCode": "TOKEN_EXPIRED",
  "timestamp": "2026-02-28T11:00:00Z"
}
```

### Profile — Get (200)
```json
// GET /api/users/me
{
  "success": true,
  "data": {
    "id": 1,
    "email": "ali@example.com",
    "username": "alidev",
    "displayName": "Ali Yılmaz",
    "avatarUrl": "https://cdn.example.com/avatars/ali.jpg",
    "timezone": "Europe/Istanbul",
    "role": "USER",
    "emailVerified": true,
    "createdAt": "2026-02-28T10:30:00Z"
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2026-02-28T12:00:00Z"
}
```

---

## 🔄 Sequence Diagrams

### Register Flow
```
Client                    Backend                     DB                    Mail
  |--- POST /register --->|                           |                      |
  |                        |-- existsByEmail() ------->|                      |
  |                        |<-- false -----------------|                      |
  |                        |-- existsByUsername() ----->|                      |
  |                        |<-- false -----------------|                      |
  |                        |-- BCrypt.hash(password) ->|                      |
  |                        |-- save(User) ------------>|                      |
  |                        |<-- User saved ------------|                      |
  |                        |-- UUID.randomUUID() ----->|                      |
  |                        |-- save(VerifToken) ------>|                      |
  |                        |-- generateAccessToken() ->|                      |
  |                        |-- generateRefreshToken()->|                      |
  |                        |-- save(RefreshToken) ---->|                      |
  |                        |                           |                      |
  |                        |------ @Async sendVerificationEmail() ---------->|
  |                        |                           |                      |
  |<-- 201 AuthResponse ---|                           |                      |
  |    Set-Cookie: refresh |                           |                      |
```

### Login Flow (with Account Lockout)
```
Client                    Backend                     DB
  |--- POST /login ------>|                           |
  |                        |-- findByEmail() --------->|
  |                        |<-- User ------------------|
  |                        |                           |
  |                        |-- lockedUntil > now? ---->|
  |                        |   YES → 423 LOCKED        |
  |                        |   NO  → continue          |
  |                        |                           |
  |                        |-- isActive? ------------->|
  |                        |   NO  → 403 DISABLED      |
  |                        |                           |
  |                        |-- BCrypt.matches()? ----->|
  |                        |   NO:                     |
  |                        |     failedLoginCount++    |
  |                        |     count >= 5?           |
  |                        |       lockedUntil=+15min  |
  |                        |   ← 401 INVALID_CREDS    |
  |                        |                           |
  |                        |   YES:                    |
  |                        |     failedLoginCount=0    |
  |                        |     lockedUntil=null      |
  |                        |-- generateTokens() ----->|
  |                        |-- save(RefreshToken) ---->|
  |<-- 200 AuthResponse ---|                           |
```

### Token Refresh Flow (with Rotation)
```
Client                    Backend                     DB
  |--- POST /refresh ----->|                          |
  |    Cookie: oldRefresh  |                          |
  |                        |-- hash(oldRefresh) ----->|
  |                        |-- findByHash() --------->|
  |                        |<-- RefreshToken ---------|
  |                        |                          |
  |                        |-- revoked? expired? ---->|
  |                        |   IF revoked (reuse):    |
  |                        |     revokeAll(userId)    |
  |                        |     ← 401 (theft detect) |
  |                        |   IF expired:            |
  |                        |     ← 401 INVALID_TOKEN  |
  |                        |                          |
  |                        |-- revoke(oldToken) ----->|
  |                        |-- newRefresh=UUID() ---->|
  |                        |-- save(newRefreshToken)->|
  |                        |-- newAccess=JWT() ------>|
  |                        |                          |
  |<-- 200 {accessToken} --|                          |
  |    Set-Cookie: new     |                          |
```

### Forgot Password → Reset Flow
```
Client                    Backend                     DB                    Mail
  |--- POST /forgot ------>|                          |                      |
  |    {email}             |-- findByEmail() -------->|                      |
  |                        |<-- User (or null) -------|                      |
  |                        |                          |                      |
  |                        |   IF user exists:        |                      |
  |                        |     UUID token --------->|                      |
  |                        |     save(ResetToken) --->|                      |
  |                        |     @Async sendEmail() ----------------------->|
  |                        |                          |                      |
  |<-- 200 (always) -------|  (email yoksa da 200!)   |                      |
  |                        |                          |                      |
  |                        |                          |                      |
  |--- POST /reset ------->|                          |                      |
  |    {token, newPass}    |-- hash(token) ---------->|                      |
  |                        |-- findByHash() --------->|                      |
  |                        |<-- ResetToken -----------|                      |
  |                        |-- expired? used? ------->|                      |
  |                        |-- BCrypt(newPass) ------>|                      |
  |                        |-- updatePassword() ----->|                      |
  |                        |-- markUsed(token) ------>|                      |
  |                        |-- revokeAllRefresh() --->|                      |
  |<-- 200 ---------------|                          |                      |
```

---

## 🛡️ Security Considerations

### Account Lockout
| Parameter | Value |
|-----------|-------|
| Max failed attempts | 5 |
| Lock duration | 15 dakika |
| Lock scope | Hesap bazlı (IP bazlı değil) |
| Reset condition | Başarılı giriş sonrası counter sıfırlanır |
| Admin bypass | Admin panelinden manuel unlock `[DECISION REQUIRED: Faz 9'da mı?]` |

### Rate Limiting
| Endpoint | Limit | Window | Key |
|----------|-------|--------|-----|
| `/api/auth/login` | 10 req | 1 dakika | IP address |
| `/api/auth/register` | 5 req | 1 dakika | IP address |
| `/api/auth/forgot-password` | 3 req | 1 dakika | IP address |
| `/api/auth/refresh` | 20 req | 1 dakika | IP address |

### Security Headers
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 0
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'
Strict-Transport-Security: max-age=31536000; includeSubDomains  (production only)
```

### Token Security
- Access Token: Bellekte (Zustand), localStorage/sessionStorage **ASLA** kullanılmaz
- Refresh Token: `HttpOnly`, `Secure`, `SameSite=Strict` cookie — JavaScript erişemez
- Token hash'leri DB'de saklanır (SHA-256) — DB sızıntısında bile token'lar güvende
- Refresh Token Rotation: Her refresh'te yeni token → eski revoke
- Theft Detection: Revoke edilmiş token kullanılırsa → tüm session'lar sonlandırılır

### Password Security
- BCrypt strength: 12 (hash süresi ~250ms — brute-force koruması)
- Minimum 8 karakter, en az 1 büyük harf, 1 küçük harf, 1 rakam
- `passwordHash` alanı hiçbir response'a dahil edilmez (`@JsonIgnore` + DTO'da yok)
- Şifre sıfırlama sonrası tüm mevcut session'lar sonlandırılır

### Email Enumeration Protection
- `/api/auth/forgot-password` her zaman 200 döner (email bulunsun/bulunmasın)
- Response süresi sabitlenmeli veya yakın olmalı (timing attack koruması)
- Log'da `log.info("Password reset requested")` — email loglanır ama response'ta leak etmez

---

## ❌ Error Response Format

### Standart Error Code Tablosu
| Error Code | HTTP Status | Açıklama |
|------------|-------------|----------|
| `VALIDATION_ERROR` | 400 | Bean validation / Zod hatası |
| `INVALID_TOKEN` | 400 | Geçersiz doğrulama/sıfırlama tokeni |
| `TOKEN_EXPIRED` | 400 | Token süresi dolmuş |
| `INVALID_CREDENTIALS` | 401 | Yanlış e-posta veya şifre |
| `UNAUTHORIZED` | 401 | JWT eksik veya geçersiz |
| `ACCOUNT_DISABLED` | 403 | Hesap devre dışı (soft deleted) |
| `ACCOUNT_LOCKED` | 423 | Çok fazla başarısız giriş denemesi |
| `EMAIL_ALREADY_EXISTS` | 409 | E-posta zaten kayıtlı |
| `USERNAME_ALREADY_EXISTS` | 409 | Kullanıcı adı zaten kayıtlı |
| `RATE_LIMIT_EXCEEDED` | 429 | İstek limiti aşıldı |
| `INTERNAL_ERROR` | 500 | Beklenmeyen sunucu hatası |

---

## 📁 Oluşturulacak / Güncellenecek Dosyalar

### Backend
```
src/main/java/com/goaltracker/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── MailConfig.java
│   └── CorsConfig.java               (veya SecurityConfig içinde)
├── controller/
│   ├── AuthController.java
│   └── UserController.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   └── MailService.java
├── repository/
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── EmailVerificationTokenRepository.java
│   └── PasswordResetTokenRepository.java
├── model/
│   ├── User.java
│   ├── RefreshToken.java
│   ├── EmailVerificationToken.java
│   ├── PasswordResetToken.java
│   └── enums/
│       └── Role.java
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   └── ChangePasswordRequest.java
│   └── response/
│       ├── AuthResponse.java
│       └── UserResponse.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   └── UserDetailsServiceImpl.java
├── mapper/
│   └── UserMapper.java
├── validation/
│   ├── StrongPassword.java            (annotation)
│   └── StrongPasswordValidator.java
├── scheduler/
│   └── TokenCleanupScheduler.java
└── exception/
    ├── EmailAlreadyExistsException.java
    ├── UsernameAlreadyExistsException.java
    ├── InvalidCredentialsException.java
    ├── AccountDisabledException.java
    ├── AccountLockedException.java
    ├── InvalidTokenException.java
    ├── TokenExpiredException.java
    └── RateLimitExceededException.java
```

### Flyway Migrations
```
src/main/resources/db/migration/
├── V1__create_users.sql
├── V1_1__create_refresh_tokens.sql
├── V1_2__create_email_verification_tokens.sql
└── V1_3__create_password_reset_tokens.sql
```

### Thymeleaf Templates
```
src/main/resources/templates/
├── auth/
│   ├── login.html
│   ├── register.html
│   ├── forgot-password.html
│   ├── reset-password.html
│   ├── verify-email.html
│   └── email-verification-sent.html
```

### E-posta Şablonları (Thymeleaf)
```
src/main/resources/templates/email/
├── verification.html          ← E-posta doğrulama maili
└── password-reset.html        ← Şifre sıfırlama maili
```

---

## 💡 İş Kuralları

### JWT Stratejisi
- **Access Token:** 15 dakika ömür, `Authorization: Bearer <token>` header'ında gönderilir, Zustand memory'de tutulur
- **Refresh Token:** 7 gün ömür, `HttpOnly`, `Secure`, `SameSite=Strict` cookie, DB'de SHA-256 hash olarak saklanır
- Access token **asla localStorage/sessionStorage'a yazılmaz**
- 401 alındığında otomatik olarak `/api/auth/refresh` çağrılır, yeni token set edilir, orijinal istek tekrarlanır
- Concurrent 401 handling: birden fazla isteğin aynı anda 401 alması durumunda tek refresh çağrısı yapılır
- Refresh da başarısız olursa kullanıcı logout edilir
- **Refresh Token Rotation zorunludur** — her refresh'te yeni token verilir

### Şifre Kuralları
```
Minimum 8 karakter
En az 1 büyük harf (A-Z)
En az 1 küçük harf (a-z)
En az 1 rakam (0-9)
BCrypt strength: 12
Regex: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$
```

### Token Yönetimi
| Token Tipi | Ömür | Storage | Format |
|------------|------|---------|--------|
| Access Token | 15 dakika | Zustand (memory) | JWT (HS256) |
| Refresh Token | 7 gün | HttpOnly cookie + DB hash | UUID |
| Email Verification | 24 saat | DB (SHA-256 hash) | UUID |
| Password Reset | 1 saat | DB (SHA-256 hash), tek kullanımlık | UUID |

### Account Lockout
```
Başarısız giriş sayısı < 5  → sadece hata mesajı
Başarısız giriş sayısı >= 5 → hesap 15 dakika kilitlenir
Başarılı giriş              → counter sıfırlanır, lock kalkar
Kilitli hesaba giriş denemesi → 423 Locked + kalan süre bilgisi
```

### Refresh Token Rotation (Zorunlu)
- Her refresh isteğinde yeni bir refresh token üretilir
- Eski token geçersiz sayılır (revoked = true)
- Token çalınma tespiti: eski (revoked) token tekrar kullanılırsa tüm session'lar sonlandırılır

---

## ⚠️ Dikkat Edilecek Noktalar

```java
// ❌ Access token'ı cookie'ye koyma — response body'den dön
// ❌ BCrypt strength'i 10'un altında tutma → 12 kullan
// ❌ /forgot-password'da "email bulunamadı" mesajı verme → güvenlik açığı
//    → Her zaman 200 dön, log'a yaz
// ❌ Şifre hash'ini herhangi bir response'a dahil etme
//    → @JsonIgnore veya DTO kullan (passwordHash alanı DTO'da YOK)
// ❌ Token'da userId yerine email kullan — email değişirse?
//    [DECISION REQUIRED: JWT subject email mi userId mi? Email önerilir — değişmez ID daha güvenli ama email daha pratik]
// ❌ Refresh token'ı plaintext DB'ye yazma → SHA-256 hash kullan
// ❌ Revoke edilmiş refresh token tekrar kullanıldığında sessizce reddetme
//    → Tüm user session'larını sonlandır (token theft alarm)
// ❌ Mail gönderim hatalarının kullanıcı flow'unu bloklamasına izin verme
//    → @Async + @Retryable
// ❌ Rate limiting olmadan auth endpoint'leri deploy etme
//    → Bucket4j ile IP bazlı limit zorunlu
```

```typescript
// ❌ accessToken'ı localStorage'da tutma → XSS ile çalınır
//    → Zustand state (memory only), persist middleware KULLANMA
// ❌ Birden fazla 401'de birden fazla refresh isteği göndermek
//    → Queue mekanizması: ilk 401'de refresh, diğerleri bekler
// ❌ Refresh 401 dönerse sonsuz döngüye girmek
//    → Refresh endpoint'i interceptor'dan exclude et
// ❌ Password strength'i sadece frontend'de kontrol etme
//    → Backend'de @StrongPassword custom validator zorunlu
```

---

## 🧪 Testing Strategy

### Backend Unit Tests

#### JwtService Tests
- [ ] `generateAccessToken` → geçerli JWT üretir, email claim içerir
- [ ] `generateAccessToken` → 15 dakika expiry
- [ ] `isTokenValid` → geçerli token + doğru user → true
- [ ] `isTokenValid` → expired token → false
- [ ] `isTokenValid` → farklı user → false
- [ ] `extractEmail` → token'dan email çıkarır
- [ ] `hashToken` → SHA-256 hex döner, 64 karakter

#### AuthService Tests
- [ ] `register` → başarılı kayıt, User + RefreshToken + VerificationToken DB'de
- [ ] `register` → duplicate email → `EmailAlreadyExistsException`
- [ ] `register` → duplicate username → `UsernameAlreadyExistsException`
- [ ] `login` → başarılı giriş → failedLoginCount sıfırlanır
- [ ] `login` → yanlış şifre → failedLoginCount artırılır
- [ ] `login` → 5. başarısız denemede → hesap kilitlenir
- [ ] `login` → kilitli hesap → `AccountLockedException`
- [ ] `login` → devre dışı hesap → `AccountDisabledException`
- [ ] `refreshToken` → geçerli refresh → yeni access token + yeni refresh token (rotation)
- [ ] `refreshToken` → revoked token kullanılırsa → tüm session'lar revoke
- [ ] `refreshToken` → expired token → `InvalidTokenException`
- [ ] `logout` → refresh token revoke edilir
- [ ] `verifyEmail` → geçerli token → emailVerified = true
- [ ] `verifyEmail` → expired token → `TokenExpiredException`
- [ ] `forgotPassword` → mevcut email → reset token oluşturulur, mail gönderilir
- [ ] `forgotPassword` → mevcut olmayan email → hata fırlatılmaz, 200 döner
- [ ] `resetPassword` → geçerli token → şifre güncellenir, tüm refresh'ler revoke

#### UserService Tests
- [ ] `getProfile` → mevcut user → `UserResponse`
- [ ] `updateProfile` → displayName güncelleme
- [ ] `changePassword` → yanlış current password → `InvalidCredentialsException`
- [ ] `deleteAccount` → soft delete (isActive = false)

### Backend Integration Tests (MockMvc)
- [ ] `POST /api/auth/register` → 201 + AuthResponse
- [ ] `POST /api/auth/register` duplicate → 409
- [ ] `POST /api/auth/login` → 200 + AuthResponse + Set-Cookie
- [ ] `POST /api/auth/login` yanlış şifre → 401
- [ ] `POST /api/auth/refresh` → 200 + yeni access token
- [ ] `POST /api/auth/logout` → 200 + cookie cleared
- [ ] `GET /api/users/me` without token → 401
- [ ] `GET /api/users/me` with token → 200 + UserResponse
- [ ] Rate limit: 11. login isteği → 429

### Frontend Tests (Thymeleaf + MockMvc)
- [ ] `AuthControllerMvcTest`: `GET /auth/login` → 200, login.html render
- [ ] `AuthControllerMvcTest`: `POST /auth/register` geçerli form → redirect to email-sent page
- [ ] `AuthControllerMvcTest`: `POST /auth/register` duplicate email → register.html, hata mesajı
- [ ] `AuthControllerMvcTest`: `POST /auth/login` yanlış şifre → login.html, hata mesajı
- [ ] `AuthControllerMvcTest`: kimlik doğrulamalı kullanıcı `/dashboard`'a erişebilir
- [ ] `AuthControllerMvcTest`: kimlik doğrulamasız kullanıcı `/dashboard`'a erişemez → login'e redirect

---

## ⚡ Risk Assessment

| Risk | Olasılık | Etki | Azaltma |
|------|----------|------|---------|
| JWT secret key sızıntısı | Düşük | Kritik | Environment variable, rotate mekanizması, key vault `[DECISION REQUIRED: Key rotation stratejisi?]` |
| Refresh token çalınması | Orta | Yüksek | HttpOnly cookie, rotation, theft detection (revoked reuse → all revoke) |
| Email enumeration via timing | Düşük | Orta | Response süresi sabitlenmesi veya `/forgot-password`'da her zaman 200 |
| Mail servis arızası | Orta | Orta | `@Async` + `@Retryable` + fallback logging, mail kuyruğu |
| BCrypt DoS (yavaş hash exploit) | Düşük | Orta | Rate limiting, strength 12 (250ms — kabul edilebilir) |
| CORS misconfiguration | Düşük | Yüksek | Whitelist-based config, wildcard `*` asla kullanılmaz |
| Concurrent refresh requests race condition | Orta | Düşük | Frontend queue mekanizması, backend idempotent refresh |

---

## ✅ Kabul Kriterleri

### Kayıt (Register)
- [ ] `POST /api/auth/register` → 201, `AuthResponse` dönülür, `Set-Cookie` header'ında refresh token var
- [ ] Duplicate email → 409, `errorCode = "EMAIL_ALREADY_EXISTS"`
- [ ] Duplicate username → 409, `errorCode = "USERNAME_ALREADY_EXISTS"`
- [ ] Zayıf şifre → 400, `errorCode = "VALIDATION_ERROR"`, field-level hata mesajları
- [ ] Kayıt sonrası e-posta doğrulama maili gönderilir (async)

### Giriş (Login)
- [ ] `POST /api/auth/login` → 200, `AuthResponse` + `Set-Cookie`
- [ ] Yanlış şifre → 401, `errorCode = "INVALID_CREDENTIALS"`
- [ ] Devre dışı hesap → 403, `errorCode = "ACCOUNT_DISABLED"`
- [ ] 5 başarısız deneme → 423, `errorCode = "ACCOUNT_LOCKED"`
- [ ] Kilitlenme sonrası 15 dk bekleme → giriş tekrar mümkün

### Token Yönetimi
- [ ] Access token 15 dakika sonra expire olur
- [ ] `/api/auth/refresh` → yeni access token + yeni refresh token (rotation)
- [ ] Revoke edilmiş refresh token kullanılırsa → tüm session'lar sonlandırılır (theft detection)
- [ ] `/api/auth/logout` → refresh token revoke + cookie temizlenir

### E-posta Doğrulama
- [ ] Doğrulama linki tıklanınca `emailVerified = true` olur
- [ ] 24 saat sonra token expire olur → `TOKEN_EXPIRED`
- [ ] Kullanılmış token tekrar kullanılamaz → `INVALID_TOKEN`

### Şifre Sıfırlama
- [ ] `/forgot-password` email yoksa da 200 döner
- [ ] Sıfırlama tokeni 1 saat geçerli, tek kullanımlık
- [ ] Şifre sıfırlama sonrası tüm mevcut session'lar sonlanır

### Rate Limiting
- [ ] Login: 10 istek/dk/IP → 429 response
- [ ] Register: 5 istek/dk/IP → 429 response
- [ ] Forgot Password: 3 istek/dk/IP → 429 response
- [ ] 429 response'ta `Retry-After` header bulunur

### Frontend (Thymeleaf)
- [ ] `GET /auth/login` → login.html render edilir, form görünür
- [ ] `POST /auth/register` geçerli veri → email-verification-sent sayfasına yönlendirilir
- [ ] `POST /auth/register` duplicate email → register.html'de hata mesajı görünür
- [ ] `POST /auth/login` başarılı → `/dashboard` redirect
- [ ] `POST /auth/login` yanlış şifre → login.html'de hata mesajı görünür
- [ ] `GET /auth/verify-email?token=...` geçerli → başarı mesajı
- [ ] Tüm form metinleri Türkçe
- [ ] Thymeleaf `th:errors` ile alan bazlı validasyon hataları Bootstrap `is-invalid` class ile gösterilir
