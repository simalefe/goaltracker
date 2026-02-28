package com.goaltracker.integration;

import com.goaltracker.dto.request.ChangePasswordRequest;
import com.goaltracker.dto.request.LoginRequest;
import com.goaltracker.dto.request.UpdateProfileRequest;
import com.goaltracker.model.User;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for User Profile management.
 * Tests profile CRUD, password change, account deactivation, user search.
 */
class UserProfileIntegrationTest extends BaseIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndGetToken("profile@test.com", "profileuser", STRONG_PASSWORD);
    }

    // ========================================================================
    // GET PROFILE
    // ========================================================================

    @Nested
    @DisplayName("Profil Getirme (GET /api/users/me)")
    class GetProfileTests {

        @Test
        @DisplayName("Profil bilgileri doğru döner")
        void getProfile_success() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("profile@test.com"))
                    .andExpect(jsonPath("$.data.username").value("profileuser"))
                    .andExpect(jsonPath("$.data.displayName").value("profileuser Display"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.timezone").value("Europe/Istanbul"))
                    .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
        }
    }

    // ========================================================================
    // UPDATE PROFILE
    // ========================================================================

    @Nested
    @DisplayName("Profil Güncelleme (PUT /api/users/me)")
    class UpdateProfileTests {

        @Test
        @DisplayName("Display name güncelleme başarılı")
        void updateProfile_displayName() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest("Yeni İsim", null, null);

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.displayName").value("Yeni İsim"));

            // DB verification
            User user = userRepository.findByEmail("profile@test.com").orElseThrow();
            assertThat(user.getDisplayName()).isEqualTo("Yeni İsim");
        }

        @Test
        @DisplayName("Timezone güncelleme başarılı")
        void updateProfile_timezone() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest(null, "America/New_York", null);

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.timezone").value("America/New_York"));
        }

        @Test
        @DisplayName("Avatar URL güncelleme başarılı")
        void updateProfile_avatarUrl() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, "https://example.com/avatar.jpg");

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.jpg"));
        }

        @Test
        @DisplayName("Birden fazla alan aynı anda güncelleme")
        void updateProfile_multipleFields() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest("Yeni Ad", "Europe/London", "https://img.com/new.png");

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.displayName").value("Yeni Ad"))
                    .andExpect(jsonPath("$.data.timezone").value("Europe/London"))
                    .andExpect(jsonPath("$.data.avatarUrl").value("https://img.com/new.png"));
        }
    }

    // ========================================================================
    // CHANGE PASSWORD
    // ========================================================================

    @Nested
    @DisplayName("Şifre Değiştirme (PUT /api/users/me/password)")
    class ChangePasswordTests {

        @Test
        @DisplayName("Doğru mevcut şifre ile şifre değiştirilir")
        void changePassword_success() throws Exception {
            String newPassword = "NewStrongPass123!";
            ChangePasswordRequest request = new ChangePasswordRequest(STRONG_PASSWORD, newPassword);

            mockMvc.perform(put("/api/users/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // New password should work
            LoginRequest loginWithNew = new LoginRequest("profile@test.com", newPassword);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginWithNew)))
                    .andExpect(status().isOk());

            // Old password should not work
            LoginRequest loginWithOld = new LoginRequest("profile@test.com", STRONG_PASSWORD);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginWithOld)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Yanlış mevcut şifre ile şifre değiştirilmeye çalışılır — 401")
        void changePassword_wrongCurrentPassword() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("WrongOldPass123!", "NewPass123!");

            mockMvc.perform(put("/api/users/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Zayıf yeni şifre → 400")
        void changePassword_weakNewPassword() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(STRONG_PASSWORD, "weak");

            mockMvc.perform(put("/api/users/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // DELETE ACCOUNT
    // ========================================================================

    @Nested
    @DisplayName("Hesap Silme (DELETE /api/users/me)")
    class DeleteAccountTests {

        @Test
        @DisplayName("Hesap devre dışı bırakılır ve login artık çalışmaz")
        void deleteAccount_success() throws Exception {
            mockMvc.perform(delete("/api/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // User should be deactivated
            User user = userRepository.findByEmail("profile@test.com").orElseThrow();
            assertThat(user.isActive()).isFalse();

            // Login should fail
            LoginRequest loginRequest = new LoginRequest("profile@test.com", STRONG_PASSWORD);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================================================
    // USER SEARCH
    // ========================================================================

    @Nested
    @DisplayName("Kullanıcı Arama (GET /api/users/search)")
    class UserSearchTests {

        @Test
        @DisplayName("Username'e göre arama — sonuç döner")
        void searchUsers_success() throws Exception {
            // Register additional users
            registerAndGetToken("searchable@test.com", "searchable_user", STRONG_PASSWORD);
            registerAndGetToken("another@test.com", "another_user", STRONG_PASSWORD);

            mockMvc.perform(get("/api/users/search")
                            .header("Authorization", "Bearer " + token)
                            .param("q", "searchable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].username").value("searchable_user"));
        }

        @Test
        @DisplayName("Eşleşme olmayan arama — boş array")
        void searchUsers_noResults() throws Exception {
            mockMvc.perform(get("/api/users/search")
                            .header("Authorization", "Bearer " + token)
                            .param("q", "nonexistent_xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Case-insensitive arama")
        void searchUsers_caseInsensitive() throws Exception {
            registerAndGetToken("casesearch@test.com", "CaseUser", STRONG_PASSWORD);

            mockMvc.perform(get("/api/users/search")
                            .header("Authorization", "Bearer " + token)
                            .param("q", "caseuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }
    }

    // ========================================================================
    // BADGES & STATS
    // ========================================================================

    @Nested
    @DisplayName("Rozetler ve İstatistikler")
    class BadgesAndStatsTests {

        @Test
        @DisplayName("Kullanıcı rozetlerini getirme — 200")
        void getUserBadges() throws Exception {
            mockMvc.perform(get("/api/users/me/badges")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Kullanıcı istatistiklerini getirme — 200")
        void getUserStats() throws Exception {
            // Create a goal and add entry first
            Long goalId = createGoalAndGetId(token, "Stats Test");
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new com.goaltracker.dto.CreateEntryRequest(
                                    java.time.LocalDate.now(), new java.math.BigDecimal("5.0"), null))));

            mockMvc.perform(get("/api/users/me/stats")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalEntries").value(1))
                    .andExpect(jsonPath("$.data.completedGoals").isNumber())
                    .andExpect(jsonPath("$.data.totalStreakDays").isNumber())
                    .andExpect(jsonPath("$.data.earnedBadgeCount").isNumber());
        }
    }
}

