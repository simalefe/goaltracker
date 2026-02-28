package com.goaltracker.service;

import com.goaltracker.dto.request.RegisterRequest;
import com.goaltracker.dto.request.LoginRequest;
import com.goaltracker.dto.response.AuthResponse;
import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.exception.*;
import com.goaltracker.mapper.UserMapper;
import com.goaltracker.model.*;
import com.goaltracker.model.enums.Role;
import com.goaltracker.repository.*;
import com.goaltracker.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private static final int EMAIL_TOKEN_HOURS = 24;
    private static final int RESET_TOKEN_HOURS = 1;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException();
        }

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);
        user = userRepository.save(user);

        // Email verification token
        String verifyToken = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setUser(user);
        evt.setTokenHash(jwtService.hashToken(verifyToken));
        evt.setExpiresAt(Instant.now().plus(EMAIL_TOKEN_HOURS, ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(evt);

        mailService.sendVerificationEmail(user.getEmail(), verifyToken);

        // Generate tokens
        UserDetails userDetails = toUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user, response);

        log.info("Yeni kullanıcı kaydı: email={}, username={}", user.getEmail(), user.getUsername());
        return new AuthResponse(accessToken, UserMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Account lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            log.warn("Kilitli hesaba giriş denemesi: email={}", user.getEmail());
            throw new AccountLockedException();
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);
            if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
                log.warn("Hesap kilitlendi: email={}, failedAttempts={}", user.getEmail(), user.getFailedLoginCount());
            }
            userRepository.save(user);
            log.warn("Başarısız giriş: email={}, failedAttempts={}", user.getEmail(), user.getFailedLoginCount());
            throw new InvalidCredentialsException();
        }

        // Successful login — reset counters
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        UserDetails userDetails = toUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user, response);

        log.info("Başarılı giriş: email={}", user.getEmail());
        return new AuthResponse(accessToken, UserMapper.toResponse(user));
    }

    @Transactional
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshTokenFromCookie(request);
        if (rawToken == null) {
            throw new InvalidTokenException("Refresh token bulunamadı.");
        }

        String tokenHash = jwtService.hashToken(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Geçersiz refresh token."));

        // Theft detection: revoked token reuse
        if (storedToken.isRevoked()) {
            log.warn("Token theft tespiti! Tüm session'lar sonlandırılıyor. userId={}", storedToken.getUser().getId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            throw new InvalidTokenException("Güvenlik ihlali tespit edildi. Tüm oturumlar sonlandırıldı.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token süresi dolmuş.");
        }

        // Rotation: revoke old, create new
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        UserDetails userDetails = toUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        createRefreshToken(user, response);

        return newAccessToken;
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshTokenFromCookie(request);
        if (rawToken != null) {
            String tokenHash = jwtService.hashToken(rawToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevoked(true);
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            });
        }
        clearRefreshTokenCookie(response);
    }

    @Transactional
    public void verifyEmail(String token) {
        String tokenHash = jwtService.hashToken(token);
        EmailVerificationToken evt = emailVerificationTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Geçersiz veya kullanılmış doğrulama tokeni."));

        if (evt.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException();
        }

        User user = evt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        evt.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(evt);

        log.info("E-posta doğrulandı: email={}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(String email) {
        log.info("Şifre sıfırlama talebi: email={}", email);
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setTokenHash(jwtService.hashToken(rawToken));
            prt.setExpiresAt(Instant.now().plus(RESET_TOKEN_HOURS, ChronoUnit.HOURS));
            passwordResetTokenRepository.save(prt);
            mailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
        // Always return success — email enumeration protection
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = jwtService.hashToken(token);
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Geçersiz veya kullanılmış sıfırlama tokeni."));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException();
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(prt);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Şifre sıfırlandı: email={}", user.getEmail());
    }

    // --- Helper Methods ---

    private String createRefreshToken(User user, HttpServletResponse response) {
        String rawToken = jwtService.generateRefreshToken();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(jwtService.hashToken(rawToken));
        rt.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        refreshTokenRepository.save(rt);

        Cookie cookie = new Cookie("refreshToken", rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // dev: false, prod: true
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtService.getRefreshExpirationMs() / 1000));
        response.addCookie(cookie);

        return rawToken;
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}

