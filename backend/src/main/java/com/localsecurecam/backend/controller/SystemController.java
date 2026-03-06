package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.service.SystemStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SystemController {

    private final SystemStatsService stats;

    public SystemController(SystemStatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/api/system-stats")
    public Map<String, Double> getStats() {

        Map<String, Double> data = new HashMap<>();

        data.put("cpu", stats.getCpuUsage());
        data.put("ram", stats.getRamUsage());
        data.put("temperature", stats.getTemperature());
        data.put("disk", stats.getDiskUsage());

        return data;
    }
}