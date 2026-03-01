package com.goaltracker.e2e.factory;

import java.time.LocalDate;
import java.util.UUID;

/**
 * E2E testleri için rastgele test verisi üretici.
 * Her test izole veri ile çalışır — çakışma olmaz.
 */
public final class TestDataFactory {

    private static final String DEFAULT_PASSWORD = "Test1234!";

    private TestDataFactory() {
        // Utility class
    }

    /**
     * Rastgele email üretir. Format: test-{uuid}@goaltracker.com
     */
    public static String randomEmail() {
        return "test-" + UUID.randomUUID() + "@goaltracker.com";
    }

    /**
     * Rastgele kullanıcı adı üretir. Format: user-{uuid8}
     */
    public static String randomUsername() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Sabit güvenli test şifresi döndürür.
     */
    public static String randomPassword() {
        return DEFAULT_PASSWORD;
    }

    /**
     * Rastgele görünen isim üretir. Format: Test User {uuid4}
     */
    public static String randomDisplayName() {
        return "Test User " + UUID.randomUUID().toString().substring(0, 4);
    }

    /**
     * Rastgele hedef başlığı üretir. Format: Hedef-{uuid4}
     */
    public static String randomGoalTitle() {
        return "Hedef-" + UUID.randomUUID().toString().substring(0, 4);
    }

    /**
     * Bugünün tarihini döndürür.
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Bugünden belirtilen gün sonrasının tarihini döndürür.
     */
    public static LocalDate futureDate(int daysFromNow) {
        return LocalDate.now().plusDays(daysFromNow);
    }

    /**
     * Bugünden belirtilen gün öncesinin tarihini döndürür.
     */
    public static LocalDate pastDate(int daysAgo) {
        return LocalDate.now().minusDays(daysAgo);
    }
}

