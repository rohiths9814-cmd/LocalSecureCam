package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CameraStatusController {

    private final HealthService healthService;

    public CameraStatusController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/cameras")
    public Map<String, HealthService.CameraInfo> cameras() {
        return healthService.snapshot();
    }
}
