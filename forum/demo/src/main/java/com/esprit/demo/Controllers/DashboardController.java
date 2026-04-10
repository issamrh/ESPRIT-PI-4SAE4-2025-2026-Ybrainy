package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.DayActivity;
import com.esprit.demo.Dto.UserDashboardResponse;
import com.esprit.demo.Dto.XpDataPoint;
import com.esprit.demo.Services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserDashboardResponse> getDashboard(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getUserDashboard(userId));
    }

    @GetMapping("/{userId}/xp-timeline")
    public ResponseEntity<List<XpDataPoint>> getXpTimeline(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getXpTimeline(userId));
    }

    @GetMapping("/{userId}/activity")
    public ResponseEntity<List<DayActivity>> getActivity(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getWeeklyActivity(userId));
    }
}
