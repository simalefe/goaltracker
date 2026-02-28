package com.goaltracker.integration;

import com.goaltracker.dto.CreateEntryRequest;
import com.goaltracker.dto.StatusUpdateRequest;
import com.goaltracker.dto.UpdateGoalRequest;
import com.goaltracker.model.enums.GoalStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests that users can only access their own resources.
 * Two separate users are created and cross-access is verified.
 */
class GoalAuthorizationIntegrationTest extends BaseIntegrationTest {

    private UserTokenPair userA;
    private UserTokenPair userB;
    private Long userAGoalId;

    @BeforeEach
    void setUp() throws Exception {
        userA = registerUser("alice");
        userB = registerUser("bob");
        userAGoalId = createGoalAndGetId(userA.token(), "Alice'in Hedefi");
    }

    // ========================================================================
    // CROSS-USER ACCESS TESTS
    // ========================================================================

    @Test
    @DisplayName("Başka kullanıcının hedefini GET ile görüntüleme → 403/404")
    void getGoal_otherUser_denied() throws Exception {
        mockMvc.perform(get("/api/goals/" + userAGoalId)
                        .header("Authorization", "Bearer " + userB.token()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Başka kullanıcının hedefini PUT ile güncelleme → 403/404")
    void updateGoal_otherUser_denied() throws Exception {
        UpdateGoalRequest updateRequest = new UpdateGoalRequest();
        updateRequest.setTitle("Hacker Başlık");

        mockMvc.perform(put("/api/goals/" + userAGoalId)
                        .header("Authorization", "Bearer " + userB.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Başka kullanıcının hedefini DELETE ile silme → 403/404")
    void deleteGoal_otherUser_denied() throws Exception {
        mockMvc.perform(delete("/api/goals/" + userAGoalId)
                        .header("Authorization", "Bearer " + userB.token()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Başka kullanıcının hedefinin durumunu değiştirme → 403/404")
    void updateStatus_otherUser_denied() throws Exception {
        StatusUpdateRequest statusReq = new StatusUpdateRequest();
        statusReq.setNewStatus(GoalStatus.COMPLETED);

        mockMvc.perform(patch("/api/goals/" + userAGoalId + "/status")
                        .header("Authorization", "Bearer " + userB.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Başka kullanıcının hedefine entry ekleme → 403/404")
    void createEntry_otherUser_denied() throws Exception {
        CreateEntryRequest entryRequest = new CreateEntryRequest(
                LocalDate.now(), new BigDecimal("5.0"), "Hack entry");

        mockMvc.perform(post("/api/goals/" + userAGoalId + "/entries")
                        .header("Authorization", "Bearer " + userB.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Başka kullanıcının hedef entry'lerini listeleme → 403/404")
    void getEntries_otherUser_denied() throws Exception {
        mockMvc.perform(get("/api/goals/" + userAGoalId + "/entries")
                        .header("Authorization", "Bearer " + userB.token()))
                .andExpect(status().is4xxClientError());
    }

    // ========================================================================
    // USER ISOLATION — Her kullanıcı sadece kendi hedeflerini görür
    // ========================================================================

    @Test
    @DisplayName("Kullanıcı A sadece kendi hedeflerini listeler, B'ninkiler görünmez")
    void listGoals_isolation() throws Exception {
        createGoalAndGetId(userB.token(), "Bob'un Hedefi");

        // User A should only see 1 goal (their own)
        mockMvc.perform(get("/api/goals")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Alice'in Hedefi"));

        // User B should only see 1 goal (their own)
        mockMvc.perform(get("/api/goals")
                        .header("Authorization", "Bearer " + userB.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Bob'un Hedefi"));
    }

    // ========================================================================
    // UNAUTHENTICATED ACCESS
    // ========================================================================

    @Test
    @DisplayName("Token olmadan API erişimi → redirect (302)")
    void noAuth_redirected() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Token olmadan hedef oluşturma → redirect")
    void noAuth_createGoal() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is3xxRedirection());
    }

    // ========================================================================
    // PROFILE ISOLATION
    // ========================================================================

    @Test
    @DisplayName("Kullanıcı A sadece kendi profilini görebilir")
    void getProfile_showsOwnData() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(userA.email()))
                .andExpect(jsonPath("$.data.username").value(userA.username()));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + userB.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(userB.email()))
                .andExpect(jsonPath("$.data.username").value(userB.username()));
    }
}

