package com.goaltracker.e2e.util;

import io.qameta.allure.Allure;

/**
 * Allure raporlama yardımcıları.
 */
public final class AllureUtils {

    private AllureUtils() {
        // Utility class
    }

    /**
     * Test açıklamasını Allure raporuna ekler.
     */
    public static void addDescription(String description) {
        Allure.description(description);
    }

    /**
     * Allure raporuna bir adım ekler.
     */
    public static void addStep(String stepName) {
        Allure.step(stepName);
    }

    /**
     * Allure raporuna bir link ekler.
     */
    public static void addLink(String name, String url) {
        Allure.link(name, url);
    }
}

