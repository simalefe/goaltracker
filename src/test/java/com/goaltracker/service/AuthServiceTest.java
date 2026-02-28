package com.goaltracker.service;

import com.goaltracker.dto.request.LoginRequest;
import com.goaltracker.dto.request.RegisterRequest;
import com.goaltracker.dto.response.AuthResponse;
import com.goaltracker.exception.*;
import com.goaltracker.model.*;
import com.goaltracker.model.enums.Role;
import com.goaltracker.repository.*;
import com.goaltracker.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private MailService mailService;
    @Mock private NotificationService notificationService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$12$encodedHash");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.USER);
        testUser.setActive(true);
        testUser.setEmailVerified(false);
        testUser.setFailedLoginCount(0);
    }

    // ─── Register Tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Başarılı kayıt → AuthResponse döner")
        void shouldRegisterSuccessfully() {
            RegisterRequest req = new RegisterRequest("new@test.com", "newuser", "P@ssw0rd!", "New User");

            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(passwordEncoder.encode("P@ssw0rd!")).willReturn("$2a$12$encoded");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(10L);
                return u;
            });
            given(notificationService.ensureSettingsExist(10L)).willReturn(new NotificationSettings());
            given(jwtService.hashToken(anyString())).willReturn("hashedToken");
            given(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(jwtService.generateAccessToken(any())).willReturn("access-jwt-token");
            given(jwtService.generateRefreshToken()).willReturn("refresh-token-uuid");
            given(jwtService.getRefreshExpirationMs()).willReturn(604800000L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            AuthResponse result = authService.register(req, response);

            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo("access-jwt-token");
            assertThat(result.user()).isNotNull();
            verify(userRepository).save(any(User.class));
            verify(mailService).sendVerificationEmail(eq("new@test.com"), anyString());
        }

        @Test
        @DisplayName("Duplicate email → EmailAlreadyExistsException")
        void shouldThrowWhenEmailExists() {
            RegisterRequest req = new RegisterRequest("test@test.com", "newuser", "P@ssw0rd!", "User");

            given(userRepository.existsByEmail("test@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(req, response))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Duplicate username → UsernameAlreadyExistsException")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest req = new RegisterRequest("new@test.com", "testuser", "P@ssw0rd!", "User");

            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(userRepository.existsByUsername("testuser")).willReturn(true);

            assertThatThrownBy(() -> authService.register(req, response))
                    .isInstanceOf(UsernameAlreadyExistsException.class);
        }
    }

    // ─── Login Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Başarılı giriş → AuthResponse döner")
        void shouldLoginSuccessfully() {
            LoginRequest req = new LoginRequest("test@test.com", "correctPassword");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("correctPassword", "$2a$12$encodedHash")).willReturn(true);
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(jwtService.generateAccessToken(any())).willReturn("access-token");
            given(jwtService.generateRefreshToken()).willReturn("refresh-token");
            given(jwtService.hashToken("refresh-token")).willReturn("hashedRefresh");
            given(jwtService.getRefreshExpirationMs()).willReturn(604800000L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            AuthResponse result = authService.login(req, response);

            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo("access-token");
            verify(userRepository).save(argThat(u -> u.getFailedLoginCount() == 0));
        }

        @Test
        @DisplayName("Yanlış şifre → InvalidCredentialsException + failedLoginCount artıyor")
        void shouldThrowOnWrongPassword() {
            LoginRequest req = new LoginRequest("test@test.com", "wrongPassword");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPassword", "$2a$12$encodedHash")).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            assertThatThrownBy(() -> authService.login(req, response))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepository).save(argThat(u -> u.getFailedLoginCount() == 1));
        }

        @Test
        @DisplayName("5 başarısız deneme sonrası hesap kilitleniyor")
        void shouldLockAccountAfterMaxAttempts() {
            testUser.setFailedLoginCount(4); // Next fail will be 5th
            LoginRequest req = new LoginRequest("test@test.com", "wrong");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrong", "$2a$12$encodedHash")).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            assertThatThrownBy(() -> authService.login(req, response))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepository).save(argThat(u ->
                    u.getFailedLoginCount() == 5 && u.getLockedUntil() != null));
        }

        @Test
        @DisplayName("Kilitli hesap → AccountLockedException")
        void shouldThrowWhenAccountLocked() {
            testUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
            LoginRequest req = new LoginRequest("test@test.com", "P@ssw0rd!");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req, response))
                    .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @DisplayName("Deaktif hesap → AccountDisabledException")
        void shouldThrowWhenAccountDisabled() {
            testUser.setActive(false);
            LoginRequest req = new LoginRequest("test@test.com", "P@ssw0rd!");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req, response))
                    .isInstanceOf(AccountDisabledException.class);
        }

        @Test
        @DisplayName("Var olmayan email → InvalidCredentialsException")
        void shouldThrowOnNonExistentEmail() {
            LoginRequest req = new LoginRequest("notfound@test.com", "password");

            given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req, response))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("Kilit süresi dolmuş hesap → giriş yapılabilir")
        void shouldAllowLoginAfterLockExpired() {
            testUser.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
            testUser.setFailedLoginCount(5);
            LoginRequest req = new LoginRequest("test@test.com", "correctPassword");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("correctPassword", "$2a$12$encodedHash")).willReturn(true);
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(jwtService.generateAccessToken(any())).willReturn("access-token");
            given(jwtService.generateRefreshToken()).willReturn("refresh-token");
            given(jwtService.hashToken("refresh-token")).willReturn("hashedRefresh");
            given(jwtService.getRefreshExpirationMs()).willReturn(604800000L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            AuthResponse result = authService.login(req, response);

            assertThat(result).isNotNull();
        }
    }

    // ─── Refresh Token Tests ────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("Geçerli refresh token → yeni access token")
        void shouldRefreshTokenSuccessfully() {
            Cookie refreshCookie = new Cookie("refreshToken", "valid-raw-token");
            given(request.getCookies()).willReturn(new Cookie[]{refreshCookie});
            given(jwtService.hashToken("valid-raw-token")).willReturn("hashed-valid");

            RefreshToken storedToken = new RefreshToken();
            storedToken.setUser(testUser);
            storedToken.setTokenHash("hashed-valid");
            storedToken.setRevoked(false);
            storedToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

            given(refreshTokenRepository.findByTokenHash("hashed-valid")).willReturn(Optional.of(storedToken));
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));
            given(jwtService.generateAccessToken(any())).willReturn("new-access-token");
            given(jwtService.generateRefreshToken()).willReturn("new-refresh-token");
            given(jwtService.hashToken("new-refresh-token")).willReturn("hashed-new-refresh");
            given(jwtService.getRefreshExpirationMs()).willReturn(604800000L);

            String newAccessToken = authService.refreshToken(request, response);

            assertThat(newAccessToken).isEqualTo("new-access-token");
            assertThat(storedToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Refresh token cookie yok → InvalidTokenException")
        void shouldThrowWhenNoCookie() {
            given(request.getCookies()).willReturn(null);

            assertThatThrownBy(() -> authService.refreshToken(request, response))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("bulunamadı");
        }

        @Test
        @DisplayName("Revoked refresh token → güvenlik ihlali, tüm session'lar sonlandırılır")
        void shouldDetectTokenTheft() {
            Cookie refreshCookie = new Cookie("refreshToken", "stolen-token");
            given(request.getCookies()).willReturn(new Cookie[]{refreshCookie});
            given(jwtService.hashToken("stolen-token")).willReturn("hashed-stolen");

            RefreshToken storedToken = new RefreshToken();
            storedToken.setUser(testUser);
            storedToken.setRevoked(true); // Already revoked!

            given(refreshTokenRepository.findByTokenHash("hashed-stolen")).willReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> authService.refreshToken(request, response))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Güvenlik ihlali");

            verify(refreshTokenRepository).revokeAllByUserId(testUser.getId());
        }

        @Test
        @DisplayName("Süresi dolmuş refresh token → InvalidTokenException")
        void shouldThrowWhenExpiredToken() {
            Cookie refreshCookie = new Cookie("refreshToken", "expired-token");
            given(request.getCookies()).willReturn(new Cookie[]{refreshCookie});
            given(jwtService.hashToken("expired-token")).willReturn("hashed-expired");

            RefreshToken storedToken = new RefreshToken();
            storedToken.setUser(testUser);
            storedToken.setRevoked(false);
            storedToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            given(refreshTokenRepository.findByTokenHash("hashed-expired")).willReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> authService.refreshToken(request, response))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("süresi dolmuş");
        }
    }

    // ─── Logout Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("Başarılı logout → token revoke edilir")
        void shouldLogoutSuccessfully() {
            Cookie refreshCookie = new Cookie("refreshToken", "raw-token");
            given(request.getCookies()).willReturn(new Cookie[]{refreshCookie});
            given(jwtService.hashToken("raw-token")).willReturn("hashed-token");

            RefreshToken rt = new RefreshToken();
            rt.setRevoked(false);
            given(refreshTokenRepository.findByTokenHash("hashed-token")).willReturn(Optional.of(rt));
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            authService.logout(request, response);

            assertThat(rt.isRevoked()).isTrue();
            verify(response).addCookie(argThat(c -> c.getMaxAge() == 0));
        }

        @Test
        @DisplayName("Cookie yokken logout → hata fırlatmaz")
        void shouldHandleLogoutWithNoCookie() {
            given(request.getCookies()).willReturn(null);

            assertThatNoException().isThrownBy(() -> authService.logout(request, response));
            verify(response).addCookie(argThat(c -> c.getMaxAge() == 0));
        }
    }

    // ─── Email Verification Tests ───────────────────────────────────

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmailTests {

        @Test
        @DisplayName("Geçerli token → email doğrulanır")
        void shouldVerifyEmail() {
            EmailVerificationToken evt = new EmailVerificationToken();
            evt.setUser(testUser);
            evt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            given(jwtService.hashToken("valid-token")).willReturn("hashed-valid");
            given(emailVerificationTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-valid"))
                    .willReturn(Optional.of(evt));
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail("valid-token");

            assertThat(testUser.isEmailVerified()).isTrue();
            assertThat(evt.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Geçersiz/kullanılmış token → InvalidTokenException")
        void shouldThrowOnInvalidVerificationToken() {
            given(jwtService.hashToken("invalid-token")).willReturn("hashed-invalid");
            given(emailVerificationTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-invalid"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Süresi dolmuş doğrulama tokeni → TokenExpiredException")
        void shouldThrowOnExpiredVerificationToken() {
            EmailVerificationToken evt = new EmailVerificationToken();
            evt.setUser(testUser);
            evt.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            given(jwtService.hashToken("expired-token")).willReturn("hashed-expired");
            given(emailVerificationTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-expired"))
                    .willReturn(Optional.of(evt));

            assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                    .isInstanceOf(TokenExpiredException.class);
        }
    }

    // ─── Forgot Password Tests ──────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Var olan email → reset token oluşturulur")
        void shouldCreateResetTokenForExistingEmail() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(jwtService.hashToken(anyString())).willReturn("hashed-reset");
            given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            authService.forgotPassword("test@test.com");

            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(mailService).sendPasswordResetEmail(eq("test@test.com"), anyString());
        }

        @Test
        @DisplayName("Var olmayan email → hata fırlatmaz (email enumeration protection)")
        void shouldNotThrowForNonExistentEmail() {
            given(userRepository.findByEmail("nonexistent@test.com")).willReturn(Optional.empty());

            assertThatNoException().isThrownBy(() -> authService.forgotPassword("nonexistent@test.com"));
            verify(passwordResetTokenRepository, never()).save(any());
        }
    }

    // ─── Reset Password Tests ───────────────────────────────────────

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("Geçerli token → şifre sıfırlanır, tüm refresh token'lar revoke edilir")
        void shouldResetPasswordSuccessfully() {
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(testUser);
            prt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));

            given(jwtService.hashToken("reset-token")).willReturn("hashed-reset");
            given(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-reset"))
                    .willReturn(Optional.of(prt));
            given(passwordEncoder.encode("NewP@ssw0rd!")).willReturn("$2a$12$newEncoded");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            authService.resetPassword("reset-token", "NewP@ssw0rd!");

            assertThat(testUser.getPasswordHash()).isEqualTo("$2a$12$newEncoded");
            assertThat(prt.getUsedAt()).isNotNull();
            verify(refreshTokenRepository).revokeAllByUserId(testUser.getId());
        }

        @Test
        @DisplayName("Geçersiz reset token → InvalidTokenException")
        void shouldThrowOnInvalidResetToken() {
            given(jwtService.hashToken("bad-token")).willReturn("hashed-bad");
            given(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-bad"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("bad-token", "NewP@ss!"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Süresi dolmuş reset token → TokenExpiredException")
        void shouldThrowOnExpiredResetToken() {
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(testUser);
            prt.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            given(jwtService.hashToken("expired-reset")).willReturn("hashed-expired");
            given(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull("hashed-expired"))
                    .willReturn(Optional.of(prt));

            assertThatThrownBy(() -> authService.resetPassword("expired-reset", "NewP@ss!"))
                    .isInstanceOf(TokenExpiredException.class);
        }
    }
}

