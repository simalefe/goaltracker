package com.goaltracker.service;

import com.goaltracker.dto.response.UserBadgeResponse;

import java.util.List;

public interface BadgeService {

    void checkAndAwardBadges(Long userId, Long goalId);

    List<UserBadgeResponse> getUserBadges(Long userId);

    long getUserBadgeCount(Long userId);
}

