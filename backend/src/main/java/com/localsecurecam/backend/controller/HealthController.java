package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.service.DiskService;
import com.localsecurecam.backend.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final HealthService healthService;
    private final DiskService diskService;

    public HealthController(HealthService healthService, DiskService diskService) {
        this.healthService = healthService;
        this.diskService = diskService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> data = new HashMap<>();
        File recordings = new File("recordings");

        data.put("diskFreePercent", diskService.getFreePercent(recordings));
        data.put("cameras", healthService.snapshot());

        return data;
    }
}
