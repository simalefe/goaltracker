package com.goaltracker.scheduler;

import com.goaltracker.model.User;
import com.goaltracker.repository.UserRepository;
import com.goaltracker.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Her ayın 1'i saat 08:00'da geçen ayın raporunu oluşturur.
 * (Opsiyonel — raporu e-posta ile gönderme NotificationService/MailService entegrasyonu eklenebilir.)
 */
@Component
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ExportService exportService;
    private final UserRepository userRepository;

    public ReportScheduler(ExportService exportService, UserRepository userRepository) {
        this.exportService = exportService;
        this.userRepository = userRepository;
    }

    /**
     * Her ayın 1'i saat 08:00 — geçen ayın raporunu tüm aktif kullanıcılar için oluşturur.
     */
    @Scheduled(cron = "0 0 8 1 * *")
    public void generateMonthlyReports() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        int year = lastMonth.getYear();
        int month = lastMonth.getMonthValue();

        log.info("Aylık rapor oluşturuluyor: {}-{:02d}", year, month);

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .toList();

        int successCount = 0;
        int errorCount = 0;

        for (User user : activeUsers) {
            try {
                byte[] report = exportService.generateMonthlyReport(user.getId(), year, month);
                log.debug("Rapor oluşturuldu: userId={}, boyut={} bytes", user.getId(), report.length);
                // TODO: MailService ile PDF e-posta gönder
                successCount++;
            } catch (Exception e) {
                log.error("Rapor oluşturma hatası: userId={}", user.getId(), e);
                errorCount++;
            }
        }

        log.info("Aylık rapor tamamlandı: başarılı={}, hata={}", successCount, errorCount);
    }
}

