package com.goaltracker.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400 Bad Request
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Doğrulama hatası."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "İstek formatı hatalı."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Bu durum değişikliği yapılamaz."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "Token geçersiz."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Token süresi dolmuş."),
    ENTRY_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Kayıt tarihi hedef aralığı dışında."),
    GOAL_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "Bu hedefe kayıt eklenemez."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Oturum açmanız gerekiyor."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok."),
    GOAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bu hedefe erişim yetkiniz yok."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Hesabınız devre dışı bırakılmış."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "İstenen kaynak bulunamadı."),
    GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Hedef bulunamadı."),
    GOAL_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "İlerleme kaydı bulunamadı."),

    GOAL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Aktif hedef sınırına ulaştınız."),

    // Social
    SELF_FRIEND_REQUEST(HttpStatus.BAD_REQUEST, "Kendinize arkadaşlık isteği gönderemezsiniz."),
    ALREADY_FRIENDS(HttpStatus.BAD_REQUEST, "Zaten arkadaşsınız."),
    REQUEST_ALREADY_SENT(HttpStatus.BAD_REQUEST, "Zaten bekleyen bir istek var."),
    FRIENDSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "Arkadaşlık isteği bulunamadı."),
    NOT_FRIENDS(HttpStatus.BAD_REQUEST, "Bu kullanıcıyla arkadaş değilsiniz."),
    SHARE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Bu hedef zaten bu kullanıcıyla paylaşılmış."),
    SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "Paylaşım bulunamadı."),
    USER_BLOCKED(HttpStatus.FORBIDDEN, "Bu kullanıcı engellenmiş."),

    // 409 Conflict
    CONFLICT(HttpStatus.CONFLICT, "Bu kayıt zaten mevcut."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bu e-posta adresi zaten kayıtlı."),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bu kullanıcı adı zaten alınmış."),
    DUPLICATE_ENTRY(HttpStatus.CONFLICT, "Bu tarihte zaten kayıt mevcut."),
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "Bu hedef başka bir işlem tarafından güncellenmiş. Lütfen sayfayı yenileyip tekrar deneyin."),

    // 429 Too Many Requests
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Çok fazla istek, lütfen bekleyin."),

    // 500 Internal Server Error
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Bir hata oluştu, lütfen tekrar deneyin.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getDefaultMessage() { return defaultMessage; }
}

