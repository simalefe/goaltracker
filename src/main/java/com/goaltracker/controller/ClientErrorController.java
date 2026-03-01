package com.goaltracker.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tarayıcı tarafında yakalanan JavaScript hatalarını
 * sunucu console'una (log) iletir.
 *
 * <p>Endpoint: POST /api/client-error</p>
 * <p>Payload örneği:
 * <pre>
 * {
 *   "message": "Cannot read property 'x' of undefined",
 *   "source":  "app.js",
 *   "line":    42,
 *   "col":     7,
 *   "stack":   "TypeError: ...",
 *   "url":     "/goals"
 * }
 * </pre>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/client-error")
public class ClientErrorController {

    /**
     * Tarayıcıdan gelen JS hatası payload'unu loglar.
     * Güvenlik: anonim erişime açık (SecurityConfig'de permit yapılmalı),
     * çünkü login olmamış kullanıcı da hata üretebilir.
     */
    @PostMapping
    public ResponseEntity<Void> logClientError(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        String message = getString(payload, "message");
        String source  = getString(payload, "source");
        String url     = getString(payload, "url");
        Object line    = payload.get("line");
        Object col     = payload.get("col");
        String stack   = getString(payload, "stack");
        String userIp  = request.getRemoteAddr();

        log.error("[CLIENT-JS-ERROR] ip={} url={} source={}:{}:{} message={}\n{}",
                userIp, url, source, line, col, message, stack);

        return ResponseEntity.noContent().build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}

