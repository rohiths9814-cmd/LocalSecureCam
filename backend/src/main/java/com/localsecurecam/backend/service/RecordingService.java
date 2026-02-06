package com.localsecurecam.backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecordingService {

    private static final String FFMPEG = "/usr/bin/ffmpeg";
    private static final String BASE_DIR = "/home/pi/LocalSecureCam/recordings";

    // Thread-safe (VERY IMPORTANT)
    private final Map<String, Process> processes = new ConcurrentHashMap<>();

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

                // 🔧 FIX TIMESTAMPS (THIS IS THE KEY)
                "-fflags", "+genpts",
                "-use_wallclock_as_timestamps", "1",

                "-rtsp_transport", "tcp",
                "-probesize", "10M",
                "-analyzeduration", "10M",

                "-i", rtspUrl,

                // ✅ COPY STREAM (NO RE-ENCODE)
                "-map", "0:v:0",
                "-c:v", "copy",

                "-f", "segment",
                "-segment_time", "300",
                "-reset_timestamps", "1",
                "-strftime", "1",

                outputTemplate
            );

            System.out.println("\n▶ STARTING CAMERA: " + cameraId);
            command.forEach(System.out::println);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            processes.put(cameraId, process);

            // 🔥 LOG OUTPUT (DO NOT REMOVE)
            new Thread(() -> readLogs(cameraId, process)).start();

            System.out.println("✅ Recording started: " + cameraId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start recording " + cameraId, e);
        }
    }

    public synchronized void stopRecording(String cameraId) {
        Process process = processes.remove(cameraId);
        if (process != null) {
            process.destroy();
            System.out.println("🛑 Recording stopped: " + cameraId);
        }
    }

    private void readLogs(String cameraId, Process process) {
        try (BufferedReader br =
                 new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("[" + cameraId + "] " + line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
