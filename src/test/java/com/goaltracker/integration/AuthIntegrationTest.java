package com.goaltracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.goaltracker.dto.request.*;
import com.goaltracker.model.EmailVerificationToken;
import com.goaltracker.model.PasswordResetToken;
import com.goaltracker.model.User;
import com.goaltracker.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Authentication flows.
 * Tests the full lifecycle: HTTP → Controller → Service → Repository → DB
 */
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    // ========================================================================
    // REGISTRATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Kayıt (Register)")
    class RegisterTests {

        @Test
        @DisplayName("Başarılı kayıt — 201 Created, JWT token döner, DB'de user oluşur")
        void register_success() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "newuser@test.com", "newuser", STRONG_PASSWORD, "Yeni Kullanıcı");

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.user.email").value("newuser@test.com"))
                    .andExpect(jsonPath("$.data.user.username").value("newuser"))
                    .andExpect(jsonPath("$.data.user.displayName").value("Yeni Kullanıcı"))
                    .andExpect(jsonPath("$.data.user.emailVerified").value(false))
                    .andExpect(jsonPath("$.data.user.role").value("USER"))
                    .andReturn();

            // DB assertions
            User user = userRepository.findByEmail("newuser@test.com").orElseThrow();
            assertThat(user.getUsername()).isEqualTo("newuser");
            assertThat(user.isActive()).isTrue();
            assertThat(user.isEmailVerified()).isFalse();
            assertThat(user.getFailedLoginCount()).isZero();
            assertThat(user.getCreatedAt()).isNotNull();

            // Refresh token cookie should be set
            Cookie refreshCookie = result.getResponse().getCookie("refreshToken");
            assertThat(refreshCookie).isNotNull();
            assertThat(refreshCookie.isHttpOnly()).isTrue();

            // Email verification token should be created
            assertThat(emailVerificationTokenRepository.count()).isGreaterThan(0);

            // Mail should be sent
            verify(mailService).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Aynı email ile tekrar kayıt — 409 CONFLICT")
        void register_duplicateEmail() throws Exception {
            // First registration
            registerAndGetToken("dup@test.com", "dup_user1", STRONG_PASSWORD);

            // Duplicate email
            RegisterRequest request = new RegisterRequest("dup@test.com", "dup_user2", STRONG_PASSWORD, "Dup");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("Aynı username ile tekrar kayıt — 409 CONFLICT")
        void register_duplicateUsername() throws Exception {
            registerAndGetToken("user1@test.com", "same_username", STRONG_PASSWORD);

            RegisterRequest request = new RegisterRequest("user2@test.com", "same_username", STRONG_PASSWORD, "Dup");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("Validation hataları — boş email, zayıf şifre → 400")
        void register_validationErrors() throws Exception {
            RegisterRequest request = new RegisterRequest("", "u", "weak", "");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("Geçersiz email formatı → 400")
        void register_invalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest("invalid-email", "validuser", STRONG_PASSWORD, "Test");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    // ========================================================================
    // LOGIN TESTS
    // ========================================================================

    @Nested
    @DisplayName("Giriş (Login)")
    class LoginTests {

        @Test
        @DisplayName("Başarılı login — 200, access token döner")
        void login_success() throws Exception {
            registerAndGetToken("login@test.com", "loginuser", STRONG_PASSWORD);

            LoginRequest loginRequest = new LoginRequest("login@test.com", STRONG_PASSWORD);

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.user.email").value("login@test.com"))
                    .andReturn();

            // Token should be valid
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            String token = root.path("data").path("accessToken").asText();
            assertThat(jwtService.isTokenExpired(token)).isFalse();

            // Refresh token cookie
            Cookie refreshCookie = result.getResponse().getCookie("refreshToken");
            assertThat(refreshCookie).isNotNull();

            // Failed login count should be reset
            User user = userRepository.findByEmail("login@test.com").orElseThrow();
            assertThat(user.getFailedLoginCount()).isZero();
            assertThat(user.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Yanlış şifre ile login — 401 UNAUTHORIZED")
        void login_wrongPassword() throws Exception {
            registerAndGetToken("wrong@test.com", "wronguser", STRONG_PASSWORD);

            LoginRequest loginRequest = new LoginRequest("wrong@test.com", "WrongPassword1!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

            // Failed login count should increase
            User user = userRepository.findByEmail("wrong@test.com").orElseThrow();
            assertThat(user.getFailedLoginCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Kayıtlı olmayan email ile login — 401")
        void login_nonExistentEmail() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent@test.com", STRONG_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("5 başarısız deneme sonrası hesap kilitlenir — 423")
        void login_accountLockout() throws Exception {
            registerAndGetToken("lockme@test.com", "lockuser", STRONG_PASSWORD);

            LoginRequest wrongRequest = new LoginRequest("lockme@test.com", "WrongPassword1!");

            // 5 failed attempts
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongRequest)))
                        .andExpect(status().isUnauthorized());
            }

            // Verify account is locked in DB
            User user = userRepository.findByEmail("lockme@test.com").orElseThrow();
            assertThat(user.getFailedLoginCount()).isEqualTo(5);
            assertThat(user.getLockedUntil()).isNotNull();

            // 6th attempt with correct password should fail with LOCKED
            LoginRequest correctRequest = new LoginRequest("lockme@test.com", STRONG_PASSWORD);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(correctRequest)))
                    .andExpect(status().is(423))
                    .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
        }

        @Test
        @DisplayName("Devre dışı bırakılmış hesap ile login — 403")
        void login_disabledAccount() throws Exception {
            registerAndGetToken("disabled@test.com", "disableduser", STRONG_PASSWORD);

            // Disable the account
            User user = userRepository.findByEmail("disabled@test.com").orElseThrow();
            user.setActive(false);
            userRepository.save(user);

            LoginRequest loginRequest = new LoginRequest("disabled@test.com", STRONG_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
        }
    }

    // ========================================================================
    // EMAIL VERIFICATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("E-posta Doğrulama")
    class EmailVerificationTests {

        @Test
        @DisplayName("Başarılı e-posta doğrulama — token ile verify")
        void verifyEmail_success() throws Exception {
            registerAndGetToken("verify@test.com", "verifyuser", STRONG_PASSWORD);

            // Get the token from DB
            User user = userRepository.findByEmail("verify@test.com").orElseThrow();
            assertThat(user.isEmailVerified()).isFalse();

            EmailVerificationToken evt = emailVerificationTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()) && t.getUsedAt() == null)
                    .findFirst().orElseThrow();

            // We need raw token — since hash is stored, we need to use the raw one.
            // Unfortunately we can't reverse SHA-256. Let's create a known token.
            String rawToken = "test-verify-token-12345";
            evt.setTokenHash(jwtService.hashToken(rawToken));
            emailVerificationTokenRepository.save(evt);

            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("token", rawToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // User should be verified
            User updatedUser = userRepository.findByEmail("verify@test.com").orElseThrow();
            assertThat(updatedUser.isEmailVerified()).isTrue();

            // Token should be marked as used
            EmailVerificationToken usedToken = emailVerificationTokenRepository.findById(evt.getId()).orElseThrow();
            assertThat(usedToken.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Geçersiz token ile doğrulama — 400")
        void verifyEmail_invalidToken() throws Exception {
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("token", "invalid-token"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========================================================================
    // PASSWORD RESET TESTS
    // ========================================================================

    @Nested
    @DisplayName("Şifre Sıfırlama")
    class PasswordResetTests {

        @Test
        @DisplayName("Şifre sıfırlama tam akışı — forgot → reset → yeni şifre ile login")
        void passwordReset_fullFlow() throws Exception {
            registerAndGetToken("reset@test.com", "resetuser", STRONG_PASSWORD);

            // Step 1: Forgot password
            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("reset@test.com");
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Step 2: Get the token from DB and create known raw token
            User user = userRepository.findByEmail("reset@test.com").orElseThrow();
            PasswordResetToken prt = passwordResetTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()) && t.getUsedAt() == null)
                    .findFirst().orElseThrow();

            String rawResetToken = "test-reset-token-67890";
            prt.setTokenHash(jwtService.hashToken(rawResetToken));
            passwordResetTokenRepository.save(prt);

            // Step 3: Reset password
            String newPassword = "NewPassword456!";
            ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawResetToken, newPassword);
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Step 4: Login with new password
            LoginRequest loginRequest = new LoginRequest("reset@test.com", newPassword);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

            // Step 5: Old password should not work
            LoginRequest oldLoginRequest = new LoginRequest("reset@test.com", STRONG_PASSWORD);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(oldLoginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Kayıtlı olmayan email ile forgot password — 200 döner (email enumeration protection)")
        void forgotPassword_nonExistentEmail() throws Exception {
            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("noone@test.com");
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ========================================================================
    // REFRESH TOKEN TESTS
    // ========================================================================

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh token ile yeni access token al — 200")
        void refresh_success() throws Exception {
            RegisterRequest request = new RegisterRequest("refresh@test.com", "refreshuser", STRONG_PASSWORD, "Ref");

            MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Cookie refreshCookie = registerResult.getResponse().getCookie("refreshToken");
            assertThat(refreshCookie).isNotNull();

            // Use refresh token to get new access token
            MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(refreshCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andReturn();

            // New access token should be valid
            JsonNode root = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
            String newAccessToken = root.path("data").path("accessToken").asText();
            assertThat(jwtService.isTokenExpired(newAccessToken)).isFalse();

            // New refresh token cookie should be set (rotation)
            Cookie newRefreshCookie = refreshResult.getResponse().getCookie("refreshToken");
            assertThat(newRefreshCookie).isNotNull();
        }

        @Test
        @DisplayName("Refresh token olmadan — 400")
        void refresh_noCookie() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Geçersiz refresh token — 400")
        void refresh_invalidToken() throws Exception {
            Cookie fakeCookie = new Cookie("refreshToken", "fake-token-value");
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(fakeCookie))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========================================================================
    // LOGOUT TESTS
    // ========================================================================

    @Nested
    @DisplayName("Çıkış (Logout)")
    class LogoutTests {

        @Test
        @DisplayName("Başarılı logout — refresh token revoke edilir")
        void logout_success() throws Exception {
            RegisterRequest request = new RegisterRequest("logout@test.com", "logoutuser", STRONG_PASSWORD, "Log");

            MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Cookie refreshCookie = registerResult.getResponse().getCookie("refreshToken");

            // Logout
            MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                            .cookie(refreshCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            // Refresh token cookie should be cleared
            Cookie clearedCookie = logoutResult.getResponse().getCookie("refreshToken");
            assertThat(clearedCookie).isNotNull();
            assertThat(clearedCookie.getMaxAge()).isZero();

            // Old refresh token should not work anymore
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(refreshCookie))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // JWT TOKEN VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("JWT Doğrulama")
    class JwtValidationTests {

        @Test
        @DisplayName("Geçerli token ile korumalı endpoint'e erişim — 200")
        void validToken_accessProtectedEndpoint() throws Exception {
            String token = registerAndGetToken("jwt@test.com", "jwtuser", STRONG_PASSWORD);

            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("jwt@test.com"));
        }

        @Test
        @DisplayName("Token olmadan korumalı endpoint'e erişim — 401/302 redirect")
        void noToken_accessProtectedEndpoint() throws Exception {
            mockMvc.perform(get("/api/goals")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Geçersiz token ile erişim — 302 redirect (filter skips)")
        void invalidToken_accessProtectedEndpoint() throws Exception {
            mockMvc.perform(get("/api/goals")
                            .header("Authorization", "Bearer invalid.token.here")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is3xxRedirection());
        }
    }
}

