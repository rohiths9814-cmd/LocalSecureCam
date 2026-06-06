package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.config.CameraProperties;
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
    private final CameraProperties props;

    public HealthController(HealthService healthService, DiskService diskService, CameraProperties props) {
        this.healthService = healthService;
        this.diskService = diskService;
        this.props = props;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> data = new HashMap<>();

        File recordings = new File(props.getRecordingsDir());

        data.put("diskFreePercent", diskService.getFreePercent(recordings));
        data.put("cameras", healthService.snapshot());

        return data;
    }
}
