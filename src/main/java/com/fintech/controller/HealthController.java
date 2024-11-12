package com.fintech.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final long startTime = System.currentTimeMillis();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        long uptimeSec = (System.currentTimeMillis() - startTime) / 1000;
        return ResponseEntity.ok(Map.of("status", "UP", "uptime_seconds", uptimeSec, "version", "1.0.0"));
    }
}
