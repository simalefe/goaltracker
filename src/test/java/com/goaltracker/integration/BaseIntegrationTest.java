package com.goaltracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaltracker.dto.CreateGoalRequest;
import com.goaltracker.dto.request.LoginRequest;
import com.goaltracker.dto.request.RegisterRequest;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalFrequency;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.repository.*;
import com.goaltracker.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for all integration tests.
 * Uses real H2 database with Flyway migrations.
 * Only MailService is mocked (external SMTP dependency).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // Repositories for setup/verification
    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected GoalRepository goalRepository;

    @Autowired
    protected GoalEntryRepository goalEntryRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    protected PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    protected FriendshipRepository friendshipRepository;

    @Autowired
    protected GoalShareRepository goalShareRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected StreakRepository streakRepository;

    @Autowired
    protected NotificationSettingsRepository notificationSettingsRepository;

    @Autowired
    protected UserBadgeRepository userBadgeRepository;

    // Only mock external mail dependency
    @MockBean
    protected MailService mailService;

    @BeforeEach
    void cleanDatabase() {
        // Clean in proper order to respect foreign key constraints
        goalShareRepository.deleteAll();
        goalEntryRepository.deleteAll();
        streakRepository.deleteAll();
        notificationRepository.deleteAll();
        notificationSettingsRepository.deleteAll();
        friendshipRepository.deleteAll();
        userBadgeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        goalRepository.deleteAll();
        // Don't delete all users — keep demo user from migration
        // Instead delete all non-demo users
        userRepository.findAll().stream()
                .filter(u -> !"demo@goaltracker.local".equals(u.getEmail()))
                .forEach(u -> userRepository.delete(u));
    }

    // ========== HELPER METHODS ==========

    /**
     * Register a new user and return the access token.
     */
    protected String registerAndGetToken(String email, String username, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(email, username, password, username + " Display");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    /**
     * Login with existing credentials and return the access token.
     */
    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    /**
     * Create a standard goal via API and return the goal ID.
     */
    protected Long createGoalAndGetId(String token, String title) throws Exception {
        CreateGoalRequest goalRequest = new CreateGoalRequest();
        goalRequest.setTitle(title);
        goalRequest.setDescription("Test hedef açıklaması");
        goalRequest.setUnit("saat");
        goalRequest.setGoalType(GoalType.CUMULATIVE);
        goalRequest.setFrequency(GoalFrequency.DAILY);
        goalRequest.setTargetValue(new BigDecimal("100.00"));
        goalRequest.setStartDate(LocalDate.now());
        goalRequest.setEndDate(LocalDate.now().plusDays(30));
        goalRequest.setCategory(GoalCategory.EDUCATION);
        goalRequest.setColor("#FF5733");

        MvcResult result = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    /**
     * Create a goal with custom parameters.
     */
    protected Long createGoalAndGetId(String token, String title, GoalType goalType,
                                       BigDecimal targetValue, LocalDate startDate,
                                       LocalDate endDate, GoalCategory category) throws Exception {
        CreateGoalRequest goalRequest = new CreateGoalRequest();
        goalRequest.setTitle(title);
        goalRequest.setDescription("Özel hedef");
        goalRequest.setUnit("birim");
        goalRequest.setGoalType(goalType);
        goalRequest.setFrequency(GoalFrequency.DAILY);
        goalRequest.setTargetValue(targetValue);
        goalRequest.setStartDate(startDate);
        goalRequest.setEndDate(endDate);
        goalRequest.setCategory(category);

        MvcResult result = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    /**
     * Register a user and return their token. Convenience for multi-user tests.
     */
    protected UserTokenPair registerUser(String suffix) throws Exception {
        String email = suffix + "@test.com";
        String username = "user_" + suffix;
        String password = "Password123!";
        String token = registerAndGetToken(email, username, password);
        return new UserTokenPair(email, username, token);
    }

    /**
     * Simple record to hold user info + token.
     */
    protected record UserTokenPair(String email, String username, String token) {}

    protected static final String STRONG_PASSWORD = "Password123!";
}

