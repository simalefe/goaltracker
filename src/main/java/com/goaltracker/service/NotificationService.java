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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MailService mailService;

    public NotificationService(NotificationRepository notificationRepository,
                                NotificationSettingsRepository settingsRepository,
                                UserRepository userRepository,
                                SimpMessagingTemplate messagingTemplate,
                                MailService mailService) {
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.mailService = mailService;
    }

    @Transactional
    public NotificationResponse createNotification(Long userId, NotificationType type,
                                                     String title, String message,
                                                     Map<String, Object> metadata) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        if (metadata != null && !metadata.isEmpty()) {
            notification.setMetadata(metadata.toString());
        }
        notification.setSentAt(Instant.now());

        notification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification);

        // Send WebSocket notification
        sendWebSocketNotification(userId, response);

        // Send email if enabled
        NotificationSettings settings = getOrCreateSettings(userId);
        if (settings.isEmailEnabled() && shouldSendEmail(type)) {
            sendEmailNotification(user.getEmail(), title, message, type);
        }

        log.info("Bildirim oluşturuldu: userId={}, type={}, title={}", userId, type, title);
        return response;
    }

    public void sendWebSocketNotification(Long userId, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(), "/queue/notifications", notification);
            log.debug("WebSocket bildirimi gönderildi: userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket bildirimi gönderilemedi: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Async
    public void sendEmailNotification(String to, String title, String message, NotificationType type) {
        try {
            String subject = "GoalTracker Pro — " + title;
            String htmlBody = buildEmailHtml(title, message, type);
            mailService.sendNotificationEmail(to, subject, htmlBody);
            log.info("E-posta bildirimi gönderildi: to={}, type={}", to, type);
        } catch (Exception e) {
            log.error("E-posta bildirimi gönderilemedi: to={}, error={}", to, e.getMessage());
        }
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Bildirim bulunamadı: " + notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bu bildirime erişim yetkiniz yok.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Tüm bildirimler okundu olarak işaretlendi: userId={}", userId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByType(Long userId, NotificationType type, Pageable pageable) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long userId) {
        NotificationSettings settings = getOrCreateSettings(userId);
        return toSettingsResponse(settings);
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest request) {
        NotificationSettings settings = getOrCreateSettings(userId);

        if (request.getEmailEnabled() != null) settings.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) settings.setPushEnabled(request.getPushEnabled());
        if (request.getDailyReminderTime() != null) settings.setDailyReminderTime(request.getDailyReminderTime());
        if (request.getWeeklySummaryDay() != null) settings.setWeeklySummaryDay(request.getWeeklySummaryDay());
        if (request.getWeeklySummaryEnabled() != null) settings.setWeeklySummaryEnabled(request.getWeeklySummaryEnabled());
        if (request.getStreakDangerEnabled() != null) settings.setStreakDangerEnabled(request.getStreakDangerEnabled());

        settings = settingsRepository.save(settings);
        log.info("Bildirim ayarları güncellendi: userId={}", userId);
        return toSettingsResponse(settings);
    }

    @Transactional
    public NotificationSettings ensureSettingsExist(Long userId) {
        return getOrCreateSettings(userId);
    }

    /**
     * Check if a notification of this type was already sent today for this user.
     */
    @Transactional(readOnly = true)
    public boolean alreadySentToday(Long userId, NotificationType type) {
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        return notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(userId, type, todayStart);
    }

    // --- Private helpers ---

    private NotificationSettings getOrCreateSettings(Long userId) {
        return settingsRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));
            NotificationSettings ns = new NotificationSettings();
            ns.setUser(user);
            return settingsRepository.save(ns);
        });
    }

    private boolean shouldSendEmail(NotificationType type) {
        return type == NotificationType.DAILY_REMINDER
                || type == NotificationType.GOAL_COMPLETED
                || type == NotificationType.WEEKLY_SUMMARY
                || type == NotificationType.STREAK_LOST;
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getMetadata(),
                n.getCreatedAt()
        );
    }

    private NotificationSettingsResponse toSettingsResponse(NotificationSettings s) {
        return new NotificationSettingsResponse(
                s.isEmailEnabled(),
                s.isPushEnabled(),
                s.getDailyReminderTime(),
                s.getWeeklySummaryDay(),
                s.isWeeklySummaryEnabled(),
                s.isStreakDangerEnabled()
        );
    }

    private String buildEmailHtml(String title, String message, NotificationType type) {
        String color = switch (type) {
            case BADGE_EARNED, GOAL_COMPLETED -> "#198754";
            case STREAK_DANGER -> "#ffc107";
            case STREAK_LOST -> "#dc3545";
            default -> "#0d6efd";
        };

        return """
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h2 style="margin: 0;">%s</h2>
                </div>
                <div style="padding: 20px; background-color: #f8f9fa; border-radius: 0 0 8px 8px;">
                    <p style="font-size: 16px; color: #333;">%s</p>
                    <hr style="border: none; border-top: 1px solid #dee2e6;">
                    <p style="color: #888; font-size: 12px; text-align: center;">
                        GoalTracker Pro — Bildirim ayarlarınızı profil sayfanızdan yönetebilirsiniz.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(color, title, message);
    }
}

