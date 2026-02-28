# GoalTracker Pro 🎯

Kişisel hedef takip ve ilerleme yönetimi uygulaması. Spring Boot + Thymeleaf + H2 ile geliştirilmiştir.

## ✨ Özellikler

- **Hedef Yönetimi:** CRUD, durum geçişleri (ACTIVE, PAUSED, COMPLETED, ARCHIVED)
- **İlerleme Takibi:** Günlük giriş, kümülatif ve oran bazlı hedef tipleri
- **Grafikler & Dashboard:** İlerleme grafikleri, dashboard özeti
- **Streak & Rozetler:** Ardışık gün takibi, otomatik rozet kazanımı
- **Bildirimler:** Günlük hatırlatma, streak uyarıları, WebSocket gerçek zamanlı bildirim
- **Sosyal:** Arkadaşlık, hedef paylaşımı, liderlik tablosu, aktivite akışı
- **Dışa Aktarma:** Excel (3 sayfa), PDF, CSV (UTF-8 BOM)
- **Güvenlik:** JWT + Cookie, BCrypt(12), rate limiting, OWASP uyumlu

## 🛠 Teknolojiler

| Katman | Teknoloji |
|--------|-----------|
| Backend | Java 21, Spring Boot 3.3.7 |
| Şablon | Thymeleaf + Bootstrap 5.3 |
| Veritabanı | H2 (in-memory / file-based) |
| Migration | Flyway |
| Güvenlik | Spring Security, JWT (jjwt 0.12.6) |
| Cache | Caffeine |
| WebSocket | Spring WebSocket + STOMP |
| Export | Apache POI, iText 7 |
| Test | JUnit 5, Mockito, MockMvc |
| Coverage | JaCoCo |

## 🚀 Hızlı Başlangıç

### Gereksinimler
- Java 21+
- Maven 3.9+ (veya dahili `mvnw` kullanılabilir)

### 1. Projeyi klonlayın
```bash
git clone https://github.com/your-username/goaltracker.git
cd goaltracker
```

### 2. Ortam değişkenlerini ayarlayın
```bash
cp .env.example .env
# .env dosyasını düzenleyin (özellikle JWT_SECRET)
```

### 3. Uygulamayı çalıştırın
```bash
# Geliştirme profili ile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Uygulama varsayılan olarak http://localhost:8080 adresinde çalışır.

### 4. H2 Console (sadece dev profili)
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:goaltracker`
- Username: `sa` / Password: *(boş)*

## 🧪 Testler

### Tüm testleri çalıştır
```bash
./mvnw clean test
```

### Coverage raporu oluştur
```bash
./mvnw clean verify
# Rapor: target/site/jacoco/index.html
```

### Test hedefleri
| Katman | Hedef |
|--------|-------|
| Service (Unit) | %90 |
| Controller (Entegrasyon) | %75 |
| Thymeleaf MVC | %70 |

## 📁 Proje Yapısı

```
src/
├── main/
│   ├── java/com/goaltracker/
│   │   ├── config/          # SecurityConfig, WebSocketConfig, CacheConfig
│   │   ├── controller/      # MVC + REST controller'lar
│   │   ├── dto/             # Request/Response DTO'lar
│   │   ├── exception/       # Custom exception'lar + GlobalExceptionHandler
│   │   ├── mapper/          # Entity ↔ DTO dönüşümleri
│   │   ├── model/           # JPA entity'leri
│   │   ├── repository/      # Spring Data JPA repository'leri
│   │   ├── scheduler/       # Zamanlanmış görevler
│   │   ├── security/        # JWT filter, JwtService
│   │   ├── service/         # İş mantığı katmanı
│   │   ├── util/            # GoalCalculator, SecurityUtils
│   │   └── validation/      # Custom validator'lar
│   └── resources/
│       ├── application.yml          # Ana yapılandırma
│       ├── application-dev.yml      # Geliştirme profili
│       ├── application-prod.yml     # Üretim profili
│       ├── db/migration/            # Flyway migration dosyaları
│       ├── static/                  # CSS, JS, görseller
│       └── templates/               # Thymeleaf şablonları
└── test/
    └── java/com/goaltracker/       # JUnit 5 + Mockito testleri
```

## ⚙️ Ortam Değişkenleri

| Değişken | Açıklama | Zorunlu | Varsayılan |
|----------|----------|---------|------------|
| `JWT_SECRET` | JWT imzalama anahtarı (≥32 karakter) | ✅ (prod) | dev fallback |
| `JWT_ACCESS_EXP` | Access token süresi (ms) | ❌ | 900000 (15dk) |
| `JWT_REFRESH_EXP` | Refresh token süresi (ms) | ❌ | 604800000 (7gün) |
| `MAIL_HOST` | SMTP sunucu adresi | ❌ | smtp.gmail.com |
| `MAIL_PORT` | SMTP port | ❌ | 587 |
| `MAIL_USERNAME` | SMTP kullanıcı adı | ❌ | — |
| `MAIL_PASSWORD` | SMTP şifresi | ❌ | — |
| `APP_BASE_URL` | Uygulama base URL | ❌ | http://localhost:8080 |
| `DATABASE_URL` | JDBC URL | ❌ | H2 in-memory |
| `SERVER_PORT` | Sunucu portu | ❌ | 8080 |
| `SPRING_PROFILES_ACTIVE` | Aktif profil (dev/prod) | ❌ | — |

## 🔒 Güvenlik

- BCrypt strength = 12
- JWT + HttpOnly cookie refresh token
- Refresh token rotation (tek kullanımlık)
- Token theft detection
- Account lockout (5 başarısız deneme → 15 dk kilit)
- Rate limiting (Bucket4j)
- Tüm endpoint'lerde ownership kontrolü
- Parameterized JPA queries (SQL injection koruması)
- CORS whitelist
- Stack trace response'da gizli (prod)
- H2 console prod'da kapalı

## 🏗 Üretim Dağıtımı

```bash
# JAR oluştur
./mvnw clean package -Pprod

# Çalıştır
java -jar target/goaltracker-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 📄 Lisans

Bu proje eğitim amaçlı geliştirilmiştir.

