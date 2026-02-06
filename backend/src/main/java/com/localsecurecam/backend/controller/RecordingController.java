package com.localsecurecam.backend.controller;

import com.localsecurecam.backend.service.RecordingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/camera")
public class RecordingController {

    private final RecordingService recordingService;

    public RecordingController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @GetMapping("/{name}/start")
    public String start(@PathVariable String name) throws Exception {
        recordingService.startRecording(name);
        return name + " started";
    }

    @GetMapping("/{name}/stop")
    public String stop(@PathVariable String name) {
        recordingService.stopRecording(name);
        return name + " stopped";
    }
}
