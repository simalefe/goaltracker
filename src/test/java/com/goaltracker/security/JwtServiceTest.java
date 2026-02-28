package com.goaltracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set values via reflection since @Value won't work in unit tests
        setField(jwtService, "secret", "mySecretKeyAtLeast256BitsLongForHS256AlgorithmGoalTracker2026");
        setField(jwtService, "accessExpiration", 900000L);
        setField(jwtService, "refreshExpiration", 604800000L);
    }

    @Test
    void generateAccessToken_shouldReturnValidJwt() {
        UserDetails user = createUser("test@example.com");
        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        UserDetails user = createUser("test@example.com");
        String token = jwtService.generateAccessToken(user);

        String email = jwtService.extractEmail(token);
        assertEquals("test@example.com", email);
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidToken() {
        UserDetails user = createUser("test@example.com");
        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forDifferentUser() {
        UserDetails user1 = createUser("test1@example.com");
        UserDetails user2 = createUser("test2@example.com");
        String token = jwtService.generateAccessToken(user1);

        assertFalse(jwtService.isTokenValid(token, user2));
    }

    @Test
    void isTokenExpired_shouldReturnFalse_forFreshToken() {
        UserDetails user = createUser("test@example.com");
        String token = jwtService.generateAccessToken(user);

        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void generateRefreshToken_shouldReturnUUID() {
        String token = jwtService.generateRefreshToken();
        assertNotNull(token);
        assertEquals(36, token.length()); // UUID format
    }

    @Test
    void hashToken_shouldReturn64CharHex() {
        String hash = jwtService.hashToken("test-token");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void hashToken_shouldBeConsistent() {
        String hash1 = jwtService.hashToken("same-token");
        String hash2 = jwtService.hashToken("same-token");
        assertEquals(hash1, hash2);
    }

    @Test
    void hashToken_shouldBeDifferentForDifferentInputs() {
        String hash1 = jwtService.hashToken("token-1");
        String hash2 = jwtService.hashToken("token-2");
        assertNotEquals(hash1, hash2);
    }

    private UserDetails createUser(String email) {
        return new User(email, "password", Collections.emptyList());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

