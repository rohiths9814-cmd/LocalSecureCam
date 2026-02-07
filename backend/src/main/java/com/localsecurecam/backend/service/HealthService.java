package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthService {

    public enum CameraState {
        RECORDING,
        RESTARTING,
        STOPPED
    }

    public static class CameraInfo {
        public CameraState state;
        public Instant lastChange;
        public Instant lastSegmentTime;

        public CameraInfo(CameraState state) {
            this.state = state;
            this.lastChange = Instant.now();
        }
    }

    private final Map<String, CameraInfo> cameras = new ConcurrentHashMap<>();

    // ===== STATE UPDATE =====
    public void setState(String cameraId, CameraState state) {
        cameras
            .computeIfAbsent(cameraId, k -> new CameraInfo(state))
            .state = state;

        cameras.get(cameraId).lastChange = Instant.now();
    }

    // ===== RECORDING HEARTBEAT =====
    public void updateLastSegment(String cameraId) {
        cameras
            .computeIfAbsent(cameraId, k -> new CameraInfo(CameraState.RECORDING))
            .lastSegmentTime = Instant.now();
    }

    // ===== API SNAPSHOT =====
    public Map<String, CameraInfo> snapshot() {
        return cameras;
    }
}
