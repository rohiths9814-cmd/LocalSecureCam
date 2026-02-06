package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.service.RecordingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/camera")
public class CameraController {

    private final RecordingService recordingService;

    public CameraController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @GetMapping("/{cameraId}/start")
    public String start(@PathVariable String cameraId) {
        recordingService.startRecording(cameraId);
        return "Recording started for " + cameraId;
    }

    @GetMapping("/{cameraId}/stop")
    public String stop(@PathVariable String cameraId) {
        recordingService.stopRecording(cameraId);
        return "Recording stopped for " + cameraId;
    }
}
