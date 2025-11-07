package com.buildbetter.webApi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Application health and monitoring")
public class HealthController {

    private final DataSource dataSource;

    @Data
    @Builder
    @AllArgsConstructor
    public static class HealthStatus {
        private String status;
        private LocalDateTime timestamp;
        private String version;
        private Map<String, String> components;
        private Map<String, Object> details;
    }

    @GetMapping
    @Operation(summary = "Get application health status")
    public ResponseEntity<HealthStatus> getHealth() {
        Map<String, String> components = new HashMap<>();
        Map<String, Object> details = new HashMap<>();

        // Check database
        boolean dbHealthy = checkDatabase();
        components.put("database", dbHealthy ? "UP" : "DOWN");

        // Check memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        details.put("memory", Map.of(
                "max", maxMemory / (1024 * 1024) + " MB",
                "total", totalMemory / (1024 * 1024) + " MB",
                "used", usedMemory / (1024 * 1024) + " MB",
                "free", freeMemory / (1024 * 1024) + " MB"
        ));

        // System info
        details.put("system", Map.of(
                "processors", Runtime.getRuntime().availableProcessors(),
                "osName", System.getProperty("os.name"),
                "osVersion", System.getProperty("os.version"),
                "javaVersion", System.getProperty("java.version")
        ));

        String overallStatus = components.values().stream()
                .allMatch(s -> s.equals("UP")) ? "UP" : "DEGRADED";

        HealthStatus health = HealthStatus.builder()
                .status(overallStatus)
                .timestamp(LocalDateTime.now())
                .version("1.0.0-SAAS")
                .components(components)
                .details(details)
                .build();

        return ResponseEntity.ok(health);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness probe")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe")
    public ResponseEntity<Map<String, String>> readiness() {
        boolean ready = checkDatabase();
        if (ready) {
            return ResponseEntity.ok(Map.of("status", "READY"));
        } else {
            return ResponseEntity.status(503).body(Map.of("status", "NOT_READY"));
        }
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
