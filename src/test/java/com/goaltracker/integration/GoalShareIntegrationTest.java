package com.goaltracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.goaltracker.dto.request.FriendRequestRequest;
import com.goaltracker.dto.request.ShareGoalRequest;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.SharePermission;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Goal Sharing features.
 * Tests sharing with friends, permission checks, listing shared goals.
 */
class GoalShareIntegrationTest extends BaseIntegrationTest {

    private UserTokenPair alice;
    private UserTokenPair bob;
    private Long aliceGoalId;
    private Long bobUserId;

    @BeforeEach
    void setUp() throws Exception {
        alice = registerUser("alice");
        bob = registerUser("bob");
        aliceGoalId = createGoalAndGetId(alice.token(), "Alice'in Paylaşılacak Hedefi");

        // Make them friends
        makeFriends(alice, bob);

        bobUserId = userRepository.findByEmail(bob.email()).orElseThrow().getId();
    }

    // ========================================================================
    // SHARE GOAL
    // ========================================================================

    @Nested
    @DisplayName("Hedef Paylaşma")
    class ShareGoalTests {

        @Test
        @DisplayName("Arkadaşla hedef paylaşma — 201")
        void shareGoal_success() throws Exception {
            ShareGoalRequest request = new ShareGoalRequest(bobUserId, SharePermission.READ);

            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            // DB verification
            assertThat(goalShareRepository.existsByGoalIdAndSharedWithUserId(aliceGoalId, bobUserId)).isTrue();
        }

        @Test
        @DisplayName("Arkadaş olmayan kişiyle paylaşma — hata")
        void shareGoal_notFriends() throws Exception {
            UserTokenPair charlie = registerUser("charlie");
            Long charlieUserId = userRepository.findByEmail(charlie.email()).orElseThrow().getId();

            ShareGoalRequest request = new ShareGoalRequest(charlieUserId, SharePermission.READ);

            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Aynı kişiyle tekrar paylaşma — hata (duplicate)")
        void shareGoal_duplicate() throws Exception {
            ShareGoalRequest request = new ShareGoalRequest(bobUserId, SharePermission.READ);

            // First share
            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                    .header("Authorization", "Bearer " + alice.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Duplicate share
            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Başka birinin hedefini paylaşma — 403")
        void shareGoal_notOwner() throws Exception {
            Long aliceUserId = userRepository.findByEmail(alice.email()).orElseThrow().getId();
            ShareGoalRequest request = new ShareGoalRequest(aliceUserId, SharePermission.READ);

            // Bob tries to share Alice's goal
            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                            .header("Authorization", "Bearer " + bob.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================================================
    // SHARED GOALS
    // ========================================================================

    @Nested
    @DisplayName("Paylaşılan Hedefler")
    class SharedGoalsTests {

        @Test
        @DisplayName("Benimle paylaşılan hedefler listelenir")
        void getSharedWithMe() throws Exception {
            // Share Alice's goal with Bob
            ShareGoalRequest request = new ShareGoalRequest(bobUserId, SharePermission.READ);
            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                    .header("Authorization", "Bearer " + alice.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Bob checks shared goals
            mockMvc.perform(get("/api/goals/shared-with-me")
                            .header("Authorization", "Bearer " + bob.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("Alice'in Paylaşılacak Hedefi"));
        }

        @Test
        @DisplayName("Paylaşılan kullanıcılar listelenir")
        void getSharedWithUsers() throws Exception {
            ShareGoalRequest request = new ShareGoalRequest(bobUserId, SharePermission.READ);
            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                    .header("Authorization", "Bearer " + alice.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            mockMvc.perform(get("/api/goals/" + aliceGoalId + "/shared-with")
                            .header("Authorization", "Bearer " + alice.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].username").value(bob.username()));
        }
    }

    // ========================================================================
    // REMOVE SHARE
    // ========================================================================

    @Nested
    @DisplayName("Paylaşım Kaldırma")
    class RemoveShareTests {

        @Test
        @DisplayName("Paylaşımı kaldırma başarılı — 204")
        void removeShare_success() throws Exception {
            ShareGoalRequest request = new ShareGoalRequest(bobUserId, SharePermission.READ);
            mockMvc.perform(post("/api/goals/" + aliceGoalId + "/share")
                    .header("Authorization", "Bearer " + alice.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            mockMvc.perform(delete("/api/goals/" + aliceGoalId + "/share/" + bobUserId)
                            .header("Authorization", "Bearer " + alice.token()))
                    .andExpect(status().isNoContent());

            // Verify share is removed
            assertThat(goalShareRepository.existsByGoalIdAndSharedWithUserId(aliceGoalId, bobUserId)).isFalse();

            // Bob should see no shared goals
            mockMvc.perform(get("/api/goals/shared-with-me")
                            .header("Authorization", "Bearer " + bob.token()))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private void makeFriends(UserTokenPair requester, UserTokenPair receiver) throws Exception {
        FriendRequestRequest request = new FriendRequestRequest(receiver.username());
        MvcResult result = mockMvc.perform(post("/api/friends/request")
                        .header("Authorization", "Bearer " + requester.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long friendshipId = root.path("data").path("id").asLong();

        mockMvc.perform(put("/api/friends/" + friendshipId + "/accept")
                .header("Authorization", "Bearer " + receiver.token()));
    }
}

