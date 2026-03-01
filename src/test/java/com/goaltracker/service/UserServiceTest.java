package com.goaltracker.service;

import com.goaltracker.dto.request.ChangePasswordRequest;
import com.goaltracker.dto.request.UpdateProfileRequest;
import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.exception.InvalidCredentialsException;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.Role;
import com.goaltracker.repository.RefreshTokenRepository;
import com.goaltracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$12$encodedHash");
        testUser.setDisplayName("Test User");
        testUser.setAvatarUrl(null);
        testUser.setTimezone("Europe/Istanbul");
        testUser.setRole(Role.USER);
        testUser.setActive(true);
        testUser.setEmailVerified(true);
    }

    // ─── getProfile Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        @Test
        @DisplayName("Mevcut kullanıcı → UserResponse döner")
        void shouldReturnUserResponse() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

            UserResponse result = userService.getProfile("test@test.com");

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.displayName()).isEqualTo("Test User");
            assertThat(result.timezone()).isEqualTo("Europe/Istanbul");
            assertThat(result.role()).isEqualTo("USER");
            assertThat(result.emailVerified()).isTrue();
        }

        @Test
        @DisplayName("Bulunamayan kullanıcı → RuntimeException")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile("notfound@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kullanıcı bulunamadı");
        }
    }

    // ─── updateProfile Tests ────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("displayName güncelleme → güncellenen profil döner")
        void shouldUpdateDisplayName() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest("Yeni İsim", null, null);
            UserResponse result = userService.updateProfile("test@test.com", request);

            assertThat(result.displayName()).isEqualTo("Yeni İsim");
            verify(userRepository).save(argThat(u -> "Yeni İsim".equals(u.getDisplayName())));
        }

        @Test
        @DisplayName("timezone güncelleme → güncellenen profil döner")
        void shouldUpdateTimezone() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest(null, "America/New_York", null);
            UserResponse result = userService.updateProfile("test@test.com", request);

            assertThat(result.timezone()).isEqualTo("America/New_York");
            verify(userRepository).save(argThat(u -> "America/New_York".equals(u.getTimezone())));
        }

        @Test
        @DisplayName("avatarUrl güncelleme → güncellenen profil döner")
        void shouldUpdateAvatarUrl() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest(null, null, "https://cdn.example.com/avatar.jpg");
            UserResponse result = userService.updateProfile("test@test.com", request);

            assertThat(result.avatarUrl()).isEqualTo("https://cdn.example.com/avatar.jpg");
        }

        @Test
        @DisplayName("Tüm alanlar güncelleme → hepsi güncellenir")
        void shouldUpdateAllFields() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest(
                    "Güncel İsim", "UTC", "https://img.com/pic.png");
            UserResponse result = userService.updateProfile("test@test.com", request);

            assertThat(result.displayName()).isEqualTo("Güncel İsim");
            assertThat(result.timezone()).isEqualTo("UTC");
            assertThat(result.avatarUrl()).isEqualTo("https://img.com/pic.png");
        }

        @Test
        @DisplayName("Null alanlar → mevcut değerler korunur")
        void shouldPreserveExistingValuesWhenNull() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest(null, null, null);
            UserResponse result = userService.updateProfile("test@test.com", request);

            assertThat(result.displayName()).isEqualTo("Test User");
            assertThat(result.timezone()).isEqualTo("Europe/Istanbul");
        }
    }

    // ─── changePassword Tests ───────────────────────────────────────

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("Başarılı şifre değişikliği → şifre güncellenir, refresh token'lar revoke edilir")
        void shouldChangePasswordSuccessfully() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("currentPass123", "$2a$12$encodedHash")).willReturn(true);
            given(passwordEncoder.encode("NewP@ssw0rd!")).willReturn("$2a$12$newHash");
            given(userRepository.save(any(User.class))).willReturn(testUser);

            ChangePasswordRequest request = new ChangePasswordRequest("currentPass123", "NewP@ssw0rd!");
            userService.changePassword("test@test.com", request);

            verify(userRepository).save(argThat(u -> "$2a$12$newHash".equals(u.getPasswordHash())));
            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }

        @Test
        @DisplayName("Yanlış mevcut şifre → InvalidCredentialsException")
        void shouldThrowOnWrongCurrentPassword() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPassword", "$2a$12$encodedHash")).willReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "NewP@ssw0rd!");

            assertThatThrownBy(() -> userService.changePassword("test@test.com", request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }
    }

    // ─── deleteAccount Tests ────────────────────────────────────────

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {

        @Test
        @DisplayName("Soft delete → isActive = false, refresh token'lar revoke edilir")
        void shouldSoftDeleteAccount() {
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            userService.deleteAccount("test@test.com");

            verify(userRepository).save(argThat(u -> !u.isActive()));
            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }

        @Test
        @DisplayName("Bulunamayan kullanıcı → RuntimeException")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteAccount("ghost@test.com"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─── searchUsers Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("searchUsers")
    class SearchUsersTests {

        @Test
        @DisplayName("Sonuç bulunan arama → UserResponse listesi döner")
        void shouldReturnMatchingUsers() {
            User user2 = new User();
            user2.setId(2L);
            user2.setEmail("ali@test.com");
            user2.setUsername("alidev");
            user2.setDisplayName("Ali Dev");
            user2.setTimezone("Europe/Istanbul");
            user2.setRole(Role.USER);
            user2.setEmailVerified(true);
            user2.setActive(true);

            given(userRepository.findByUsernameContainingIgnoreCaseAndIsActiveTrue("ali"))
                    .willReturn(List.of(user2));

            List<UserResponse> results = userService.searchUsers("ali");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).username()).isEqualTo("alidev");
        }

        @Test
        @DisplayName("Sonuç bulunamayan arama → boş liste döner")
        void shouldReturnEmptyListWhenNoMatch() {
            given(userRepository.findByUsernameContainingIgnoreCaseAndIsActiveTrue("xyz"))
                    .willReturn(List.of());

            List<UserResponse> results = userService.searchUsers("xyz");

            assertThat(results).isEmpty();
        }
    }
}


