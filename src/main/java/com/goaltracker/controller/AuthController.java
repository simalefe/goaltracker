package com.goaltracker.controller;

import com.goaltracker.dto.request.*;
import com.goaltracker.dto.response.AuthResponse;
import com.goaltracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ---- LOGIN ----
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("errorMessage", "E-posta veya şifre hatalı.");
        if (logout != null) model.addAttribute("successMessage", "Başarıyla çıkış yaptınız.");
        model.addAttribute("loginForm", new LoginRequest("", ""));
        return "auth/login";
    }

    @PostMapping("/login-custom")
    public String loginSubmit(@Valid @ModelAttribute("loginForm") LoginRequest loginForm,
                              BindingResult bindingResult,
                              HttpServletResponse response,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        try {
            AuthResponse authResponse = authService.login(loginForm, response);
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("loginForm", loginForm);
            return "auth/login";
        }
    }

    // ---- REGISTER ----
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterRequest("", "", "", ""));
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerForm") RegisterRequest registerForm,
                                 BindingResult bindingResult,
                                 HttpServletResponse response,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(registerForm, response);
            return "redirect:/auth/email-verification-sent";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("registerForm", registerForm);
            return "auth/register";
        }
    }

    @GetMapping("/email-verification-sent")
    public String emailVerificationSent() {
        return "auth/email-verification-sent";
    }

    // ---- EMAIL VERIFICATION ----
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            authService.verifyEmail(token);
            model.addAttribute("successMessage", "E-posta adresiniz başarıyla doğrulandı.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/verify-email";
    }

    // ---- FORGOT PASSWORD ----
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotForm", new ForgotPasswordRequest(""));
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@Valid @ModelAttribute("forgotForm") ForgotPasswordRequest forgotForm,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        authService.forgotPassword(forgotForm.email());
        model.addAttribute("successMessage",
                "Eğer bu e-posta adresi kayıtlıysa, şifre sıfırlama bağlantısı gönderildi.");
        return "auth/forgot-password";
    }

    // ---- RESET PASSWORD ----
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("resetForm", new ResetPasswordRequest(token, ""));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@Valid @ModelAttribute("resetForm") ResetPasswordRequest resetForm,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("token", resetForm.token());
            return "auth/reset-password";
        }
        try {
            authService.resetPassword(resetForm.token(), resetForm.newPassword());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Şifreniz başarıyla sıfırlandı. Giriş yapabilirsiniz.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("token", resetForm.token());
            return "auth/reset-password";
        }
    }
}

