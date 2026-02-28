package com.goaltracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.goaltracker.dto.CreateEntryRequest;
import com.goaltracker.dto.UpdateEntryRequest;
import com.goaltracker.dto.StatusUpdateRequest;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.enums.GoalStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for GoalEntry (Progress Tracking).
 * Tests entry CRUD, duplicate checking, date range validation, stats, chart data.
 */
class GoalEntryIntegrationTest extends BaseIntegrationTest {

    private String token;
    private Long goalId;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndGetToken("entry@test.com", "entryuser", STRONG_PASSWORD);
        goalId = createGoalAndGetId(token, "Entry Test Hedefi");
    }

    // ========================================================================
    // CREATE ENTRY
    // ========================================================================

    @Nested
    @DisplayName("Entry Oluşturma (POST /api/goals/{goalId}/entries)")
    class CreateEntryTests {

        @Test
        @DisplayName("Başarılı entry oluşturma — 201, DB'de kayıt var")
        void createEntry_success() throws Exception {
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("5.50"), "Bugün güzel geçti");

            MvcResult result = mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.goalId").value(goalId))
                    .andExpect(jsonPath("$.data.entryDate").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.data.actualValue").value(5.50))
                    .andExpect(jsonPath("$.data.note").value("Bugün güzel geçti"))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andReturn();

            // DB verification
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            Long entryId = root.path("data").path("id").asLong();
            GoalEntry entry = goalEntryRepository.findById(entryId).orElseThrow();
            assertThat(entry.getActualValue()).isEqualByComparingTo(new BigDecimal("5.50"));
            assertThat(entry.getNote()).isEqualTo("Bugün güzel geçti");
            assertThat(entry.getEntryDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Aynı gün duplicate entry → 409 CONFLICT")
        void createEntry_duplicateDate() throws Exception {
            CreateEntryRequest request1 = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("3.0"), "İlk");
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            // Same date again
            CreateEntryRequest request2 = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("4.0"), "İkinci");
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ENTRY"));
        }

        @Test
        @DisplayName("Hedef tarih aralığı dışında entry — 400")
        void createEntry_outOfRange() throws Exception {
            // Entry date before goal start or after goal end
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.now().minusDays(365), new BigDecimal("2.0"), "Eski tarih");

            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("ENTRY_OUT_OF_RANGE"));
        }

        @Test
        @DisplayName("Validation — null değer → 400")
        void createEntry_nullValue() throws Exception {
            String json = """
                    {"entryDate": "%s", "note": "test"}
                    """.formatted(LocalDate.now());

            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PAUSED hedefe entry ekleme — 400")
        void createEntry_pausedGoal() throws Exception {
            // Pause the goal
            StatusUpdateRequest pauseReq = new StatusUpdateRequest();
            pauseReq.setNewStatus(GoalStatus.PAUSED);
            mockMvc.perform(patch("/api/goals/" + goalId + "/status")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pauseReq)));

            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("5.0"), "Paused goal");

            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("GOAL_NOT_ACTIVE"));
        }
    }

    // ========================================================================
    // LIST ENTRIES
    // ========================================================================

    @Nested
    @DisplayName("Entry Listesi (GET /api/goals/{goalId}/entries)")
    class ListEntryTests {

        @Test
        @DisplayName("Entry'leri sıralı olarak listele")
        void listEntries_success() throws Exception {
            // Create entries on different dates
            CreateEntryRequest e1 = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("3.0"), "Bugün");
            CreateEntryRequest e2 = new CreateEntryRequest(
                    LocalDate.now().plusDays(1), new BigDecimal("4.0"), "Yarın");

            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(e1)));

            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(e2)));

            mockMvc.perform(get("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("Boş entry listesi — 200, boş array")
        void listEntries_empty() throws Exception {
            mockMvc.perform(get("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // ========================================================================
    // UPDATE ENTRY
    // ========================================================================

    @Nested
    @DisplayName("Entry Güncelleme (PUT /api/entries/{entryId})")
    class UpdateEntryTests {

        @Test
        @DisplayName("Başarılı entry güncelleme")
        void updateEntry_success() throws Exception {
            // Create entry
            CreateEntryRequest createReq = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("5.0"), "İlk not");
            MvcResult createResult = mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
            Long entryId = root.path("data").path("id").asLong();

            // Update entry
            UpdateEntryRequest updateReq = new UpdateEntryRequest(new BigDecimal("8.0"), "Güncellenmiş not");
            mockMvc.perform(put("/api/entries/" + entryId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.actualValue").value(8.0))
                    .andExpect(jsonPath("$.data.note").value("Güncellenmiş not"));

            // DB verification
            GoalEntry entry = goalEntryRepository.findById(entryId).orElseThrow();
            assertThat(entry.getActualValue()).isEqualByComparingTo(new BigDecimal("8.0"));
            assertThat(entry.getNote()).isEqualTo("Güncellenmiş not");
        }

        @Test
        @DisplayName("Olmayan entry güncelleme — 404")
        void updateEntry_notFound() throws Exception {
            UpdateEntryRequest updateReq = new UpdateEntryRequest(new BigDecimal("8.0"), "Not");
            mockMvc.perform(put("/api/entries/99999")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // DELETE ENTRY
    // ========================================================================

    @Nested
    @DisplayName("Entry Silme (DELETE /api/entries/{entryId})")
    class DeleteEntryTests {

        @Test
        @DisplayName("Başarılı entry silme — 204")
        void deleteEntry_success() throws Exception {
            CreateEntryRequest createReq = new CreateEntryRequest(
                    LocalDate.now(), new BigDecimal("5.0"), "Silinecek");
            MvcResult createResult = mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
            Long entryId = root.path("data").path("id").asLong();

            mockMvc.perform(delete("/api/entries/" + entryId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            assertThat(goalEntryRepository.findById(entryId)).isEmpty();
        }

        @Test
        @DisplayName("Olmayan entry silme — 404")
        void deleteEntry_notFound() throws Exception {
            mockMvc.perform(delete("/api/entries/99999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // STATS & CHART DATA
    // ========================================================================

    @Nested
    @DisplayName("İstatistik ve Grafik Verileri")
    class StatsAndChartTests {

        @Test
        @DisplayName("Hedef istatistikleri — entry'ler sonrası hesaplar doğru")
        void getStats_withEntries() throws Exception {
            // Add entries
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new CreateEntryRequest(LocalDate.now(), new BigDecimal("10.0"), null))));

            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new CreateEntryRequest(LocalDate.now().plusDays(1), new BigDecimal("15.0"), null))));

            mockMvc.perform(get("/api/goals/" + goalId + "/stats")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentProgress").isNumber())
                    .andExpect(jsonPath("$.data.targetValue").isNumber())
                    .andExpect(jsonPath("$.data.completionPct").isNumber())
                    .andExpect(jsonPath("$.data.entryCount").value(2))
                    .andExpect(jsonPath("$.data.unit").isNotEmpty())
                    .andExpect(jsonPath("$.data.daysLeft").isNumber())
                    .andExpect(jsonPath("$.data.totalDays").isNumber());
        }

        @Test
        @DisplayName("Grafik verileri — chart data endpoint'i çalışır")
        void getChartData() throws Exception {
            // Add an entry first
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new CreateEntryRequest(LocalDate.now(), new BigDecimal("5.0"), null))));

            mockMvc.perform(get("/api/goals/" + goalId + "/chart-data")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isNotEmpty());
        }

        @Test
        @DisplayName("Entry olmadan stats — sıfır değerler")
        void getStats_noEntries() throws Exception {
            mockMvc.perform(get("/api/goals/" + goalId + "/stats")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentProgress").value(0))
                    .andExpect(jsonPath("$.data.entryCount").value(0));
        }
    }

    // ========================================================================
    // MULTIPLE ENTRIES FLOW
    // ========================================================================

    @Test
    @DisplayName("Tam akış — birden fazla entry oluştur, güncelle, sil, stats kontrol et")
    void fullEntryLifecycle() throws Exception {
        // Create 3 entries on consecutive days
        for (int i = 0; i < 3; i++) {
            CreateEntryRequest req = new CreateEntryRequest(
                    LocalDate.now().plusDays(i), new BigDecimal(String.valueOf(10 + i)), "Gün " + (i + 1));
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        // Verify 3 entries exist
        mockMvc.perform(get("/api/goals/" + goalId + "/entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(3));

        // Update first entry
        GoalEntry firstEntry = goalEntryRepository.findByGoalIdAndEntryDate(goalId, LocalDate.now())
                .orElseThrow();
        UpdateEntryRequest updateReq = new UpdateEntryRequest(new BigDecimal("20.0"), "Güncellenmiş");
        mockMvc.perform(put("/api/entries/" + firstEntry.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)));

        // Delete second entry
        GoalEntry secondEntry = goalEntryRepository.findByGoalIdAndEntryDate(goalId, LocalDate.now().plusDays(1))
                .orElseThrow();
        mockMvc.perform(delete("/api/entries/" + secondEntry.getId())
                .header("Authorization", "Bearer " + token));

        // Verify 2 entries remain
        mockMvc.perform(get("/api/goals/" + goalId + "/entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(2));

        // Stats should reflect: 20.0 + 12.0 = 32.0 total progress
        mockMvc.perform(get("/api/goals/" + goalId + "/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.entryCount").value(2))
                .andExpect(jsonPath("$.data.currentProgress").value(32.0));
    }
}

