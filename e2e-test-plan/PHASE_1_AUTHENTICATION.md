# 🔐 Faz 1 — Authentication E2E Testleri

| Alan | Değer |
|------|-------|
| Süre | 1-2 gün |
| Bağımlılık | Faz 0 (altyapı hazır olmalı) |
| Hedef | Login, Register, Email Verification, Forgot/Reset Password ve Logout akışlarının tam E2E testi |
| Test Sayısı | ~15 test case |
| Allure Epic | `Authentication` |

---

## 📁 Oluşturulacak Dosyalar

### Page Object'ler

#### 1. `LoginPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/auth/LoginPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Email input | `#username` veya `input[name='email']` | E-posta alanı |
| Password input | `#password` veya `input[name='password']` | Şifre alanı |
| Login butonu | `button[type='submit']` veya `.btn-primary` | Giriş butonu |
| Şifremi Unuttum linki | `a[href*='forgot-password']` | Şifremi unuttum sayfasına link |
| Kayıt Ol linki | `a[href*='register']` | Kayıt sayfasına link |
| Hata mesajı | `.alert-danger` | Hata alert |
| Başarı mesajı | `.alert-success` | Başarı alert (logout sonrası) |

**Fluent Metotlar:**
```java
LoginPage enterEmail(String email)
LoginPage enterPassword(String password)
DashboardPage clickLogin()                          // Başarılı login → Dashboard
LoginPage clickLoginExpectingError()                // Başarısız login → aynı sayfa
ForgotPasswordPage clickForgotPassword()
RegisterPage clickRegister()
String getErrorMessage()
String getSuccessMessage()
boolean isErrorMessageVisible()
boolean isSuccessMessageVisible()
DashboardPage loginAs(String email, String password)  // Hızlı login
LoginPage loginExpectingError(String email, String password)
boolean isOnLoginPage()                             // URL kontrolü
```

---

#### 2. `RegisterPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/auth/RegisterPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Email input | `#email` | E-posta alanı |
| Username input | `#username` | Kullanıcı adı |
| Display Name input | `#displayName` | Görünen ad |
| Password input | `#password` | Şifre alanı |
| Register butonu | `button[type='submit']` | Kayıt ol butonu |
| Giriş Yap linki | `a[href*='login']` | Login sayfasına link |
| Hata mesajı | `.alert-danger` | Sunucu tarafı hata |
| Validation hataları | `.invalid-feedback` | Alan bazlı doğrulama hataları |

**Fluent Metotlar:**
```java
RegisterPage fillEmail(String email)
RegisterPage fillUsername(String username)
RegisterPage fillDisplayName(String displayName)
RegisterPage fillPassword(String password)
EmailVerificationSentPage clickRegister()           // Başarılı kayıt
RegisterPage clickRegisterExpectingError()           // Başarısız kayıt
LoginPage clickLoginLink()
String getErrorMessage()
String getFieldError(String fieldName)              // Belirli alanın validation hatası
boolean isFieldInvalid(String fieldName)            // Alan kırmızı kenarlık
boolean isOnRegisterPage()
RegisterPage fillForm(String email, String username, String displayName, String password)
```

---

#### 3. `EmailVerificationSentPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/auth/EmailVerificationSentPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Başlık | `h1` veya `h2` | "E-posta Doğrulama" başlığı |
| Mesaj | `.alert-info` veya `p` | Doğrulama bağlantısı gönderildi mesajı |
| Login linki | `a[href*='login']` | Giriş sayfasına link |

**Fluent Metotlar:**
```java
boolean isOnVerificationSentPage()
String getMessage()
LoginPage clickLoginLink()
```

---

#### 4. `ForgotPasswordPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/auth/ForgotPasswordPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Email input | `#email` veya `input[name='email']` | E-posta alanı |
| Gönder butonu | `button[type='submit']` | Formu gönder |
| Başarı mesajı | `.alert-success` | "Bağlantı gönderildi" mesajı |
| Hata mesajı | `.alert-danger` | Hata mesajı |
| Giriş linki | `a[href*='login']` | Login sayfasına dön |

**Fluent Metotlar:**
```java
ForgotPasswordPage enterEmail(String email)
ForgotPasswordPage submit()
String getSuccessMessage()
boolean isSuccessMessageVisible()
LoginPage clickLoginLink()
```

---

#### 5. `ResetPasswordPage.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/page/auth/ResetPasswordPage.java`

**Locator'lar:**
| Element | Locator | Açıklama |
|---------|---------|----------|
| Yeni şifre input | `#newPassword` veya `input[name='newPassword']` | Yeni şifre alanı |
| Token hidden field | `input[name='token']` | Token (hidden) |
| Kaydet butonu | `button[type='submit']` | Şifreyi sıfırla |
| Başarı mesajı | `.alert-success` | Şifre sıfırlandı mesajı |
| Hata mesajı | `.alert-danger` | Hata mesajı |

**Fluent Metotlar:**
```java
ResetPasswordPage enterNewPassword(String password)
ResetPasswordPage submit()
String getErrorMessage()
boolean isOnResetPage()
```

---

## 🧪 Test Sınıfı

### `AuthenticationE2eTest.java`

**Dosya:** `src/test/java/com/goaltracker/e2e/tests/auth/AuthenticationE2eTest.java`

**Allure Metadata:**
```java
@Epic("Authentication")
@Feature("Kullanıcı Kimlik Doğrulama")
```

---

### Test Case'ler — Login

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 1 | `shouldShowLoginPage` | Login sayfasının doğru render edildiğini doğrular | 1. `/auth/login` sayfasına git | ✅ URL `/auth/login` içerir<br>✅ Email input mevcut<br>✅ Password input mevcut<br>✅ Login butonu mevcut<br>✅ "Kayıt Ol" linki mevcut<br>✅ "Şifremi Unuttum" linki mevcut |
| 2 | `shouldLoginSuccessfullyWithValidCredentials` | Doğru bilgilerle giriş yapar | 1. Verified kullanıcı oluştur (DB helper)<br>2. Login sayfasına git<br>3. Email gir<br>4. Şifre gir<br>5. Login butonuna tıkla | ✅ URL `/dashboard` içerir<br>✅ Dashboard sayfası yüklendi<br>✅ Navbar'da profil/logout elemanları görünür |
| 3 | `shouldShowErrorForInvalidPassword` | Yanlış şifre ile hata mesajı gösterir | 1. Verified kullanıcı oluştur<br>2. Doğru email, yanlış şifre gir<br>3. Login butonuna tıkla | ✅ URL `/auth/login` (aynı sayfada kalır)<br>✅ `.alert-danger` görünür<br>✅ Hata mesajı "hatalı" kelimesini içerir |
| 4 | `shouldShowErrorForNonexistentEmail` | Kayıtlı olmayan email ile hata gösterir | 1. Kayıtlı olmayan email gir<br>2. Herhangi bir şifre gir<br>3. Login tıkla | ✅ URL `/auth/login`<br>✅ Hata mesajı görünür |
| 5 | `shouldShowErrorForEmptyEmailField` | Boş email ile form gönderildiğinde hata | 1. Email boş bırak<br>2. Şifre gir<br>3. Login tıkla | ✅ Login sayfasında kalır<br>✅ HTML5 validation veya server-side hata |
| 6 | `shouldShowErrorForEmptyPasswordField` | Boş şifre ile hata | 1. Email gir<br>2. Şifre boş bırak<br>3. Login tıkla | ✅ Login sayfasında kalır<br>✅ Hata mesajı veya validation |
| 7 | `shouldRedirectUnauthenticatedUserToLogin` | Yetkisiz erişimde login'e yönlendirir | 1. Cookie/auth olmadan `/dashboard` ziyaret et | ✅ URL `/auth/login` içerir |

---

### Test Case'ler — Logout

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 8 | `shouldLogoutSuccessfully` | Başarılı logout sonrası login sayfasına yönlenir | 1. Login yap<br>2. Dashboard'da olduğunu doğrula<br>3. Navbar'dan logout tıkla | ✅ URL `/auth/login` içerir<br>✅ "Başarıyla çıkış yaptınız" mesajı görünür |

---

### Test Case'ler — Register

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 9 | `shouldShowRegisterPage` | Register sayfasının doğru render edildiğini doğrular | 1. `/auth/register` sayfasına git | ✅ URL `/auth/register` içerir<br>✅ Email, username, displayName, password input'ları mevcut<br>✅ Register butonu mevcut<br>✅ "Giriş Yap" linki mevcut |
| 10 | `shouldRegisterNewUserSuccessfully` | Yeni kullanıcı kaydeder | 1. Register sayfasına git<br>2. Benzersiz email, username, displayName, güçlü şifre gir<br>3. Register butonuna tıkla | ✅ URL `/auth/email-verification-sent` içerir<br>✅ Doğrulama mesajı sayfası görünür |
| 11 | `shouldShowValidationErrorsOnRegister` | Geçersiz form ile validation hataları gösterir | 1. Register sayfasına git<br>2. Çok kısa username ("ab"), geçersiz email ("abc"), kısa şifre ("123") gir<br>3. Register tıkla | ✅ Register sayfasında kalır<br>✅ İlgili `.invalid-feedback` mesajları görünür |
| 12 | `shouldShowErrorForDuplicateEmail` | Mevcut email ile hata gösterir | 1. Kullanıcı oluştur (DB helper)<br>2. Register sayfasına git<br>3. Aynı email ile kayıt dene | ✅ Register sayfasında kalır<br>✅ "zaten kayıtlı" veya benzer hata mesajı |
| 13 | `shouldShowErrorForDuplicateUsername` | Mevcut username ile hata gösterir | 1. Kullanıcı oluştur (DB helper)<br>2. Register sayfasına git<br>3. Aynı username ile kayıt dene | ✅ Register sayfasında kalır<br>✅ Username hata mesajı |

---

### Test Case'ler — Forgot Password

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 14 | `shouldShowForgotPasswordPage` | Forgot password sayfası doğru render edilir | 1. Login sayfasında "Şifremi Unuttum" linkine tıkla | ✅ URL `/auth/forgot-password` içerir<br>✅ Email input mevcut<br>✅ Gönder butonu mevcut |
| 15 | `shouldSubmitForgotPasswordForm` | Email girdikten sonra success mesajı gösterir | 1. Forgot password sayfasına git<br>2. Email gir<br>3. Gönder butonuna tıkla | ✅ Aynı sayfada kalır<br>✅ "bağlantı gönderildi" success mesajı görünür<br>✅ Güvenlik: kayıtlı olmayan email de aynı mesajı gösterir |

---

### Test Case'ler — Navigasyon

| # | Metot Adı | Açıklama | Adımlar | Assertion'lar |
|---|-----------|----------|---------|---------------|
| 16 | `shouldNavigateFromLoginToRegister` | Login → Register navigasyonu | 1. Login sayfasına git<br>2. "Kayıt Ol" linkine tıkla | ✅ URL `/auth/register` içerir |
| 17 | `shouldNavigateFromRegisterToLogin` | Register → Login navigasyonu | 1. Register sayfasına git<br>2. "Giriş Yap" linkine tıkla | ✅ URL `/auth/login` içerir |

---

## 🛡️ Edge Case'ler & Güvenlik Testleri

| # | Test | Doğrulama |
|---|------|-----------|
| 1 | SQL Injection denemesi (email: `' OR 1=1 --`) | Login başarısız, hata mesajı, güvenlik ihlali yok |
| 2 | XSS payload (email: `<script>alert(1)</script>`) | Script çalışmaz, escaped gösterilir |
| 3 | Çok uzun email (500 karakter) | Validation hatası veya truncation |
| 4 | Birden fazla hızlı login denemesi | Rate limiting varsa 429 davranışı, yoksa her seferinde hata mesajı |
| 5 | Boş form gönderme (tüm alanlar boş) | HTML5 required validation veya server-side hata |

---

## 🏷️ Allure Etiketleme

```java
@Epic("Authentication")
@Feature("Login")
@Story("Başarılı Login")
@Severity(SeverityLevel.CRITICAL)
@Description("Doğru e-posta ve şifre ile giriş yapıldığında dashboard'a yönlendirilir")
@Step("Login sayfasına git")
```

Her test `@Story` ile kategorize edilir:
- **Login:** Başarılı Login, Başarısız Login, Boş Alanlar
- **Register:** Başarılı Kayıt, Validation Hataları, Duplicate Hataları
- **Forgot Password:** Form Gönderimi
- **Logout:** Başarılı Logout
- **Navigation:** Sayfalar Arası Geçiş

---

## ✅ Faz 1 Tamamlama Kriterleri

- [ ] LoginPage, RegisterPage, ForgotPasswordPage, ResetPasswordPage, EmailVerificationSentPage oluşturuldu
- [ ] 17 test case yazıldı ve başarıyla çalışıyor
- [ ] Her test başarılı assertion'lar içeriyor
- [ ] Edge case testleri mevcut
- [ ] Allure raporu her test için `@Epic`, `@Feature`, `@Story` etiketli
- [ ] Test failure'da screenshot Allure'a ekleniyor
- [ ] Tüm testler birbirinden izole (her test kendi kullanıcısını oluşturur)
- [ ] Headless modda CI'da çalışıyor

---

## 📁 Dosya Listesi

```
src/test/java/com/goaltracker/e2e/
    page/auth/
        LoginPage.java
        RegisterPage.java
        EmailVerificationSentPage.java
        ForgotPasswordPage.java
        ResetPasswordPage.java
    tests/auth/
        AuthenticationE2eTest.java
```

