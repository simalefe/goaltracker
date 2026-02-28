package com.goaltracker.scheduler;

import com.goaltracker.service.StreakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StreakScheduler {

    private static final Logger log = LoggerFactory.getLogger(StreakScheduler.class);

    private final StreakService streakService;

    public StreakScheduler(StreakService streakService) {
        this.streakService = streakService;
    }

    /**
     * Her gece 00:01 — ACTIVE hedeflerin stale streak'lerini sıfırla.
     * Dün entry girilmemiş hedeflerin streak'i 0 olur.
     * PAUSED hedeflerin streak'i değişmez (dondurulmuş).
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void resetStaleStreaks() {
        log.info("Streak sıfırlama scheduler'ı başlatıldı.");
        try {
            int count = streakService.resetStaleStreaks(LocalDate.now());
            log.info("Streak sıfırlama tamamlandı: {} streak sıfırlandı.", count);
        } catch (Exception e) {
            log.error("Streak sıfırlama sırasında hata: {}", e.getMessage(), e);
        }
    }
}

