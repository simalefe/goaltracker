package com.goaltracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.goaltracker.dto.request.FriendRequestRequest;
import com.goaltracker.model.Friendship;
import com.goaltracker.model.enums.FriendshipStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Friendship (Social) features.
 * Tests friend request lifecycle, friend list, pending requests.
 */
class FriendshipIntegrationTest extends BaseIntegrationTest {

    private UserTokenPair alice;
    private UserTokenPair bob;
    private UserTokenPair charlie;

    @BeforeEach
    void setUp() throws Exception {
        alice = registerUser("alice");
        bob = registerUser("bob");
        charlie = registerUser("charlie");
    }

    // ========================================================================
    // SEND FRIEND REQUEST
    // ========================================================================

    @Nested
    @DisplayName("Arkadaşlık İsteği Gönderme")
    class SendFriendRequestTests {

        @Test
        @DisplayName("Başarılı arkadaşlık isteği — 201")
        void sendRequest_success() throws Exception {
            FriendRequestRequest request = new FriendRequestRequest(bob.username());

            MvcResult result = mockMvc.perform(post("/api/friends/request")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.requesterUsername").value(alice.username()))
                    .andExpect(jsonPath("$.data.receiverUsername").value(bob.username()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andReturn();

            // DB verification
            assertThat(friendshipRepository.count()).isGreaterThan(0);
            Friendship friendship = friendshipRepository.findAll().stream()
                    .filter(f -> f.getStatus() == FriendshipStatus.PENDING)
                    .findFirst().orElseThrow();
            assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);

            // Notification should be created for bob
            long bobNotificationCount = notificationRepository.findAll().stream()
                    .filter(n -> n.getUser().getUsername().equals(bob.username()))
                    .count();
            assertThat(bobNotificationCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Kendine arkadaşlık isteği gönderme — hata")
        void sendRequest_self() throws Exception {
            FriendRequestRequest request = new FriendRequestRequest(alice.username());

            mockMvc.perform(post("/api/friends/request")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Var olmayan kullanıcıya istek — 404")
        void sendRequest_nonExistentUser() throws Exception {
            FriendRequestRequest request = new FriendRequestRequest("nonexistent_user_xyz");

            mockMvc.perform(post("/api/friends/request")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Aynı kişiye tekrar istek gönderme — hata (duplicate)")
        void sendRequest_duplicate() throws Exception {
            FriendRequestRequest request = new FriendRequestRequest(bob.username());

            // First request
            mockMvc.perform(post("/api/friends/request")
                    .header("Authorization", "Bearer " + alice.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Duplicate request
            mockMvc.perform(post("/api/friends/request")
                            .header("Authorization", "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // ACCEPT FRIEND REQUEST
    // ========================================================================

    @Nested
    @DisplayName("Arkadaşlık İsteği Kabul Etme")
    class AcceptFriendRequestTests {

        @Test
        @DisplayName("Başarılı kabul — statü ACCEPTED olur")
        void acceptRequest_success() throws Exception {
            // Alice sends request to Bob
            Long friendshipId = sendFriendRequest(alice.token(), bob.username());

            // Bob accepts
            mockMvc.perform(put("/api/friends/" + friendshipId + "/accept")
                            .header("Authorization", "Bearer " + bob.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

            // DB verification
            Friendship friendship = friendshipRepository.findById(friendshipId).orElseThrow();
            assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        }

        @Test
        @DisplayName("İsteği gönderen kişi kabul edemez — 400")
        void acceptRequest_byRequester() throws Exception {
            Long friendshipId = sendFriendRequest(alice.token(), bob.username());

            // Alice tries to accept her own request
            mockMvc.perform(put("/api/friends/" + friendshipId + "/accept")
                            .header("Authorization", "Bearer " + alice.token()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Üçüncü kişi isteği kabul edemez — 400")
        void acceptRequest_byThirdParty() throws Exception {
            Long friendshipId = sendFriendRequest(alice.token(), bob.username());

            // Charlie cannot accept
            mockMvc.perform(put("/api/friends/" + friendshipId + "/accept")
                            .header("Authorization", "Bearer " + charlie.token()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // REMOVE FRIEND
    // ========================================================================

    @Nested
    @DisplayName("Arkadaş Kaldırma")
    class RemoveFriendTests {

        @Test
        @DisplayName("Arkadaş kaldırma başarılı — 204")
        void removeFriend_success() throws Exception {
            Long friendshipId = sendFriendRequest(alice.token(), bob.username());
            // Bob accepts
            mockMvc.perform(put("/api/friends/" + friendshipId + "/accept")
                    .header("Authorization", "Bearer " + bob.token()));

            // Alice removes
            mockMvc.perform(delete("/api/friends/" + friendshipId)
                            .header("Authorization", "Bearer " + alice.token()))
                    .andExpect(status().isNoContent());

            // DB verification
            assertThat(friendshipRepository.findById(friendshipId)).isEmpty();
        }
    }

    // ========================================================================
    // FRIEND LIST
    // ========================================================================

    @Nested
    @DisplayName("Arkadaş Listesi")
    class FriendListTests {

        @Test
        @DisplayName("Kabul edilmiş arkadaşlar listelenir")
        void getFriends_success() throws Exception {
            // Alice → Bob (accepted)
            Long fid1 = sendFriendRequest(alice.token(), bob.username());
            mockMvc.perform(put("/api/friends/" + fid1 + "/accept")
                    .header("Authorization", "Bearer " + bob.token()));

            // Alice → Charlie (accepted)
            Long fid2 = sendFriendRequest(alice.token(), charlie.username());
            mockMvc.perform(put("/api/friends/" + fid2 + "/accept")
                    .header("Authorization", "Bearer " + charlie.token()));

            // Alice should have 2 friends
            mockMvc.perform(get("/api/friends")
                            .header("Authorization", "Bearer " + alice.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));

            // Bob should have 1 friend (Alice)
            mockMvc.perform(get("/api/friends")
                            .header("Authorization", "Bearer " + bob.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].username").value(alice.username()));
        }

        @Test
        @DisplayName("Bekleyen istekler listelenir")
        void getPendingRequests() throws Exception {
            sendFriendRequest(alice.token(), bob.username());

            // Bob's pending requests (incoming)
            mockMvc.perform(get("/api/friends/pending")
                            .header("Authorization", "Bearer " + bob.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.incoming.length()").value(1))
                    .andExpect(jsonPath("$.data.incoming[0].requesterUsername").value(alice.username()));

            // Alice's pending requests (outgoing)
            mockMvc.perform(get("/api/friends/pending")
                            .header("Authorization", "Bearer " + alice.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.outgoing.length()").value(1));
        }
    }

    // ========================================================================
    // FRIENDSHIP STATUS
    // ========================================================================

    @Test
    @DisplayName("Arkadaşlık durumu sorgusu")
    void getFriendStatus() throws Exception {
        Long bobUserId = userRepository.findByEmail(bob.email()).orElseThrow().getId();

        // Before request — NONE
        mockMvc.perform(get("/api/friends/" + bobUserId + "/status")
                        .header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NONE"));

        // After request — PENDING
        sendFriendRequest(alice.token(), bob.username());
        mockMvc.perform(get("/api/friends/" + bobUserId + "/status")
                        .header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    // ========================================================================
    // HELPER
    // ========================================================================

    private Long sendFriendRequest(String senderToken, String receiverUsername) throws Exception {
        FriendRequestRequest request = new FriendRequestRequest(receiverUsername);
        MvcResult result = mockMvc.perform(post("/api/friends/request")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}

