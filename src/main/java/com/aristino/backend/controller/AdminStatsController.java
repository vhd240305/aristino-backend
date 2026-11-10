package com.aristino.backend.controller;

import com.aristino.backend.dto.admin.StatsResponse;
import com.aristino.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    // GET /api/admin/stats?days=30 - so lieu trang tong quan trong N ngay gan nhat (1..365)
    @GetMapping
    public StatsResponse getStats(@RequestParam(defaultValue = "30") int days) {
        return statsService.getStats(days);
    }
}