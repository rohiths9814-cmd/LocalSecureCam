package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.service.SystemStatsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemController {

    private final SystemStatsService stats;

    public SystemController(SystemStatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/system-stats")
    public Map<String,Object> stats() {

        Map<String,Object> m = new HashMap<>();

        m.put("cpu", stats.getCpuUsage());
        m.put("ram", stats.getRamUsage());
        m.put("temp", stats.getTemperature());
        m.put("disk", stats.getDiskUsage());

        return m;
    }
}