package com.goaltracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.goaltracker.dto.CreateGoalRequest;
import com.goaltracker.dto.StatusUpdateRequest;
import com.goaltracker.dto.UpdateGoalRequest;
import com.goaltracker.model.Goal;
import com.goaltracker.model.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Goal CRUD operations.
 * Full lifecycle: HTTP → Controller → Service → Repository → H2 DB
 */
class GoalCrudIntegrationTest extends BaseIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndGetToken("goaluser@test.com", "goaluser", STRONG_PASSWORD);
    }

    // ========================================================================
    // CREATE GOAL
    // ========================================================================

    @Nested
    @DisplayName("Hedef Oluşturma (POST /api/goals)")
    class CreateGoalTests {

        @Test
        @DisplayName("Başarılı hedef oluşturma — 201, tüm alanlar doğru")
        void createGoal_success() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("Günde 2 saat kitap oku");
            request.setDescription("Kişisel gelişim için okuma hedefi");
            request.setUnit("saat");
            request.setGoalType(GoalType.CUMULATIVE);
            request.setFrequency(GoalFrequency.DAILY);
            request.setTargetValue(new BigDecimal("60.00"));
            request.setStartDate(LocalDate.now());
            request.setEndDate(LocalDate.now().plusDays(30));
            request.setCategory(GoalCategory.EDUCATION);
            request.setColor("#3498DB");

            MvcResult result = mockMvc.perform(post("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Günde 2 saat kitap oku"))
                    .andExpect(jsonPath("$.data.description").value("Kişisel gelişim için okuma hedefi"))
                    .andExpect(jsonPath("$.data.unit").value("saat"))
                    .andExpect(jsonPath("$.data.goalType").value("CUMULATIVE"))
                    .andExpect(jsonPath("$.data.frequency").value("DAILY"))
                    .andExpect(jsonPath("$.data.targetValue").value(60.00))
                    .andExpect(jsonPath("$.data.category").value("EDUCATION"))
                    .andExpect(jsonPath("$.data.color").value("#3498DB"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                    .andReturn();

            // DB verification
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            Long goalId = root.path("data").path("id").asLong();
            Goal goal = goalRepository.findById(goalId).orElseThrow();
            assertThat(goal.getTitle()).isEqualTo("Günde 2 saat kitap oku");
            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
            assertThat(goal.getTargetValue()).isEqualByComparingTo(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("Validation hatası — başlık boş → 400")
        void createGoal_emptyTitle() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("");
            request.setUnit("birim");
            request.setGoalType(GoalType.DAILY);
            request.setTargetValue(new BigDecimal("10.00"));
            request.setStartDate(LocalDate.now());
            request.setEndDate(LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Bitiş tarihi başlangıçtan önce → 400")
        void createGoal_endDateBeforeStartDate() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("Geçersiz tarih hedefi");
            request.setUnit("birim");
            request.setGoalType(GoalType.DAILY);
            request.setTargetValue(new BigDecimal("10.00"));
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now());

            mockMvc.perform(post("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Hedef değeri sıfır veya negatif → 400")
        void createGoal_invalidTargetValue() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("Hedef");
            request.setUnit("birim");
            request.setGoalType(GoalType.DAILY);
            request.setTargetValue(new BigDecimal("0.00"));
            request.setStartDate(LocalDate.now());
            request.setEndDate(LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Farklı GoalType'lar ile oluşturma — DAILY, CUMULATIVE, RATE")
        void createGoal_allTypes() throws Exception {
            for (GoalType type : GoalType.values()) {
                CreateGoalRequest request = new CreateGoalRequest();
                request.setTitle("Hedef " + type.name());
                request.setUnit("birim");
                request.setGoalType(type);
                request.setTargetValue(new BigDecimal("50.00"));
                request.setStartDate(LocalDate.now());
                request.setEndDate(LocalDate.now().plusDays(30));
                request.setCategory(GoalCategory.HEALTH);

                mockMvc.perform(post("/api/goals")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.goalType").value(type.name()));
            }
        }
    }

    // ========================================================================
    // GET GOAL
    // ========================================================================

    @Nested
    @DisplayName("Hedef Getirme (GET /api/goals/{id})")
    class GetGoalTests {

        @Test
        @DisplayName("Mevcut hedefi getirme — 200, tüm alanlar mevcut")
        void getGoal_success() throws Exception {
            Long goalId = createGoalAndGetId(token, "Test Hedefi");

            mockMvc.perform(get("/api/goals/" + goalId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(goalId))
                    .andExpect(jsonPath("$.data.title").value("Test Hedefi"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.targetValue").isNumber())
                    .andExpect(jsonPath("$.data.startDate").isNotEmpty())
                    .andExpect(jsonPath("$.data.endDate").isNotEmpty())
                    .andExpect(jsonPath("$.data.version").isNumber());
        }

        @Test
        @DisplayName("Olmayan hedefi getirme — 404")
        void getGoal_notFound() throws Exception {
            mockMvc.perform(get("/api/goals/99999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("GOAL_NOT_FOUND"));
        }
    }

    // ========================================================================
    // LIST & FILTER GOALS
    // ========================================================================

    @Nested
    @DisplayName("Hedef Listesi (GET /api/goals)")
    class ListGoalsTests {

        @Test
        @DisplayName("Tüm hedefleri listele — pagination doğru")
        void listGoals_success() throws Exception {
            // Create 3 goals
            createGoalAndGetId(token, "Hedef 1");
            createGoalAndGetId(token, "Hedef 2");
            createGoalAndGetId(token, "Hedef 3");

            mockMvc.perform(get("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(3))
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.page").value(0));
        }

        @Test
        @DisplayName("Statüye göre filtrele — ACTIVE")
        void listGoals_filterByStatus() throws Exception {
            Long goalId1 = createGoalAndGetId(token, "Aktif Hedef");
            Long goalId2 = createGoalAndGetId(token, "Duraklatılacak Hedef");

            // Pause one goal
            StatusUpdateRequest pauseReq = new StatusUpdateRequest();
            pauseReq.setNewStatus(GoalStatus.PAUSED);
            mockMvc.perform(patch("/api/goals/" + goalId2 + "/status")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pauseReq)));

            // Filter by ACTIVE
            mockMvc.perform(get("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Kategoriye göre filtrele")
        void listGoals_filterByCategory() throws Exception {
            createGoalAndGetId(token, "Sağlık Hedefi", GoalType.DAILY,
                    new BigDecimal("30"), LocalDate.now(), LocalDate.now().plusDays(30), GoalCategory.HEALTH);
            createGoalAndGetId(token, "Eğitim Hedefi", GoalType.DAILY,
                    new BigDecimal("30"), LocalDate.now(), LocalDate.now().plusDays(30), GoalCategory.EDUCATION);

            mockMvc.perform(get("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .param("category", "HEALTH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].category").value("HEALTH"));
        }

        @Test
        @DisplayName("Arama sorgusu ile filtrele")
        void listGoals_filterByQuery() throws Exception {
            createGoalAndGetId(token, "Kitap Okuma Hedefi");
            createGoalAndGetId(token, "Spor Yapma Hedefi");

            mockMvc.perform(get("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .param("query", "Kitap"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Kitap Okuma Hedefi"));
        }

        @Test
        @DisplayName("Pagination — sayfa 2")
        void listGoals_pagination() throws Exception {
            for (int i = 1; i <= 5; i++) {
                createGoalAndGetId(token, "Hedef " + i);
            }

            mockMvc.perform(get("/api/goals")
                            .header("Authorization", "Bearer " + token)
                            .param("size", "2")
                            .param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(5))
                    .andExpect(jsonPath("$.data.totalPages").value(3));
        }
    }

    // ========================================================================
    // UPDATE GOAL
    // ========================================================================

    @Nested
    @DisplayName("Hedef Güncelleme (PUT /api/goals/{id})")
    class UpdateGoalTests {

        @Test
        @DisplayName("Başarılı güncelleme — 200, DB'de güncelleme yansır")
        void updateGoal_success() throws Exception {
            Long goalId = createGoalAndGetId(token, "Eski Başlık");

            UpdateGoalRequest updateRequest = new UpdateGoalRequest();
            updateRequest.setTitle("Yeni Başlık");
            updateRequest.setDescription("Güncellenmiş açıklama");
            updateRequest.setTargetValue(new BigDecimal("200.00"));
            updateRequest.setCategory(GoalCategory.FITNESS);

            mockMvc.perform(put("/api/goals/" + goalId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Yeni Başlık"))
                    .andExpect(jsonPath("$.data.description").value("Güncellenmiş açıklama"))
                    .andExpect(jsonPath("$.data.targetValue").value(200.00))
                    .andExpect(jsonPath("$.data.category").value("FITNESS"));

            // DB verification
            Goal goal = goalRepository.findById(goalId).orElseThrow();
            assertThat(goal.getTitle()).isEqualTo("Yeni Başlık");
            assertThat(goal.getTargetValue()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(goal.getCategory()).isEqualTo(GoalCategory.FITNESS);
        }

        @Test
        @DisplayName("Olmayan hedefi güncelleme — 404")
        void updateGoal_notFound() throws Exception {
            UpdateGoalRequest updateRequest = new UpdateGoalRequest();
            updateRequest.setTitle("Yeni Başlık");

            mockMvc.perform(put("/api/goals/99999")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // DELETE GOAL
    // ========================================================================

    @Nested
    @DisplayName("Hedef Silme (DELETE /api/goals/{id})")
    class DeleteGoalTests {

        @Test
        @DisplayName("Başarılı silme — 204, DB'de kayıt yok")
        void deleteGoal_success() throws Exception {
            Long goalId = createGoalAndGetId(token, "Silinecek Hedef");

            mockMvc.perform(delete("/api/goals/" + goalId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            // DB verification
            assertThat(goalRepository.findById(goalId)).isEmpty();
        }

        @Test
        @DisplayName("Olmayan hedefi silme — 404")
        void deleteGoal_notFound() throws Exception {
            mockMvc.perform(delete("/api/goals/99999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // STATUS UPDATE
    // ========================================================================

    @Nested
    @DisplayName("Durum Güncelleme (PATCH /api/goals/{id}/status)")
    class StatusUpdateTests {

        @Test
        @DisplayName("ACTIVE → PAUSED geçişi başarılı")
        void statusUpdate_activeToPaused() throws Exception {
            Long goalId = createGoalAndGetId(token, "Duraklatılacak Hedef");

            StatusUpdateRequest statusReq = new StatusUpdateRequest();
            statusReq.setNewStatus(GoalStatus.PAUSED);

            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PAUSED"));

            Goal goal = goalRepository.findById(goalId).orElseThrow();
            assertThat(goal.getStatus()).isEqualTo(GoalStatus.PAUSED);
        }

        @Test
        @DisplayName("ACTIVE → COMPLETED geçişi başarılı")
        void statusUpdate_activeToCompleted() throws Exception {
            Long goalId = createGoalAndGetId(token, "Tamamlanacak Hedef");

            StatusUpdateRequest statusReq = new StatusUpdateRequest();
            statusReq.setNewStatus(GoalStatus.COMPLETED);

            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("PAUSED → ACTIVE geçişi başarılı")
        void statusUpdate_pausedToActive() throws Exception {
            Long goalId = createGoalAndGetId(token, "Devam Ettirilecek Hedef");

            // First pause
            StatusUpdateRequest pauseReq = new StatusUpdateRequest();
            pauseReq.setNewStatus(GoalStatus.PAUSED);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pauseReq)));

            // Then resume
            StatusUpdateRequest resumeReq = new StatusUpdateRequest();
            resumeReq.setNewStatus(GoalStatus.ACTIVE);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resumeReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Geçersiz durum geçişi (ARCHIVED → ACTIVE) — 400")
        void statusUpdate_invalidTransition() throws Exception {
            Long goalId = createGoalAndGetId(token, "Arşivlenecek Hedef");

            // ACTIVE → ARCHIVED
            StatusUpdateRequest archiveReq = new StatusUpdateRequest();
            archiveReq.setNewStatus(GoalStatus.ARCHIVED);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(archiveReq)));

            // ARCHIVED → ACTIVE (invalid)
            StatusUpdateRequest reactivateReq = new StatusUpdateRequest();
            reactivateReq.setNewStatus(GoalStatus.ACTIVE);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reactivateReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
        }

        @Test
        @DisplayName("COMPLETED → ARCHIVED geçişi başarılı")
        void statusUpdate_completedToArchived() throws Exception {
            Long goalId = createGoalAndGetId(token, "Arşivlenecek Tamamlanan");

            // ACTIVE → COMPLETED
            StatusUpdateRequest completeReq = new StatusUpdateRequest();
            completeReq.setNewStatus(GoalStatus.COMPLETED);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeReq)));

            // COMPLETED → ARCHIVED
            StatusUpdateRequest archiveReq = new StatusUpdateRequest();
            archiveReq.setNewStatus(GoalStatus.ARCHIVED);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(archiveReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        }
    }
}

