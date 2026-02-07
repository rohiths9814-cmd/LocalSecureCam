package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthService {

    public enum CameraState {
        RECORDING,
        STOPPED,
        RESTARTING
    }

    public static class CameraInfo {
        public CameraState state;
        public Instant lastChange;

        public CameraInfo(CameraState state) {
            this.state = state;
            this.lastChange = Instant.now();
        }
    }

    private final Map<String, CameraInfo> cameras = new ConcurrentHashMap<>();

    public void setState(String cameraId, CameraState state) {
        cameras.put(cameraId, new CameraInfo(state));
    }

    public Map<String, CameraInfo> snapshot() {
        return cameras;
    }
}
