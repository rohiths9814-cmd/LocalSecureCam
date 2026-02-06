package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthService {

    public enum CameraStatus {
        RECORDING,
        RECONNECTING,
        STOPPED
    }

    private final Map<String, CameraStatus> statusMap = new ConcurrentHashMap<>();

    public void setStatus(String camera, CameraStatus status) {
        statusMap.put(camera, status);
    }

    public Map<String, CameraStatus> snapshot() {
        return statusMap;
    }
}
