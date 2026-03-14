package com.leeinx.acasb.controller;

import com.leeinx.acasb.dto.ApiResponse;
import com.leeinx.acasb.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/color-levels")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getColorLevels(
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getColorLevels(datasetName)));
    }

    @GetMapping("/dynasty-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDynastyStats(
            @RequestParam String dynasty,
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDynastyStats(datasetName, dynasty)));
    }

    @GetMapping("/map-distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMapDistribution(
            @RequestParam String dynasty,
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getMapDistribution(datasetName, dynasty)));
    }

    @GetMapping("/core-colors")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCoreColors(
            @RequestParam String dynasty,
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCoreColors(datasetName, dynasty)));
    }

    @GetMapping("/color-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getColorAnalysis(
            @RequestParam String dynasty,
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getColorAnalysis(datasetName, dynasty)));
    }

    @GetMapping("/level-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLevelStats(
            @RequestParam String dynasty,
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getLevelStats(datasetName, dynasty)));
    }

    @GetMapping("/dynasty-comparison")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDynastyComparison(
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDynastyComparison(datasetName)));
    }

    @GetMapping("/history-trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistoryTrend(
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getHistoryTrend(datasetName)));
    }

    @GetMapping("/region-rank-dist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRegionRankDist(
            @RequestParam(required = false) String datasetName) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getRegionRankDistribution(datasetName)));
    }

    @GetMapping("/material-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMaterialAnalysis() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getMaterialAnalysis()));
    }
}
