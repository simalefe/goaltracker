package com.goaltracker.service;

import com.goaltracker.dto.request.NotificationSettingsRequest;
import com.goaltracker.dto.response.NotificationResponse;
import com.goaltracker.dto.response.NotificationSettingsResponse;
import com.goaltracker.model.Notification;
import com.goaltracker.model.NotificationSettings;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.repository.NotificationRepository;
import com.goaltracker.repository.NotificationSettingsRepository;
import com.goaltracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSettingsRepository settingsRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private MailService mailService;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;
    private NotificationSettings testSettings;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setUsername("testuser");

        testNotification = new Notification();
        testNotification.setId(100L);
        testNotification.setUser(testUser);
        testNotification.setType(NotificationType.DAILY_REMINDER);
        testNotification.setTitle("Günlük Hatırlatma");
        testNotification.setMessage("Bugün hedefinize giriş yapmayı unutmayın!");
        testNotification.setRead(false);
        testNotification.setSentAt(Instant.now());

        testSettings = new NotificationSettings();
        testSettings.setId(1L);
        testSettings.setUser(testUser);
        testSettings.setEmailEnabled(true);
        testSettings.setPushEnabled(true);
        testSettings.setDailyReminderTime(LocalTime.of(20, 0));
        testSettings.setWeeklySummaryDay(1);
        testSettings.setWeeklySummaryEnabled(true);
        testSettings.setStreakDangerEnabled(true);
    }

    // ─── createNotification Tests ────────────────────────────────────

    @Nested
    @DisplayName("createNotification")
    class CreateNotificationTests {

        @Test
        @DisplayName("Başarılı bildirim oluşturma")
        void shouldCreateNotification() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(100L);
                return n;
            });
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            NotificationResponse result = notificationService.createNotification(
                    1L, NotificationType.DAILY_REMINDER, "Test Başlık", "Test mesaj", null);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Başlık");
            verify(notificationRepository).save(any(Notification.class));
            verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any());
        }

        @Test
        @DisplayName("Email etkinken DAILY_REMINDER → e-posta gönderilir")
        void shouldSendEmailForDailyReminder() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(101L);
                return n;
            });
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            notificationService.createNotification(
                    1L, NotificationType.DAILY_REMINDER, "Hatırlatma", "Mesaj", null);

            verify(mailService).sendNotificationEmail(eq("test@test.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("Email kapalıyken → e-posta gönderilmez")
        void shouldNotSendEmailWhenDisabled() {
            testSettings.setEmailEnabled(false);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(102L);
                return n;
            });
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            notificationService.createNotification(
                    1L, NotificationType.DAILY_REMINDER, "Hatırlatma", "Mesaj", null);

            verify(mailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("FRIEND_ACTIVITY tipi için e-posta gönderilmez")
        void shouldNotSendEmailForFriendActivity() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(103L);
                return n;
            });
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            notificationService.createNotification(
                    1L, NotificationType.FRIEND_ACTIVITY, "Arkadaş", "Mesaj", null);

            verify(mailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Metadata ile bildirim oluşturma")
        void shouldCreateNotificationWithMetadata() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(104L);
                return n;
            });
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            Map<String, Object> metadata = Map.of("goalId", 10L);
            notificationService.createNotification(
                    1L, NotificationType.GOAL_COMPLETED, "Tebrikler!", "Hedefinizi tamamladınız!", metadata);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getMetadata()).isNotNull();
        }

        @Test
        @DisplayName("Kullanıcı bulunamadı → RuntimeException")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.createNotification(
                    999L, NotificationType.DAILY_REMINDER, "Test", "Mesaj", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kullanıcı bulunamadı");
        }
    }

    // ─── markAsRead Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("Bildirim okundu olarak işaretlenir")
        void shouldMarkAsRead() {
            given(notificationRepository.findById(100L)).willReturn(Optional.of(testNotification));
            given(notificationRepository.save(any(Notification.class))).willReturn(testNotification);

            notificationService.markAsRead(100L, 1L);

            assertThat(testNotification.isRead()).isTrue();
            verify(notificationRepository).save(testNotification);
        }

        @Test
        @DisplayName("Başka kullanıcının bildirimi → RuntimeException")
        void shouldThrowWhenNotOwner() {
            given(notificationRepository.findById(100L)).willReturn(Optional.of(testNotification));

            assertThatThrownBy(() -> notificationService.markAsRead(100L, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("erişim yetkiniz yok");
        }

        @Test
        @DisplayName("Bildirim bulunamadı → RuntimeException")
        void shouldThrowWhenNotificationNotFound() {
            given(notificationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Bildirim bulunamadı");
        }
    }

    // ─── markAllAsRead Tests ─────────────────────────────────────────

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Tüm bildirimler okundu işaretlenir")
        void shouldMarkAllAsRead() {
            notificationService.markAllAsRead(1L);
            verify(notificationRepository).markAllAsReadByUserId(1L);
        }
    }

    // ─── getNotifications Tests ──────────────────────────────────────

    @Nested
    @DisplayName("getNotifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Sayfalı bildirim listesi döner")
        void shouldReturnPagedNotifications() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(testNotification), pageable, 1);
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).willReturn(page);

            Page<NotificationResponse> result = notificationService.getNotifications(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Günlük Hatırlatma");
        }
    }

    // ─── getUnreadCount Tests ────────────────────────────────────────

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Okunmamış bildirim sayısı döner")
        void shouldReturnUnreadCount() {
            given(notificationRepository.countByUserIdAndIsReadFalse(1L)).willReturn(5L);

            long count = notificationService.getUnreadCount(1L);

            assertThat(count).isEqualTo(5);
        }
    }

    // ─── Settings Tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("Settings")
    class SettingsTests {

        @Test
        @DisplayName("Ayarlar döner")
        void shouldReturnSettings() {
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            NotificationSettingsResponse response = notificationService.getSettings(1L);

            assertThat(response.isEmailEnabled()).isTrue();
            assertThat(response.isPushEnabled()).isTrue();
            assertThat(response.getDailyReminderTime()).isEqualTo(LocalTime.of(20, 0));
        }

        @Test
        @DisplayName("Ayarlar yoksa varsayılanlar oluşturulur")
        void shouldCreateDefaultSettingsIfNotExist() {
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.empty());
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(settingsRepository.save(any(NotificationSettings.class))).willAnswer(inv -> {
                NotificationSettings ns = inv.getArgument(0);
                ns.setId(1L);
                return ns;
            });

            NotificationSettingsResponse response = notificationService.getSettings(1L);

            assertThat(response).isNotNull();
            verify(settingsRepository).save(any(NotificationSettings.class));
        }

        @Test
        @DisplayName("Ayarlar güncelleniyor")
        void shouldUpdateSettings() {
            NotificationSettingsRequest req = new NotificationSettingsRequest();
            req.setEmailEnabled(false);
            req.setPushEnabled(false);
            req.setDailyReminderTime(LocalTime.of(9, 0));
            req.setWeeklySummaryDay(5);
            req.setWeeklySummaryEnabled(false);
            req.setStreakDangerEnabled(false);

            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));
            given(settingsRepository.save(any(NotificationSettings.class))).willAnswer(inv -> inv.getArgument(0));

            NotificationSettingsResponse response = notificationService.updateSettings(1L, req);

            assertThat(response.isEmailEnabled()).isFalse();
            assertThat(response.isPushEnabled()).isFalse();
            assertThat(response.getDailyReminderTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(response.getWeeklySummaryDay()).isEqualTo(5);
        }

        @Test
        @DisplayName("Kısmi güncelleme — sadece emailEnabled değişir")
        void shouldPartiallyUpdateSettings() {
            NotificationSettingsRequest req = new NotificationSettingsRequest();
            req.setEmailEnabled(false);
            // Other fields null → unchanged

            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));
            given(settingsRepository.save(any(NotificationSettings.class))).willAnswer(inv -> inv.getArgument(0));

            NotificationSettingsResponse response = notificationService.updateSettings(1L, req);

            assertThat(response.isEmailEnabled()).isFalse();
            assertThat(response.isPushEnabled()).isTrue(); // unchanged
            assertThat(response.isWeeklySummaryEnabled()).isTrue(); // unchanged
        }
    }

    // ─── alreadySentToday Tests ──────────────────────────────────────

    @Nested
    @DisplayName("alreadySentToday")
    class AlreadySentTodayTests {

        @Test
        @DisplayName("Bugün gönderilmiş → true")
        void shouldReturnTrueIfAlreadySent() {
            given(notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(NotificationType.DAILY_REMINDER), any(Instant.class))).willReturn(true);

            boolean result = notificationService.alreadySentToday(1L, NotificationType.DAILY_REMINDER);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Bugün gönderilmemiş → false")
        void shouldReturnFalseIfNotSent() {
            given(notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(NotificationType.DAILY_REMINDER), any(Instant.class))).willReturn(false);

            boolean result = notificationService.alreadySentToday(1L, NotificationType.DAILY_REMINDER);

            assertThat(result).isFalse();
        }
    }

    // ─── ensureSettingsExist Tests ───────────────────────────────────

    @Nested
    @DisplayName("ensureSettingsExist")
    class EnsureSettingsExistTests {

        @Test
        @DisplayName("Ayarlar varsa mevcut döner")
        void shouldReturnExistingSettings() {
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.of(testSettings));

            NotificationSettings result = notificationService.ensureSettingsExist(1L);

            assertThat(result).isEqualTo(testSettings);
            verify(settingsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ayarlar yoksa yeni oluşturulur")
        void shouldCreateNewSettings() {
            given(settingsRepository.findByUserId(1L)).willReturn(Optional.empty());
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(settingsRepository.save(any(NotificationSettings.class))).willAnswer(inv -> inv.getArgument(0));

            NotificationSettings result = notificationService.ensureSettingsExist(1L);

            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            verify(settingsRepository).save(any(NotificationSettings.class));
        }
    }

    // ─── WebSocket Tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("sendWebSocketNotification")
    class WebSocketTests {

        @Test
        @DisplayName("WebSocket hatası → sessizce yakalar, exception fırlatmaz")
        void shouldHandleWebSocketErrorGracefully() {
            NotificationResponse resp = new NotificationResponse(
                    1L, NotificationType.DAILY_REMINDER, "Test", "Msg", false, null, Instant.now());

            doThrow(new RuntimeException("WebSocket bağlantısı yok"))
                    .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

            assertThatNoException().isThrownBy(() ->
                    notificationService.sendWebSocketNotification(1L, resp));
        }
    }
}

