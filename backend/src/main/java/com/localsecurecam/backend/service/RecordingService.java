package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecordingService {

    private final Map<String, Process> processes = new HashMap<>();

    // 👉 Update camera RTSP URLs here
    private final Map<String, String> cameraUrls = Map.of(
        "camera1", "rtsp://192.168.31.196:554/",
        "camera2", "rtsp://192.168.31.107:554/"
    );

    public synchronized void startRecording(String cameraId) {

        if (processes.containsKey(cameraId)) {
            System.out.println(cameraId + " is already recording");
            return;
        }

        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) {
            throw new RuntimeException("Unknown camera: " + cameraId);
        }

        try {
            // recordings/camera1/2026-02-06/
            Path outputDir = Paths.get(
                "recordings",
                cameraId,
                LocalDate.now().toString()
            );

            Files.createDirectories(outputDir);

            String outputTemplate =
                outputDir.resolve("%Y-%m-%d_%H-%M-%S.mkv").toString();

            List<String> command = List.of(
                "ffmpeg",

                // ---------- INPUT ----------
                "-rtsp_transport", "tcp",
                "-stimeout", "5000000",      // 5s RTSP timeout
                "-i", rtspUrl,

                // ---------- VIDEO ----------
                "-map", "0:v:0",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "zerolatency",
                "-crf", "28",
                "-g", "28",
                "-keyint_min", "28",

                // ---------- SEGMENT ----------
                "-f", "segment",
                "-segment_time", "300",     // 5 minutes
                "-reset_timestamps", "1",
                "-strftime", "1",

                // ---------- OUTPUT ----------
                outputTemplate
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            processes.put(cameraId, process);

            System.out.println("STARTED recording: " + cameraId);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start FFmpeg for " + cameraId, e);
        }
    }

    public synchronized void stopRecording(String cameraId) {
        Process process = processes.remove(cameraId);
        if (process != null) {
            process.destroy();
            System.out.println("STOPPED recording: " + cameraId);
        }
    }
}
