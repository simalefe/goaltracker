package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.request.FriendRequestRequest;
import com.goaltracker.dto.response.FriendRequestResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.model.enums.FriendshipStatus;
import com.goaltracker.service.FriendshipService;
import com.goaltracker.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final SecurityUtils securityUtils;

    public FriendshipController(FriendshipService friendshipService, SecurityUtils securityUtils) {
        this.friendshipService = friendshipService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriends() {
        Long userId = securityUtils.getCurrentUserId();
        List<FriendResponse> friends = friendshipService.getFriends(userId);
        return ResponseEntity.ok(ApiResponse.ok(friends));
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequest(
            @Valid @RequestBody FriendRequestRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        FriendRequestResponse response = friendshipService.sendRequest(userId, request.getReceiverUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Arkadaşlık isteği gönderildi."));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> acceptRequest(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        FriendRequestResponse response = friendshipService.acceptRequest(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Arkadaşlık isteği kabul edildi."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFriend(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        friendshipService.removeFriend(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<String, List<FriendRequestResponse>>>> getPendingRequests() {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, List<FriendRequestResponse>> pending = friendshipService.getPendingRequests(userId);
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getFriendStatus(
            @PathVariable("userId") Long otherUserId) {
        Long userId = securityUtils.getCurrentUserId();
        FriendshipStatus status = friendshipService.getFriendStatus(userId, otherUserId);
        String statusStr = status != null ? status.name() : null;
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", statusStr != null ? statusStr : "NONE")));
    }
}

