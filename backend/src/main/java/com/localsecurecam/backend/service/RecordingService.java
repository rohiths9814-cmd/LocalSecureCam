package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class RecordingService {

    private final Map<String, Process> processes = new HashMap<>();

    private static final String FFMPEG = "/usr/bin/ffmpeg";
    private static final String BASE_DIR = "/home/pi/LocalSecureCam/recordings";

    private final Map<String, String> cameraUrls = Map.of(
        "camera1", "rtsp://192.168.31.196:554/",
        "camera2", "rtsp://192.168.31.107:554/"
    );

    public synchronized void startRecording(String cameraId) {

        if (processes.containsKey(cameraId)) {
            System.out.println("⚠ Already recording: " + cameraId);
            return;
        }

        String rtspUrl = cameraUrls.get(cameraId);
        if (rtspUrl == null) {
            throw new RuntimeException("Unknown camera: " + cameraId);
        }

        try {
            Path outputDir = Paths.get(
                BASE_DIR,
                cameraId,
                LocalDate.now().toString()
            );

            Files.createDirectories(outputDir);

            String outputTemplate =
                outputDir.resolve("%Y-%m-%d_%H-%M-%S.mkv").toString();

            List<String> command = List.of(
                FFMPEG,

                "-loglevel", "info",

                "-rtsp_transport", "tcp",
                "-i", rtspUrl,

                "-map", "0:v:0",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "zerolatency",
                "-crf", "28",

                "-f", "segment",
                "-segment_time", "300",
                "-reset_timestamps", "1",
                "-strftime", "1",

                outputTemplate
            );

            System.out.println("▶ Starting FFmpeg:");
            command.forEach(System.out::println);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            processes.put(cameraId, process);

            // 🔥 READ FFMPEG LOGS (THIS IS KEY)
            new Thread(() -> {
                try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[" + cameraId + "] " + line);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            System.out.println("✅ Recording started: " + cameraId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("FFmpeg failed for " + cameraId, e);
        }
    }

    public synchronized void stopRecording(String cameraId) {
        Process process = processes.remove(cameraId);
        if (process != null) {
            process.destroy();
            System.out.println("🛑 Recording stopped: " + cameraId);
        }
    }
}
