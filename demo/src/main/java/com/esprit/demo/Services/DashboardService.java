package com.esprit.demo.Services;

import com.esprit.demo.Dto.DayActivity;
import com.esprit.demo.Dto.UserDashboardResponse;
import com.esprit.demo.Dto.XpDataPoint;

import java.util.List;

public interface DashboardService {
    UserDashboardResponse getUserDashboard(Long userId);
    List<XpDataPoint> getXpTimeline(Long userId);
    List<DayActivity> getWeeklyActivity(Long userId);
}
