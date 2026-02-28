package com.goaltracker.service;

import com.goaltracker.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboard(Long userId);
}

