package com.localsecurecam.backend.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutoStartService {

    private final RecordingService recordingService;

    // Cameras to auto-start on boot
    private final List<String> autoStartCameras = List.of(
            "camera1",
            "camera2"
    );

    public AutoStartService(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startCamerasOnBoot() {

        System.out.println("🚀 Application ready — starting cameras on boot");

        for (String cameraId : autoStartCameras) {
            try {
                recordingService.startRecording(cameraId);
            } catch (Exception e) {
                System.err.println("❌ Failed to start " + cameraId + " on boot");
                e.printStackTrace();
            }
        }
    }
}
