package com.goaltracker.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /auth/login → 200 login sayfası")
    void loginPage_shouldReturn200() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /auth/register → 200 kayıt sayfası")
    void registerPage_shouldReturn200() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("GET /auth/forgot-password → 200")
    void forgotPasswordPage_shouldReturn200() throws Exception {
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"));
    }

    @Test
    @DisplayName("GET /dashboard (unauthorized) → login'e redirect")
    void dashboard_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("GET /auth/email-verification-sent → 200")
    void emailVerificationSentPage_shouldReturn200() throws Exception {
        mockMvc.perform(get("/auth/email-verification-sent"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/email-verification-sent"));
    }

    @Test
    @DisplayName("GET /auth/login?error=true → hata mesajı gösteriliyor")
    void loginPage_shouldShowErrorMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /auth/login?logout → başarı mesajı gösteriliyor")
    void loginPage_shouldShowLogoutMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("GET /goals (unauthorized) → login'e redirect")
    void goalsPage_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/goals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("GET /profile (unauthorized) → login'e redirect")
    void profilePage_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("GET /notifications (unauthorized) → login'e redirect")
    void notificationsPage_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }
}

