package com.goaltracker.e2e.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * WebDriver factory — e2e.browser property'sine göre Chrome veya Firefox driver oluşturur.
 */
@Component
public class WebDriverConfig {

    @Value("${e2e.browser:chrome}")
    private String browser;

    @Value("${e2e.headless:true}")
    private boolean headless;

    @Value("${e2e.timeout.implicit:5}")
    private int implicitTimeout;

    /**
     * Yapılandırılmış browser'a göre yeni bir WebDriver instance oluşturur.
     */
    public WebDriver createDriver() {
        WebDriver driver = switch (browser.toLowerCase()) {
            case "firefox" -> createFirefoxDriver();
            default -> createChromeDriver();
        };

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitTimeout));
        return driver;
    }

    /**
     * Chrome options — headless, sandbox, window size ve download dizini ayarları.
     */
    public ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        // Download dizini (export testleri için)
        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = Path.of(System.getProperty("java.io.tmpdir"), "goaltracker-e2e-downloads")
                .toAbsolutePath().toString();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    /**
     * Firefox options — headless ve window size ayarları.
     */
    public FirefoxOptions getFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        options.addArguments("--width=1920");
        options.addArguments("--height=1080");

        return options;
    }

    private WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(getChromeOptions());
    }

    private WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        return new FirefoxDriver(getFirefoxOptions());
    }
}

