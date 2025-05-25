package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.service.SystemUtilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api") // Đặt base path là /api cho các endpoint này
@RequiredArgsConstructor
public class SystemController {

    private final SystemUtilityService systemUtilityService;

    /**
     * GET /api/health - Health check
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        log.info("Request health check");
        Map<String, String> healthStatus = systemUtilityService.getHealthStatus();
        if ("UP".equals(healthStatus.get("status"))) {
            return ResponseEntity.ok(ApiResponse.success("Hệ thống đang hoạt động.", healthStatus));
        } else {
            return ResponseEntity.status(503).body(ApiResponse.error("Hệ thống có lỗi."));
        }
    }
}