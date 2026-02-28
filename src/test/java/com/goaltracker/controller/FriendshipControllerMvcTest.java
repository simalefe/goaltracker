package com.goaltracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaltracker.dto.request.FriendRequestRequest;
import com.goaltracker.dto.response.FriendRequestResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.model.enums.FriendshipStatus;
import com.goaltracker.service.*;
import com.goaltracker.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FriendshipControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FriendshipService friendshipService;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/friends → 200 arkadaş listesi")
    void getFriends_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(friendshipService.getFriends(1L)).willReturn(List.of(
                new FriendResponse(2L, "bob", "Bob", null, Instant.now())
        ));

        mockMvc.perform(get("/api/friends")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("bob"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST /api/friends/request → 201 istek gönderme")
    void sendRequest_shouldReturn201() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        FriendRequestResponse response = new FriendRequestResponse();
        response.setId(100L);
        response.setRequesterId(1L);
        response.setReceiverUsername("bob");
        response.setStatus(FriendshipStatus.PENDING);
        response.setCreatedAt(Instant.now());
        given(friendshipService.sendRequest(1L, "bob")).willReturn(response);

        String json = """
                { "receiverUsername": "bob" }
                """;

        mockMvc.perform(post("/api/friends/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PUT /api/friends/{id}/accept → 200 kabul")
    void acceptRequest_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        FriendRequestResponse response = new FriendRequestResponse();
        response.setId(100L);
        response.setStatus(FriendshipStatus.ACCEPTED);
        given(friendshipService.acceptRequest(100L, 1L)).willReturn(response);

        mockMvc.perform(put("/api/friends/100/accept")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("DELETE /api/friends/{id} → 204")
    void removeFriend_shouldReturn204() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/friends/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/friends/pending → 200")
    void getPending_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(friendshipService.getPendingRequests(1L)).willReturn(
                Map.of("incoming", List.of(), "outgoing", List.of())
        );

        mockMvc.perform(get("/api/friends/pending")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.incoming").isArray())
                .andExpect(jsonPath("$.data.outgoing").isArray());
    }

    @Test
    @DisplayName("Yetkilendirilmemiş erişim → 302 redirect")
    void shouldRedirectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/friends")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }
}

